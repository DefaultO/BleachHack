/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.gui;

import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.server.packs.PackType;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.math.NumberUtils;


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

		addButton = addDrawableChild(Button.builder(Component.literal("Done"), button -> {
			int i = Integer.parseInt(protocolField.getText());
			int i1 = Integer.parseInt(packVerField.getText());

			DetectedVersion version = (DetectedVersion) SharedConstants.getGameVersion();
			version.name = versionField.getText();
			// version.releaseTarget = versionField.getText();
			version.protocolVersion =  i;
			version.dataPackVersion = i1;
			BRAND = brandField.getText();

			close();
		}).position(width / 2 - 100, height / 2 + 50).size(196, 20).build());

		addDrawableChild(Button.builder(Component.literal("Cancel"),
				button -> close()).position(width / 2 - 100, height / 2 + 73).size(196, 20).build());

		versionField = addDrawableChild(new EditBox(textRenderer, width / 2 - 98, height / 2 - 60, 196, 18, Component.empty()));
		versionField.setText(SharedConstants.getGameVersion().getName());

		protocolField = addDrawableChild(new EditBox(textRenderer, width / 2 - 98, height / 2 - 35, 196, 18, Component.empty()));
		protocolField.setText(Integer.toString(SharedConstants.getProtocolVersion()));
		protocolField.setChangedListener(text -> updateAddButton());

		packVerField = addDrawableChild(new EditBox(textRenderer, width / 2 - 98, height / 2 - 10, 196, 18, Component.empty()));
		packVerField.setText(Integer.toString(SharedConstants.getGameVersion().getResourceVersion(PackType.CLIENT_RESOURCES)));
		packVerField.setChangedListener(text -> updateAddButton());
		
		brandField = addDrawableChild(new EditBox(textRenderer, width / 2 - 98, height / 2 + 15, 128, 18, Component.empty()));
		brandField.setText(ClientBrandRetriever.getClientModName());
		
		addDrawableChild(Button.builder(Component.literal("V"),
				button -> brandField.setText("vanilla")).position(width / 2 + 33, height / 2 + 14).size(20, 20).build());
		addDrawableChild(Button.builder(Component.literal("Fa"),
				button -> brandField.setText("fabric")).position(width / 2 + 56, height / 2 + 14).size(20, 20).build());
		addDrawableChild(Button.builder(Component.literal("Fo"),
				button -> brandField.setText("forge")).position(width / 2 + 79, height / 2 + 14).size(20, 20).build());
	}

	public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
		renderBackground(drawContext, mouseX, mouseY, delta);
		drawContext.drawTextWithShadow(textRenderer, "NOTE: This will not make the game compatible with other versions", width / 5, 5, 0xaaaaaa);
		drawContext.drawTextWithShadow(textRenderer, "It will only change what the client says it is to servers.", width / 5, 15, 0xaaaaaa);
		drawContext.drawTextWithShadow(textRenderer, "Version:", width / 2 - 103 - textRenderer.getWidth("Version:"), height / 2 - 55, 0xaaaaaa);
		drawContext.drawTextWithShadow(textRenderer, "Protocol:", width / 2 - 103 - textRenderer.getWidth("Protocol:"), height / 2 - 30, 0xaaaaaa);
		drawContext.drawTextWithShadow(textRenderer, "Pack Ver:", width / 2 - 103 - textRenderer.getWidth("Pack Ver:"), height / 2 - 5, 0xaaaaaa);
		drawContext.drawTextWithShadow(textRenderer, "Brand:", width / 2 - 103 - textRenderer.getWidth("Brand:"), height / 2 + 20, 0xaaaaaa);

		super.render(drawContext, mouseX, mouseY, delta);
	}

	public void close() {
		client.setScreen(parent);
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
		addButton.active = NumberUtils.isDigits(protocolField.getText()) && NumberUtils.isDigits(packVerField.getText());
	}
}
