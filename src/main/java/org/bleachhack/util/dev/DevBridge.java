/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.dev;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.Level;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.setting.module.ModuleSetting;
import org.bleachhack.setting.module.SettingColor;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.BleachLogger;

/**
 * Loopback control socket for automated testing, dev environment only.
 *
 * Lets a script drive the client the way a player would - toggle modules, change
 * settings, run server commands, grab screenshots - so feature work can be verified
 * without a human at the keyboard.
 *
 * One request per connection: send a line, read the response until EOF.
 * Never starts outside a dev environment, and only binds to loopback.
 */
public class DevBridge {

	public static final int PORT = 26501;

	private static volatile boolean started;

	public static void start() {
		if (started || !FabricLoader.getInstance().isDevelopmentEnvironment()) {
			return;
		}

		started = true;

		// Screenshot-driven testing needs the game to keep rendering the world while the
		// window is in the background, instead of throwing up the pause menu.
		Minecraft.getInstance().options.pauseOnLostFocus = false;

		Thread thread = new Thread(DevBridge::listen, "BleachHack-DevBridge");
		thread.setDaemon(true);
		thread.start();
	}

	private static void listen() {
		try (ServerSocket server = new ServerSocket(PORT, 8, InetAddress.getLoopbackAddress())) {
			BleachLogger.logger.log(Level.INFO, "DevBridge listening on 127.0.0.1:%d", PORT);

			while (!server.isClosed()) {
				try (Socket socket = server.accept();
						BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
						PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
					String line = in.readLine();
					out.print(line == null ? "ERR empty request" : onClientThread(line.trim()));
					out.flush();
				} catch (IOException e) {
					BleachLogger.logger.log(Level.WARN, "DevBridge connection failed: %s", e);
				}
			}
		} catch (IOException e) {
			BleachLogger.logger.log(Level.WARN, "DevBridge could not listen on %d: %s", PORT, e);
		}
	}

	/** Commands touch game state, so they have to run on the client thread. */
	private static String onClientThread(String line) {
		CompletableFuture<String> result = new CompletableFuture<>();

		Minecraft.getInstance().execute(() -> {
			try {
				result.complete(handle(line));
			} catch (Throwable t) {
				result.complete("ERR " + t);
			}
		});

		try {
			return result.get(15, TimeUnit.SECONDS);
		} catch (Exception e) {
			return "ERR timed out: " + e;
		}
	}

