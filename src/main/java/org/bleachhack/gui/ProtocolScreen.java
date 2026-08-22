/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.gui;

import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.math.NumberUtils;

import java.lang.reflect.Field;


public class ProtocolScreen extends Screen {

	public static String BRAND = null;

	private EditBox versionField;
	private EditBox protocolField; // int
	private EditBox packVerField; // int
	private EditBox brandField;
	private Button addButton;
	private Screen parent;

	public ProtocolScreen(Screen parent) {
		super(Component.literal("Protocol Screen"));
		this.parent = parent;
	}

	public void init() {
		super.init();

		addButton = addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
			int i = Integer.parseInt(protocolField.getValue());
			int i1 = Integer.parseInt(packVerField.getValue());

			// 26.2: WorldVersion is an immutable record and SharedConstants.setVersion refuses
			// overrides, so build a spoofed copy and swap the private CURRENT_VERSION field
			WorldVersion cur = SharedConstants.getCurrentVersion();
			WorldVersion spoofed = new WorldVersion.Simple(
					cur.id(),
					versionField.getValue(),
					cur.dataVersion(),
					i,
					PackFormat.of(i1, cur.packVersion(PackType.CLIENT_RESOURCES).minor()),
					cur.packVersion(PackType.SERVER_DATA),
					cur.buildTime(),
					cur.stable());

			try {
				Field field = SharedConstants.class.getDeclaredField("CURRENT_VERSION");
				field.setAccessible(true);
				field.set(null, spoofed);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Failed to spoof game version", e);
			}

			BRAND = brandField.getValue();

			onClose();
		}).pos(width / 2 - 100, height / 2 + 50).size(196, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Cancel"),
				button -> onClose()).pos(width / 2 - 100, height / 2 + 73).size(196, 20).build());

		versionField = addRenderableWidget(new EditBox(font, width / 2 - 98, height / 2 - 60, 196, 18, Component.empty()));
		versionField.setValue(SharedConstants.getCurrentVersion().name());

		protocolField = addRenderableWidget(new EditBox(font, width / 2 - 98, height / 2 - 35, 196, 18, Component.empty()));
		protocolField.setValue(Integer.toString(SharedConstants.getProtocolVersion()));
		protocolField.setResponder(text -> updateAddButton());

		packVerField = addRenderableWidget(new EditBox(font, width / 2 - 98, height / 2 - 10, 196, 18, Component.empty()));
		packVerField.setValue(Integer.toString(SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES).major()));
		packVerField.setResponder(text -> updateAddButton());

		brandField = addRenderableWidget(new EditBox(font, width / 2 - 98, height / 2 + 15, 128, 18, Component.empty()));
		brandField.setValue(ClientBrandRetriever.getClientModName());

		addRenderableWidget(Button.builder(Component.literal("V"),
				button -> brandField.setValue("vanilla")).pos(width / 2 + 33, height / 2 + 14).size(20, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Fa"),
				button -> brandField.setValue("fabric")).pos(width / 2 + 56, height / 2 + 14).size(20, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Fo"),
				button -> brandField.setValue("forge")).pos(width / 2 + 79, height / 2 + 14).size(20, 20).build());
	}

	public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
		// background is drawn by the vanilla extractBackground pass before this is called
		drawContext.text(font, "NOTE: This will not make the game compatible with other versions", width / 5, 5, 0xffaaaaaa);
		drawContext.text(font, "It will only change what the client says it is to servers.", width / 5, 15, 0xffaaaaaa);
		drawContext.text(font, "Version:", width / 2 - 103 - font.width("Version:"), height / 2 - 55, 0xffaaaaaa);
		drawContext.text(font, "Protocol:", width / 2 - 103 - font.width("Protocol:"), height / 2 - 30, 0xffaaaaaa);
		drawContext.text(font, "Pack Ver:", width / 2 - 103 - font.width("Pack Ver:"), height / 2 - 5, 0xffaaaaaa);
		drawContext.text(font, "Brand:", width / 2 - 103 - font.width("Brand:"), height / 2 + 20, 0xffaaaaaa);

		super.extractRenderState(drawContext, mouseX, mouseY, delta);
	}

	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	public void tick() {
		super.tick();
		//versionField.tick();
		//protocolField.tick();
		//packVerField.tick();
		//brandField.tick();

		super.tick();
	}

	private void updateAddButton() {
		addButton.active = NumberUtils.isDigits(protocolField.getValue()) && NumberUtils.isDigits(packVerField.getValue());
	}
}
