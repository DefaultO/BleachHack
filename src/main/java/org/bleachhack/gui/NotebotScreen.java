/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;
import org.bleachhack.gui.window.Window;
import org.bleachhack.gui.window.WindowScreen;
import org.bleachhack.gui.window.widget.WindowButtonWidget;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.Notebot;
import org.bleachhack.module.mods.Notebot.Song;
import org.bleachhack.util.NotebotUtils;
import org.bleachhack.util.io.BleachFileMang;

import java.net.URI;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class NotebotScreen extends WindowScreen {

	private Set<String> files = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	private int page = 0;

	private Song entry;
	private boolean playing;
	private int playTick;

	public NotebotScreen() {
		super(Component.literal("Notebot Gui"));
	}

	public void init() {
		super.init();

		files.clear();

		BleachFileMang.getDir().resolve("notebot/").toFile().mkdirs();
		for (String f: BleachFileMang.getDir().resolve("notebot/").toFile().list())
			files.add(f);

		int ww = Math.max(width / 2, 360);
		int wh = Math.max(height / 2, 200);
		addWindow(new Window(
				width / 2 - ww / 2,
				height / 2 - wh / 2,
				width / 2 + ww / 2,
				height / 2 + wh / 2,
				"Notebot Gui", new ItemStack(Items.NOTE_BLOCK)));

		getWindow(0).addWidget(new WindowButtonWidget(22, 14, 32, 24, "<", () -> page = page <= 0 ? 0 : page - 1));
		getWindow(0).addWidget(new WindowButtonWidget(77, 14, 87, 24, ">", () -> page++));

		int xEnd = getWindow(0).x2 - getWindow(0).x1;

		getWindow(0).addWidget(new WindowButtonWidget(xEnd - 30, 14, xEnd - 3, 24, "Help", () ->
		Util.getPlatform().openUri(URI.create("https://www.youtube.com/watch?v=Z6O80jItoAk"))));
	}

	// no extractRenderState override needed: the framework draws the background and windows

	public void onRenderWindow(GuiGraphicsExtractor drawContext, int window, int mouseX, int mouseY) {
		super.onRenderWindow(drawContext, window, mouseX, mouseY);

		if (window == 0) {
			int x = getWindow(0).x1;
			int y = getWindow(0).y1 + 10;
			int w = getWindow(0).x2 - x;
			int h = getWindow(0).y2 - y;

			int pageEntries = 0;
			for (int i = y + 20; i < y + h - 27; i += 10)
				pageEntries++;

			drawContext.text(font, "Page " + (page + 1), x + 55, y + 5, 0xffc0c0ff);

			fillButton(drawContext, x + 10, y + h - 13, x + 99, y + h - 3, 0xff3a3a3a, 0xff353535, mouseX, mouseY);
			drawContext.text(font, "Download Songs..", x + 55, y + h - 12, 0xffc0dfdf);

			Song nbSong = ModuleManager.getModule(Notebot.class).song;
			int c = 0, c1 = -1;
			for (String s : files) {
				c1++;
				if (c1 < page * pageEntries)
					continue;
				if (c1 > (page + 1) * pageEntries)
					break;

				fillButton(drawContext, x + 5, y + 15 + c * 10, x + 105, y + 25 + c * 10,
						nbSong != null && s.equals(nbSong.filename) ? 0xf0408040 : entry != null && s.equals(entry.filename) ? 0xf0202020 : 0xf0404040, 0xf0303030, mouseX, mouseY);

				drawContext.text(font, font.plainSubstrByWidth(s, 100), x + 55, y + 16 + c * 10, -1);

				c++;
			}

			if (entry != null) {
				int textX = x + w - w / 4;
				drawContext.text(font,  entry.name, textX, y + 8, 0xffffffff);
				drawContext.text(font, "By: " + entry.author, textX, y + 18, 0xffb0b0b0);

				drawContext.text(font,"Format: §a" + entry.format, textX, y + 35, 0xffb0b0b0);
				drawContext.text(font, "Length: §f" +  entry.length / 20 + "s", textX, y + 45, 0xffb0b0b0);
				//drawCenteredText(matrices, textRenderer, "Notes: §f" + entry.notes.size(), textX, y + 55, 0xb0b0b0);
				drawContext.text(font, "Noteblocks: ", textX, y + 62, 0xff80f080);

				int c2 = 0;
				for (Entry<NoteBlockInstrument, ItemStack> e : NotebotUtils.INSTRUMENT_TO_ITEM.entrySet()) {
					int count = (int) entry.requirements.stream().filter(n -> n.instrument == e.getKey().ordinal()).count();

					if (count != 0) {
						drawContext.text(font, StringUtils.capitalize(e.getKey().getSerializedName()) + " x" + count,
								textX, y + 74 + c2 * 10, 0xff50f050);

						// 26.2: item() manages its own gui lighting; the old Lighting calls are gone
						drawContext.item(e.getValue(), textX + 55, y + 70 + c2 * 10);

						c2++;
					}
				}

				fillButton(drawContext, x + w - w / 2 + 10, y + h - 15, x + w - w / 4, y + h - 5, 0xff903030, 0xff802020, mouseX, mouseY);
				fillButton(drawContext, x + w - w / 4 + 5, y + h - 15, x + w - 5, y + h - 5, 0xff308030, 0xff207020, mouseX, mouseY);
				fillButton(drawContext, x + w - w / 4 - w / 8, y + h - 27, x + w - w / 4 + w / 8, y + h - 17, 0xff303080, 0xff202070, mouseX, mouseY);

				int pixels = (int) Math.round(Mth.clamp((w / 4d) * ((double) playTick / (double) entry.length), 0, w / 4d));
				drawContext.fill(x + w - w / 4 - w / 8, y + h - 27, (x + w - w / 4 - w / 8) + pixels, y + h - 17, 0x507050ff);

				drawContext.text(font, "Delete", (int) (x + w - w / 2.8), y + h - 14, 0xffff0000);
				drawContext.text(font, "Select", x + w - w / 8, y + h - 14, 0xff00ff00);
				drawContext.text(font, playing ? "Previewing.." : "Preview", x + w - w / 4, y + h - 26, 0xff6060ff);
			}
		}
	}

	public void tick() {
		if (entry != null && playing) {
			playTick++;
			NotebotUtils.playNote(entry.notes, playTick);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();

		if (!getWindow(0).closed) {
			int x = getWindow(0).x1;
			int y = getWindow(0).y1 + 10;
			int w = getWindow(0).x2 - x;
			int h = getWindow(0).y2 - y;

			if (mouseX > x + 10 && mouseX < x + 99 && mouseY > y + h - 13 && mouseY < y + h - 3) {
				NotebotUtils.downloadSongs(true);
				init();
			}

			if (entry != null) {
				/* Pfft why use buttons when you can use meaningless rectangles with messy code */
				if (mouseX > x + w - w / 2 + 10 && mouseX < x + w - w / 4 && mouseY > y + h - 15 && mouseY < y + h - 5) {
					BleachFileMang.deleteFile("notebot/" + entry.filename);
					minecraft.setScreen(this);
				}
				if (mouseX > x + w - w / 4 + 5 && mouseX < x + w - 5 && mouseY > y + h - 15 && mouseY < y + h - 5) {
					ModuleManager.getModule(Notebot.class).song = entry;
				}
				if (mouseX > x + w - w / 4 - w / 8 && mouseX < x + w - w / 4 + w / 8 && mouseY > y + h - 27 && mouseY < y + h - 17) {
					playing = !playing;
				}
			}

			int pageEntries = 0;
			for (int i = y + 20; i < y + h - 27; i += 10)
				pageEntries++;

			int c = 0;
			int c1 = -1;
			for (String s : files) {
				c1++;
				if (c1 < page * pageEntries)
					continue;
	
				if (mouseX > x + 5 && mouseX < x + 105 && mouseY > y + 15 + c * 10 && mouseY < y + 25 + c * 10) {
					entry = NotebotUtils.parse(BleachFileMang.getDir().resolve("notebot/" + s));
					playing = false;
					playTick = 0;
					break;
				}

				c++;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	private void fillButton(GuiGraphicsExtractor drawContext, int x1, int y1, int x2, int y2, int color, int colorHover, int mouseX, int mouseY) {
		drawContext.fill(x1, y1, x2, y2, (mouseX > x1 && mouseX < x2 && mouseY > y1 && mouseY < y2 ? colorHover : color));
	}
}
