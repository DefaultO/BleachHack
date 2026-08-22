/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.command.commands;

import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;

import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.ChatFormatting;
import org.apache.commons.lang3.StringUtils;
import org.bleachhack.BleachHack;
import org.bleachhack.command.Command;
import org.bleachhack.command.CommandCategory;
import org.bleachhack.command.exception.CmdSyntaxException;
import org.bleachhack.event.events.EventPacket;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.util.BleachLogger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CmdServer extends Command {

	public CmdServer() {
		super("server", "Server things.", "server address | server brand | server day | server difficulty | server ip | server motd | server ping | server permissions | server plugins | server protocol | server version", CommandCategory.MISC);
	}

	@Override
	public void onCommand(String alias, String[] args) throws CmdSyntaxException {
		boolean sp = mc.isLocalServer();

		if (!sp && mc.getCurrentServer() == null) {
			BleachLogger.error("Unable to get server info.");
			return;
		}

		BleachLogger.info("Server Info");

		if (args.length == 0) {
			BleachLogger.noPrefix(createText("Address", getAddress(sp)));
			BleachLogger.noPrefix(createText("Brand", getBrand(sp)));
			BleachLogger.noPrefix(createText("Day", getDay(sp)));
			BleachLogger.noPrefix(createText("Difficulty", getDifficulty(sp)));
			BleachLogger.noPrefix(createText("IP", getIP(sp)));
			BleachLogger.noPrefix(createText("Motd", getMotd(sp)));
			BleachLogger.noPrefix(createText("Ping", getPing(sp)));
			BleachLogger.noPrefix(createText("Permission Level", getPerms(sp)));
			BleachLogger.noPrefix(createText("Protocol", getProtocol(sp)));
			BleachLogger.noPrefix(createText("Version", getVersion(sp)));
			checkForPlugins();
		} else if (args[0].equalsIgnoreCase("address")) {
			BleachLogger.noPrefix(createText("Address", getAddress(sp)));
		} else if (args[0].equalsIgnoreCase("brand")) {
			BleachLogger.noPrefix(createText("Brand", getBrand(sp)));
		} else if (args[0].equalsIgnoreCase("day")) {
			BleachLogger.noPrefix(createText("Day", getDay(sp)));
		} else if (args[0].equalsIgnoreCase("difficulty")) {
			BleachLogger.noPrefix(createText("Difficulty", getDifficulty(sp)));
		} else if (args[0].equalsIgnoreCase("ip")) {
			BleachLogger.noPrefix(createText("IP", getIP(sp)));
		} else if (args[0].equalsIgnoreCase("motd")) {
			BleachLogger.noPrefix(createText("Motd", getMotd(sp)));
		} else if (args[0].equalsIgnoreCase("ping")) {
			BleachLogger.noPrefix(createText("Ping", getPing(sp)));
		} else if (args[0].equalsIgnoreCase("permissions")) {
			BleachLogger.noPrefix(createText("Permission Level", getPerms(sp)));
		} else if (args[0].equalsIgnoreCase("plugins")) {
			checkForPlugins();
		} else if (args[0].equalsIgnoreCase("protocol")) {
			BleachLogger.noPrefix(createText("Protocol", getProtocol(sp)));
		} else if (args[0].equalsIgnoreCase("version")) {
			BleachLogger.noPrefix(createText("Version", getVersion(sp)));
		} else {
			throw new CmdSyntaxException("Invalid server bruh.");
		}
	}

	@BleachSubscribe
	public void onReadPacket(EventPacket.Read event) {
		if (event.getPacket() instanceof ClientboundCommandSuggestionsPacket) {
			BleachHack.eventBus.unsubscribe(this);

			ClientboundCommandSuggestionsPacket packet = (ClientboundCommandSuggestionsPacket) event.getPacket();
			List<String> plugins = packet.suggestions().stream()
					.map(s -> {
						String[] split = s.text().split(":");
						return split.length != 1 ? split[0].replace("/", "") : null;
					})
					.filter(Objects::nonNull)
					.distinct()
					.sorted()
					.collect(Collectors.toList());

			if (!plugins.isEmpty()) {
				BleachLogger.noPrefix(createText("Plugins §f(" + plugins.size() + ")", "§a" + String.join("§f, §a", plugins)));
			} else {
				BleachLogger.noPrefix("§cNo plugins found");
			}
		}
	}

	public Component createText(String name, String value) {
		boolean newlines = value.contains("\n");
		return Component.literal("§7" + name + "§f:" + (newlines ? "\n" : " " ) + "§a" + value).withStyle(style -> style
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy to clipboard")))
				.withClickEvent(new ClickEvent.CopyToClipboard(ChatFormatting.stripFormatting(value))));
	}

	public void checkForPlugins() {
		BleachHack.eventBus.subscribe(this); // Plugins
		mc.player.connection.send(new ServerboundCommandSuggestionPacket(0, "/"));

		Thread timeoutThread = new Thread(() -> {
			try {
				Thread.sleep(5000);
				if (BleachHack.eventBus.unsubscribe(this))
					BleachLogger.noPrefix("§cPlugin check timed out");
			} catch (InterruptedException ignored) {
			}
		});
		timeoutThread.setDaemon(true);
		timeoutThread.start();
	}

	public String getAddress(boolean singleplayer) {
		if (singleplayer)
			return "Singleplayer";

		return mc.getCurrentServer().ip != null ? mc.getCurrentServer().ip : "Unknown";
	}

	public String getBrand(boolean singleplayer) {
		if (singleplayer)
			return "Integrated Server";

		return mc.getConnection().serverBrand() != null ? mc.getConnection().serverBrand() : "unknown";
	}

	public String getDay(boolean singleplayer) {
		return "Day " + (mc.level.getOverworldClockTime() / 24000L);
	}

	public String getDifficulty(boolean singleplayer) {
		BlockPos pos = mc.player.blockPosition();
		float moonBrightness = DimensionType.MOON_BRIGHTNESS_PER_PHASE[mc.level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos).index()];
		DifficultyInstance localDifficulty = new DifficultyInstance(
				mc.level.getDifficulty(), mc.level.getOverworldClockTime(), mc.level.getChunkAt(pos).getInhabitedTime(), moonBrightness);
		return StringUtils.capitalize(mc.level.getDifficulty().getSerializedName()) + " (Local: " + localDifficulty.getEffectiveDifficulty() + ")";
	}

	public String getIP(boolean singleplayer) {
		try {
			if (singleplayer)
				return InetAddress.getLocalHost().getHostAddress();

			return mc.getCurrentServer().ip != null ? InetAddress.getByName(mc.getCurrentServer().ip).getHostAddress() : "Unknown";
		} catch (UnknownHostException e) {
			return "Unknown";
		}
	}

	public String getMotd(boolean singleplayer) {
		if (singleplayer)
			return "-";

		return mc.getCurrentServer().motd != null ? mc.getCurrentServer().motd.getString() : "Unknown";
	}

	public String getPing(boolean singleplayer) {
		PlayerInfo playerEntry = mc.player.connection.getPlayerInfo(mc.player.getGameProfile().id());
		return playerEntry == null ? "0" : Integer.toString(playerEntry.getLatency());
	}

	public String getPerms(boolean singleplayer) {
		// 26.2: numeric permission levels were replaced by a permission set, map back to the old 0-4 scale
		PermissionSet perms = mc.player.permissions();
		int p = perms.hasPermission(Permissions.COMMANDS_OWNER) ? 4
				: perms.hasPermission(Permissions.COMMANDS_ADMIN) ? 3
				: perms.hasPermission(Permissions.COMMANDS_GAMEMASTER) ? 2
				: perms.hasPermission(Permissions.COMMANDS_MODERATOR) ? 1 : 0;

		return switch (p) {
			case 0 -> "0 (No Perms)";
			case 1 -> "1 (No Perms)";
			case 2 -> "2 (Player Command Access)";
			case 3 -> "3 (Server Command Access)";
			case 4 -> "4 (Operator)";
			default -> p + " (Unknown)";
		};
	}

	public String getProtocol(boolean singleplayer) {
		if (singleplayer)
			return Integer.toString(SharedConstants.getProtocolVersion());

		return Integer.toString(mc.getCurrentServer().protocol);
	}

	public String getVersion(boolean singleplayer) {
		if (singleplayer)
			return SharedConstants.getCurrentVersion().name();

		return mc.getCurrentServer().version != null ? mc.getCurrentServer().version.getString() : "Unknown (" + SharedConstants.getCurrentVersion().name() + ")";
	}
}
