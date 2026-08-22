/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bleachhack.gui.window.Window;
import org.bleachhack.gui.window.WindowScreen;
import org.bleachhack.util.collections.MutablePairList;
import org.bleachhack.util.io.BleachFileHelper;

public class EntityMenuEditScreen extends WindowScreen {

	private MutablePairList<String, String> interactions;
	private String selectedEntry;
	private String hoverEntry;
	private String deleteEntry;

	private int scroll;
	private int scrollOffset;

	private boolean addEntry;

	private String insertString;
	private String insertStartString;

	private EditBox editNameField;
	private EditBox editValueField;

	public EntityMenuEditScreen(MutablePairList<String, String> interactions) {
		super(Component.literal("Interaction Edit Screen"));

		this.interactions = interactions;
	}

	@Override
	public void init() {
		super.init();

		addWindow(new Window(
				width / 4,
				height / 6,
				width - width / 4,
				height - height / 6,
				"Edit Interactions", (java.util.function.Supplier<ItemStack>) () -> new ItemStack(Items.OAK_SIGN)));

		if (editNameField == null) {
			editNameField = new EditBox(font, 0, 0, 1000, 16, Component.empty());
		}

		if (editValueField == null) {
			editValueField = new EditBox(font, 0, 0, 1000, 16, Component.empty());
		}
	}

	// the old render() override only drew the background first — 26.2 already calls
	// extractBackground before extractRenderState, so no override is needed

	@SuppressWarnings("unchecked")
	@Override
	public void onRenderWindow(GuiGraphicsExtractor drawContext, int window, int mouseX, int mouseY) {
		super.onRenderWindow(drawContext, window, mouseX, mouseY);

		if (window == 0) {
			int x = getWindow(0).x1;
			int y = getWindow(0).y1 + 12;
			int w = getWindow(0).x2 - getWindow(0).x1;
			int h = getWindow(0).y2 - getWindow(0).y1 - 13;

			hoverEntry = null;
			deleteEntry = null;
			scrollOffset = 0;
			addEntry = false;
			insertString = null;
			insertStartString = null;

			int seperator = (int) (x + w / 3.25);
			drawContext.fill(seperator, y, seperator + 1, y + h, 0xff606090);

			drawContext.text(font, "Interactions:", x + 6, y + 5, 0xffffffff);

			boolean mouseOverAdd = mouseX >= seperator - 16 && mouseX <= seperator - 3 && mouseY >= y + 3 && mouseY <= y + 15;
			Window.fill(drawContext, seperator - 16, y + 3, seperator - 3, y + 15,
					mouseOverAdd ? 0x4fb070f0 : 0x60606090);
			drawContext.text(font, "§a+", seperator - 12, y + 5, 0xffffffff);

			if (mouseOverAdd) {
				addEntry = true;
			}

			int maxEntries = (h - 33) / 17;
			int entries = 0;

			scroll = Mth.clamp(scroll, 0, interactions.size() - maxEntries);

			if (scroll > 0) {
				boolean mouseOver = mouseX >= x + 2 && mouseX <= seperator - 1 && mouseY >= y + 17 && mouseY <= y + 33;

				Window.fill(drawContext, x + 3, y + 17, seperator - 2, y + 33,
						mouseOver ? 0x4fb070f0 : 0x50606090);
				drawContext.text(font, "§a§l^", x + (seperator - x) / 2, y + 21, 0xffffffff);

				entries++;
				if (mouseOver) {
					scrollOffset = -1;
				}
			}

			if (interactions.size() - maxEntries > 0 && scroll < interactions.size() - maxEntries) {
				boolean mouseOver = mouseX >= x + 2 && mouseX <= seperator - 1 && mouseY >= y + 17 + (maxEntries * 17) && mouseY <= y + 33 + (maxEntries * 17);

				Window.fill(drawContext, x + 3, y + 17 + (maxEntries * 17), seperator - 2, y + 33 + (maxEntries * 17),
						mouseOver ? 0x4fb070f0 : 0x50606090);
				drawContext.text(font, "§a§lv", x + (seperator - x) / 2, y + 21 + (maxEntries * 17), 0xffffffff);

				maxEntries--;
				if (mouseOver) {
					scrollOffset = 1;
				}
			}

			int localScroll = scroll;
			for (String entry: interactions.getEntries()) {
				if (entries < localScroll) {
					localScroll--;
					continue;
				}

				int curY = y + 17 + entries * 17;
				boolean mouseOver = mouseX >= x + 2 && mouseX <= seperator - 1 && mouseY >= curY && mouseY <= curY + 16;

				Window.fill(drawContext, x + 3, curY, seperator - 2, curY + 16,
						entry.equals(selectedEntry) ? 0x4f90f090 : mouseOver ? 0x4fb070f0 : 0x50606090);
				drawContext.text(font, font.plainSubstrByWidth(entry, seperator - x - 6), x + (seperator - x) / 2, curY + 4, 0xffffffff);

				if (mouseOver) {
					hoverEntry = entry;
				}

				entries++;
				if (entries > maxEntries) {
					break;
				}
			}

			if (selectedEntry != null) {
				drawContext.text(font, "Name:", seperator + 8, y + 5, 0xffffffff);

				editNameField.setX(seperator + 8);
				editNameField.setY(y + 18);
				editNameField.setWidth(w - (seperator - x) - 16);
				editNameField.extractRenderState(drawContext, mouseX, mouseY, 0f);

				drawContext.text(font, "Value:", seperator + 8, y + 45, 0xffffffff);

				editValueField.setX(seperator + 8);
				editValueField.setY(y + 57);
				editValueField.setWidth(w - (seperator - x) - 16);
				editValueField.extractRenderState(drawContext, mouseX, mouseY, 0f);

				if (!selectedEntry.equals(editNameField.getValue()) && !interactions.containsKey(editNameField.getValue())) {
					MutablePair<String, String> pair = interactions.getPair(selectedEntry);
					selectedEntry = editNameField.getValue();
					pair.setLeft(selectedEntry);
				}

				if (!interactions.getValue(selectedEntry).equals(editValueField.getValue())) {
					interactions.getPair(selectedEntry).setRight(editValueField.getValue());
				}

				drawContext.text(font, "Insert:", seperator + 8, y + 85, 0xffffffff);

				int line = 0;
				int curX = 0;
				for (String insert: new String[] { "%name%", "%uuid%", "%health%", "%x%", "%y%", "%z%"}) {
					int textLen = font.width(insert);

					if (seperator + 9 + curX + textLen > x + w) {
						line++;
						curX = 0;
					}

					boolean mouseOverInsert = mouseX >= seperator + 7 + curX && mouseX <= seperator + 10 + curX + textLen && mouseY >= y + 97 + line * 14 && mouseY <= y + 108 + line * 14;
					drawContext.fill(seperator + 7 + curX, y + 97 + line * 14, seperator + 10 + curX + textLen, y + 108 + line * 14, mouseOverInsert ? 0x9f6060b0 : 0x9f8070b0);
					drawContext.text(font, insert, seperator + 9 + curX, y + 99 + line * 14, 0xffffffff);


					if (mouseOverInsert) {
						insertString = insert;
					}

					curX += textLen + 7;
				}

				drawContext.text(font, "Mode:", seperator + 8, y + 120 + line * 14, 0xffffffff);

				int startY = y + 132 + line * 14;
				line = 0;
				curX = 0;
				for (Pair<String, String> pair: new Pair[] { Pair.of("Normal", ""), Pair.of("Suggest", ">suggest "), Pair.of("Open Url", ">url ") }) {
					int textLen = font.width(pair.getLeft());

					if (seperator + 9 + curX + textLen > x + w) {
						line++;
						curX = 0;
					}

					boolean mouseOverInsert = mouseX >= seperator + 7 + curX && mouseX <= seperator + 10 + curX + textLen && mouseY >= startY + line * 14 && mouseY <= startY + 11 + line * 14;
					drawContext.fill(seperator + 7 + curX, startY + line * 14, seperator + 10 + curX + textLen, startY + 11 + line * 14, mouseOverInsert ? 0x9f6060b0 : 0x9f8070b0);
					drawContext.text(font, pair.getLeft(), seperator + 9 + curX, startY + 2 + line * 14, 0xffffffff);

					if (mouseOverInsert) {
						insertStartString = pair.getRight();
					}

					curX += textLen + 7;
				}

				boolean mouseOverDelete = mouseX >= x + w - 70 && mouseX <= x + w - 5 && mouseY >= y + h - 22 && mouseY <= y + h - 4;
				Window.fill(drawContext, x + w - 70, y + h - 22, x + w - 5, y + h - 4, 0x60e05050, 0x60c07070, mouseOverDelete ? 0x20e05050 : 0x10e07070);
				drawContext.text(font, "Delete", x + w - 37, y + h - 17, 0xfff0f0f0);

				if (mouseOverDelete) {
					deleteEntry = selectedEntry;
				}
			}
		}
	}

