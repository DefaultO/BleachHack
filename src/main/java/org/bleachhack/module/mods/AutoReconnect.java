/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.bleachhack.event.events.EventOpenScreen;
import org.bleachhack.event.events.EventPacket;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.chat.Component;


public class AutoReconnect extends Module {

	public ServerData server;

	public AutoReconnect() {
		super("AutoReconnect", KEY_UNBOUND, ModuleCategory.MISC, "Shows reconnect options when disconnecting from a server.",
				new SettingToggle("Auto", true).withDesc("Automatically reconnects.").withChildren(
						new SettingSlider("Delay", 0.2, 10, 5, 2).withDesc("How long to wait before reconnecting (in seconds).")));
	}

	@BleachSubscribe
	public void onOpenScreen(EventOpenScreen event) {
		if (event.getScreen() instanceof DisconnectedScreen
				&& !(event.getScreen() instanceof NewDisconnectScreen)) {
			mc.gui.setScreen(new NewDisconnectScreen((DisconnectedScreen) event.getScreen()));
			event.setCancelled(true);
		}
	}

	@BleachSubscribe
	public void sendPacket(EventPacket.Send event) {
		if (event.getPacket() instanceof ClientIntentionPacket) {
			ClientIntentionPacket packet = (ClientIntentionPacket) event.getPacket();
			server = new ServerData("Server", packet.hostName() + ":" + packet.port(), ServerData.Type.LAN);
		}
	}

	public class NewDisconnectScreen extends DisconnectedScreen {

		public long reconnectTime = Long.MAX_VALUE - 1000000L;

		private Button reconnectButton;

		public NewDisconnectScreen(DisconnectedScreen screen) {
			// TODO(26.2): DisconnectedScreen.parent/details are private now - the parent falls back to the
			// multiplayer screen and the reason is recovered from the narration message (title + reason).
			super(new JoinMultiplayerScreen(new TitleScreen()), screen.getTitle(), screen.getNarrationMessage());
		}

		public void init() {
			super.init();

			reconnectTime = System.currentTimeMillis();
			int buttonH = Math.min(height / 2 + this.height / 2 + 9, height - 30);

			addRenderableWidget(Button.builder(Component.literal("Reconnect"), button -> {
				if (server != null)
					ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), minecraft, ServerAddress.parseString(server.ip), server, false, null);
			}).pos(width / 2 - 100, buttonH + 22).size(200, 20).build());
			reconnectButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
				getSetting(0).asToggle().setValue(!getSetting(0).asToggle().getState());
				reconnectTime = System.currentTimeMillis();
			}).pos(width / 2 - 100, buttonH + 44).size(200, 20).build());
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			super.extractRenderState(graphics, mouseX, mouseY, delta);

			int startTime = (int) (getSetting(0).asToggle().getChild(0).asSlider().getValue() * 1000);
			reconnectButton.setMessage(Component.literal(
					getSetting(0).asToggle().getState()
					? "§aAutoReconnect [" + (reconnectTime + startTime - System.currentTimeMillis()) + "]"
							: "§cAutoReconnect [" + startTime + "]"));

			if (reconnectTime + startTime < System.currentTimeMillis() && getSetting(0).asToggle().getState()) {
				if (server != null)
					ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), minecraft, ServerAddress.parseString(server.ip), server, false, null);
			}
		}

	}

}
