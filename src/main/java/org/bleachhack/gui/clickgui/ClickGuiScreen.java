/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.gui.clickgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.bleachhack.gui.clickgui.window.ClickGuiWindow;
import org.bleachhack.gui.clickgui.window.ClickGuiWindow.Tooltip;
import org.bleachhack.gui.window.Window;
import org.bleachhack.gui.window.WindowScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ClickGuiScreen extends WindowScreen {

	protected int keyDown = -1;
	protected boolean lmDown = false;
	protected boolean rmDown = false;
	protected boolean lmHeld = false;
	protected int mwScroll = 0;
	
	private int warningOpacity;

	public ClickGuiScreen(Component title) {
		super(title);
	}
	
	@Override
	public void init() {
		// super.init(); Don't call super because it clears the windows

		warningOpacity = 0;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
		// background is drawn by the vanilla flow (extractBackground) before this runs

		for (Window w : getWindows()) {
			if (w instanceof ClickGuiWindow) {
				((ClickGuiWindow) w).updateKeys(mouseX, mouseY, keyDown, lmDown, rmDown, lmHeld, mwScroll);
			}
		}

		super.extractRenderState(drawContext, mouseX, mouseY, delta);

		// was getMatrices().translate(0, 0, 250) — strata replace z-translation in 26.2
		drawContext.nextStratum();

		for (Window w : getWindows()) {
			if (w instanceof ClickGuiWindow) {
				Tooltip tooltip = ((ClickGuiWindow) w).getTooltip();

				if (tooltip != null) {
					int tooltipY = tooltip.y;

					String[] split = tooltip.text.split("\n", -1 /* Adding -1 makes it keep empty splits */);
					ArrayUtils.reverse(split);
					for (String s: split) {
						/* Match lines to end of words after it reaches 22 characters long */
						Matcher mat = Pattern.compile(".{1,22}\\b\\W*").matcher(s);

						List<String> lines = new ArrayList<>();

						while (mat.find())
							lines.add(mat.group().trim());

						if (lines.isEmpty())
							lines.add(s);

						int start = tooltipY - lines.size() * 10;
						for (int l = 0; l < lines.size(); l++) {
							drawContext.fill(tooltip.x, start + (l * 10) - 1,
									tooltip.x + font.width(lines.get(l)) + 3,
									start + (l * 10) + 9, 0xff000000);

							drawContext.text(font, lines.get(l), tooltip.x + 2, start + (l * 10), -1);
						}

						tooltipY -= lines.size() * 10;
					}
				}
			}
		}
		
		Window.fill(drawContext, width / 2 - 50, -1, width / 2 - 2, 12,
				mouseX >= width / 2 - 50 && mouseX <= width / 2 - 2 && mouseY >= 0 && mouseY <= 12 ? 0x60b070f0 : 0x60606090);
		Window.fill(drawContext, width / 2 + 2, -1, width / 2 + 50, 12,
				mouseX >= width / 2 + 2 && mouseX <= width / 2 + 50 && mouseY >= 0 && mouseY <= 12 ? 0x60b070f0 : 0x60606090);

		// 26.2 text() skips zero-alpha colors, so alpha is written out explicitly
		drawContext.text(font, "Modules", width / 2 - 26, 2, 0xfff0f0f0);
		drawContext.text(font, "UI", width / 2 + 26, 2, 0xfff0f0f0);

		if (warningOpacity > 3) {
			drawContext.text(font, "UI not available on the main menu!", width / 2, 17,
					warningOpacity > 255 ? 0xffd14a3b : (warningOpacity << 24) | 0xd14a3b);
			warningOpacity -= 3;
		}

		lmDown = false;
		rmDown = false;
		keyDown = -1;
		mwScroll = 0;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();

		if (button == 0) {
			if (mouseX >= width / 2 - 50 && mouseX <= width / 2 - 2 && mouseY >= 0 && mouseY <= 12) {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
				tryOpen(ModuleClickGuiScreen.INSTANCE);
			} else if (mouseX >= width / 2 + 2 && mouseX <= width / 2 + 50 && mouseY >= 0 && mouseY <= 12) {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
				tryOpen(UIClickGuiScreen.INSTANCE);
			} else {
				lmDown = true;
				lmHeld = true;
			}
		} else if (button == 1) {
			rmDown = true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0)
			lmHeld = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		keyDown = event.key();
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		mwScroll = (int) scrollY;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void tryOpen(Screen screen) {
		if (minecraft.level != null) {
			minecraft.gui.setScreen(screen);
		} else {
			warningOpacity = 500;
		}
	}
}