	private static String handle(String line) {
		String[] parts = line.split(" ", 2);
		String command = parts[0].toLowerCase();
		String rest = parts.length > 1 ? parts[1].trim() : "";
		Minecraft mc = Minecraft.getInstance();

		switch (command) {
			case "ping":
				return "OK pong";

			case "state": {
				StringBuilder sb = new StringBuilder("OK\n");
				sb.append("world=").append(mc.level != null).append('\n');
				sb.append("screen=").append(mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName()).append('\n');
				sb.append("fps=").append(mc.getFps()).append('\n');

				if (mc.player != null) {
					sb.append(String.format("pos=%.2f,%.2f,%.2f%n", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
					sb.append(String.format("rot=%.1f,%.1f%n", mc.player.getYRot(), mc.player.getXRot()));
					sb.append("health=").append(mc.player.getHealth()).append('\n');
				}

				if (mc.level != null) {
					int count = 0;
					StringBuilder types = new StringBuilder();
					for (Entity e : mc.level.entitiesForRendering()) {
						count++;
						if (count <= 25) {
							types.append(e.getType().toShortString()).append('@')
									.append(String.format("%.0f,%.0f,%.0f", e.getX(), e.getY(), e.getZ())).append(' ');
						}
					}
					sb.append("entities=").append(count).append('\n');
					sb.append("nearby=").append(types).append('\n');
				}

				return sb.toString();
			}

			case "modules": {
				StringBuilder sb = new StringBuilder("OK\n");
				for (Module m : ModuleManager.getModules()) {
					sb.append(m.getName()).append('=').append(m.isEnabled() ? "on" : "off").append('\n');
				}
				return sb.toString();
			}

			case "enable":
			case "disable":
			case "toggle": {
				Module module = ModuleManager.getModule(rest);
				if (module == null) {
					return "ERR no module named '" + rest + "'";
				}

				if (command.equals("toggle")) {
					module.toggle();
				} else {
					module.setEnabled(command.equals("enable"));
				}

				return "OK " + module.getName() + "=" + (module.isEnabled() ? "on" : "off");
			}

			case "settings": {
				Module module = ModuleManager.getModule(rest);
				if (module == null) {
					return "ERR no module named '" + rest + "'";
				}

				StringBuilder sb = new StringBuilder("OK\n");
				List<ModuleSetting<?>> settings = module.getSettings();
				for (int i = 0; i < settings.size(); i++) {
					describe(sb, String.valueOf(i), settings.get(i));

					if (settings.get(i) instanceof SettingToggle toggle) {
						for (int c = 0; c < toggle.getChildren().size(); c++) {
							describe(sb, i + "." + c, toggle.getChild(c));
						}
					}
				}
				return sb.toString();
			}

			case "set": {
				String[] args = rest.split(" ", 3);
				if (args.length < 3) {
					return "ERR usage: set <module> <index[.child]> <value>";
				}

				Module module = ModuleManager.getModule(args[0]);
				if (module == null) {
					return "ERR no module named '" + args[0] + "'";
				}

				ModuleSetting<?> setting = resolve(module, args[1]);
				if (setting == null) {
					return "ERR no setting at index '" + args[1] + "'";
				}

				return apply(setting, args[2].trim());
			}

			case "cmd": {
				if (mc.player == null) {
					return "ERR not in a world";
				}
				mc.player.connection.sendCommand(rest);
				return "OK sent /" + rest;
			}

			case "chat": {
				if (mc.player == null) {
					return "ERR not in a world";
				}
				mc.player.connection.sendChat(rest);
				return "OK";
			}

			case "look": {
				String[] args = rest.split("[ ,]+");
				if (mc.player == null || args.length < 2) {
					return "ERR usage: look <yaw> <pitch> (needs a world)";
				}
				mc.player.setYRot(Float.parseFloat(args[0]));
				mc.player.setXRot(Float.parseFloat(args[1]));
				return "OK";
			}

			case "connect": {
				String host = rest.isEmpty() ? "localhost" : rest;
				ConnectScreen.startConnecting(mc.gui.screen(), mc, ServerAddress.parseString(host),
						new ServerData("BleachHack Dev", host, ServerData.Type.OTHER), false, null);
				return "OK connecting to " + host;
			}

			case "disconnect": {
				if (mc.level == null) {
					return "ERR not in a world";
				}
				mc.level.disconnect(Component.literal("DevBridge disconnect"));
				return "OK";
			}

			case "clearchat": {
				mc.gui.hud.getChat().clearMessages(true);
				return "OK";
			}

			case "screenshot": {
				String name = rest.isEmpty() ? "devbridge" : rest;
				return DevScreenshot.grab(name);
			}

			default:
				return "ERR unknown command '" + command + "'. Try: ping, state, modules, enable, disable, toggle, "
						+ "settings, set, cmd, chat, look, connect, disconnect, clearchat, hidegui, screenshot";
		}
	}

	private static void describe(StringBuilder sb, String index, ModuleSetting<?> setting) {
		sb.append(index).append(' ').append(setting.getName()).append(' ');

		if (setting instanceof SettingToggle toggle) {
			sb.append("toggle=").append(toggle.getState());
			if (!toggle.getChildren().isEmpty()) {
				sb.append(" children=").append(toggle.getChildren().size());
			}
		} else if (setting instanceof SettingSlider slider) {
			sb.append("slider=").append(slider.getValue()).append(" range=").append(slider.min).append('-').append(slider.max);
		} else if (setting instanceof SettingMode mode) {
			sb.append("mode=").append(mode.modes[mode.getMode()]).append(" options=").append(String.join(",", mode.modes));
		} else if (setting instanceof SettingColor color) {
			int[] rgb = color.getRGBArray();
			sb.append("color=").append(rgb[0]).append(',').append(rgb[1]).append(',').append(rgb[2]);
		} else {
			sb.append(setting.getClass().getSimpleName()).append('=').append(setting.getValue());
		}

		sb.append('\n');
	}

	private static ModuleSetting<?> resolve(Module module, String path) {
		try {
			String[] parts = path.split("\\.");
			ModuleSetting<?> setting = module.getSetting(Integer.parseInt(parts[0]));

			if (parts.length > 1 && setting instanceof SettingToggle toggle) {
				setting = toggle.getChild(Integer.parseInt(parts[1]));
			}

			return setting;
		} catch (Exception e) {
			return null;
		}
	}

	private static String apply(ModuleSetting<?> setting, String value) {
		if (setting instanceof SettingToggle toggle) {
			boolean target = value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("on");
			if (toggle.getState() != target) {
				toggle.setValue(target);
			}
			return "OK " + setting.getName() + "=" + toggle.getState();
		}

		if (setting instanceof SettingSlider slider) {
			slider.setValue(Double.parseDouble(value));
			return "OK " + setting.getName() + "=" + slider.getValue();
		}

		if (setting instanceof SettingMode mode) {
			for (int i = 0; i < mode.modes.length; i++) {
				if (mode.modes[i].equalsIgnoreCase(value)) {
					mode.setValue(i);
					return "OK " + setting.getName() + "=" + mode.modes[i];
				}
			}
			mode.setValue(Integer.parseInt(value));
			return "OK " + setting.getName() + "=" + mode.modes[mode.getMode()];
		}

		if (setting instanceof SettingColor color) {
			String[] rgb = value.split("[ ,]+");
			color.setRGB(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
			int[] out = color.getRGBArray();
			return "OK " + setting.getName() + "=" + out[0] + "," + out[1] + "," + out[2];
		}

		return "ERR cannot set " + setting.getClass().getSimpleName() + " yet";
	}
}