	@Override
	public void onClose() {
		JsonObject json = new JsonObject();
		for (MutablePair<String, String> entry: interactions) {
			json.add(entry.getLeft(), new JsonPrimitive(entry.getRight()));
		}

		BleachFileHelper.saveMiscSetting("entityMenu", json);

		super.onClose();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		editNameField.setFocused(editNameField.mouseClicked(event, doubleClick));
		editValueField.setFocused(editValueField.mouseClicked(event, doubleClick));

		if (hoverEntry != null && interactions.containsKey(hoverEntry)) {
			selectedEntry = hoverEntry;
			hoverEntry = null;

			editNameField.setValue(selectedEntry);
			editValueField.setValue(interactions.getValue(selectedEntry));
		}

		if (deleteEntry != null) {
			interactions.removeKey(deleteEntry);
			deleteEntry = null;
			selectedEntry = null;
		}

		if (scrollOffset != 0) {
			scroll += scrollOffset;
			scrollOffset = 0;
		}

		if (addEntry) {
			String name = "New Interaction";
			for (int toAdd = 1; interactions.containsKey(name); toAdd++) {
				name = "New Interaction (" + toAdd + ")";
			}

			interactions.add(name, "Hi %name%");
			addEntry = false;
		}

		if (insertString != null) {
			editValueField.insertText(insertString);
			insertString = null;
		}

		if (insertStartString != null) {
			if (editValueField.getValue().startsWith(">")) {
				editValueField.setValue(editValueField.getValue().replaceFirst(">.*? ", ""));
			}

			editValueField.setValue(insertStartString + editValueField.getValue());
			insertString = null;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (editNameField.isFocused()) editNameField.charTyped(event);
		if (editValueField.isFocused()) editValueField.charTyped(event);

		return super.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (editNameField.isFocused()) editNameField.keyPressed(event);
		if (editValueField.isFocused()) editValueField.keyPressed(event);

		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
