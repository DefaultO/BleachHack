/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.gui.window;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bleachhack.gui.window.widget.WindowWidget;

import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.ConcurrentModificationException;
import java.util.List;

public class Window {

	public int x1;
	public int y1;
	public int x2;
	public int y2;

	public String title;
	public ItemStack icon;
	// ponytail: 26.2 can't build an ItemStack before item components bind (e.g. clickgui windows
	// created in postInit during Minecraft.<init>), so allow a lazy supplier resolved at render time.
	private Supplier<ItemStack> iconSupplier;

	public boolean closed;
	public boolean selected = false;

	private List<WindowWidget> widgets = new ArrayList<>();

	protected boolean dragging;
	protected int dragOffX;
	protected int dragOffY;

	public Window(int x1, int y1, int x2, int y2, String title, ItemStack icon) {
		this(x1, y1, x2, y2, title, icon, false);
	}

	public Window(int x1, int y1, int x2, int y2, String title, ItemStack icon, boolean closed) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.title = title;
		this.icon = icon;
		this.closed = closed;
	}

	public Window(int x1, int y1, int x2, int y2, String title, Supplier<ItemStack> icon) {
		this(x1, y1, x2, y2, title, icon, false);
	}

	public Window(int x1, int y1, int x2, int y2, String title, Supplier<ItemStack> icon, boolean closed) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.title = title;
		this.iconSupplier = icon;
		this.closed = closed;
	}

	public List<WindowWidget> getWidgets() {
		return widgets;
	}

	public <T extends WindowWidget> T addWidget(T widget) {
		widgets.add(widget);
		return widget;
	}

	public void render(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
		Font textRend = Minecraft.getInstance().font;

		if ((icon == null || icon.isEmpty()) && iconSupplier != null) {
			icon = org.bleachhack.util.SafeItem.resolve(iconSupplier);
		}

		if (dragging) {
			x2 = (x2 - x1) + mouseX - dragOffX - Math.min(0, mouseX - dragOffX);
			y2 = (y2 - y1) + mouseY - dragOffY - Math.min(0, mouseY - dragOffY);
			x1 = Math.max(0, mouseX - dragOffX);
			y1 = Math.max(0, mouseY - dragOffY);
		}

		drawBackground(drawContext, mouseX, mouseY, textRend);

		for (WindowWidget w : widgets) {
			if (w.shouldRender(x1, y1, x2, y2)) {
				w.render(drawContext, x1, y1, mouseX, mouseY);
			}
		}

		boolean blockItem = icon != null && icon.getItem() instanceof BlockItem;

		/* window icon */
		if (icon != null) {
			drawContext.pose().pushMatrix();
			drawContext.pose().translate(x1 + (blockItem ? 3 : 2), y1 + 2);
			drawContext.pose().scale(0.6f, 0.6f);

			drawContext.item(icon, 0, 0);
			drawContext.pose().popMatrix();
		}

		/* window title */
		drawContext.text(textRend, title,
				x1 + (icon == null || icon.getItem() == Items.AIR ? 4 : (blockItem ? 15 : 14)), y1 + 3, -1);
	}

	protected void drawBackground(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, Font textRend) {
		/* background */
		drawContext.fill(x1, y1 + 1, x1 + 1, y2 - 1, 0xff6060b0);
		horizontalGradient(drawContext, x1 + 1, y1, x2 - 1, y1 + 1, 0xff6060b0, 0xff8070b0);
		drawContext.fill(x2 - 1, y1 + 1, x2, y2 - 1, 0xff8070b0);
		horizontalGradient(drawContext, x1 + 1, y2 - 1, x2 - 1, y2, 0xff6060b0, 0xff8070b0);

		drawContext.fill(x1 + 1, y1 + 12, x2 - 1, y2 - 1, 0x90606090);

		/* title bar */
		horizontalGradient(drawContext, x1 + 1, y1 + 1, x2 - 1, y1 + 12, (selected ? 0xff6060b0 : 0xff606060), (selected ? 0xff8070b0 : 0xffa0a0a0));

		/* buttons */
		// 26.2 text() skips zero-alpha colors (old TextRenderer forced them opaque), so black is written out
		drawContext.text(textRend, "x", x2 - 10, y1 + 3, 0xff000000, false);
		drawContext.text(textRend, "x", x2 - 11, y1 + 2, -1, false);

		drawContext.text(textRend, "_", x2 - 22, y1 + 2, 0xff000000, false);
		drawContext.text(textRend, "_", x2 - 22, y1 + 1, -1, false);
	}

	public boolean shouldClose(int mouseX, int mouseY) {
		return selected && mouseX > x2 - 23 && mouseX < x2 && mouseY > y1 + 2 && mouseY < y1 + 12;
	}

	public void mouseClicked(double mouseX, double mouseY, int button) {
		// 26.2: vanilla only routes mouseReleased back to us for the left button, so only left-drag.
		if (button == 0 && mouseX >= x1 && mouseX <= x2 - 2 && mouseY >= y1 && mouseY <= y1 + 11) {
			dragging = true;
			dragOffX = (int) mouseX - x1;
			dragOffY = (int) mouseY - y1;
		}

		if (selected) {
			try {
				for (WindowWidget w : widgets) {
					if (w.shouldRender(x1, y1, x2, y2)) {
						w.mouseClicked(x1, y1, (int) mouseX, (int) mouseY, button);
					}
				}
			} catch (ConcurrentModificationException ignored) {}
		}
	}

	public void mouseReleased(double mouseX, double mouseY, int button) {
		dragging = false;

		if (selected) {
			for (WindowWidget w : widgets) {
				if (w.shouldRender(x1, y1, x2, y2)) {
					w.mouseReleased(x1, y1, (int) mouseX, (int) mouseY, button);
				}
			}
		}
	}

	public void tick() {
		for (WindowWidget w : widgets) {
			w.tick();
		}
	}

	public void charTyped(char chr, int modifiers) {
		if (selected) {
			for (WindowWidget w : widgets) {
				w.charTyped(chr, modifiers);
			}
		}
	}

	public void keyPressed(int keyCode, int scanCode, int modifiers) {
		if (selected) {
			for (WindowWidget w : widgets) {
				w.keyPressed(keyCode, scanCode, modifiers);
			}
		}
	}

	public static void fill(GuiGraphicsExtractor drawContext, int x1, int y1, int x2, int y2) {
		fill(drawContext, x1, y1, x2, y2, 0xff6060b0, 0xff8070b0, 0x00000000);
	}

	public static void fill(GuiGraphicsExtractor drawContext, int x1, int y1, int x2, int y2, int fill) {
		fill(drawContext, x1, y1, x2, y2, 0xff6060b0, 0xff8070b0, fill);
	}

	public static void fill(GuiGraphicsExtractor drawContext, int x1, int y1, int x2, int y2, int colTop, int colBot, int colFill) {
		drawContext.fill(x1, y1 + 1, x1 + 1, y2 - 1, colTop);
		drawContext.fill(x1 + 1, y1, x2 - 1, y1 + 1, colTop);
		drawContext.fill(x2 - 1, y1 + 1, x2, y2 - 1, colBot);
		drawContext.fill(x1 + 1, y2 - 1, x2 - 1, y2, colBot);
		drawContext.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, colFill);
	}

	/** color1 = left, color2 = right. Now needs the draw context (26.2 has no global tessellator path). */
	public static void horizontalGradient(GuiGraphicsExtractor drawContext, int x1, int y1, int x2, int y2, int color1, int color2) {
		// fillGradient is vertical-only; rotate the pose 90 degrees so its axis runs horizontally
		drawContext.pose().pushMatrix();
		drawContext.pose().translate(x2, y1);
		drawContext.pose().rotate((float) (Math.PI / 2));
		drawContext.fillGradient(0, 0, y2 - y1, x2 - x1, color2, color1);
		drawContext.pose().popMatrix();
	}

	/** color1 = top, color2 = bottom. */
	public static void verticalGradient(GuiGraphicsExtractor drawContext, int x1, int y1, int x2, int y2, int color1, int color2) {
		drawContext.fillGradient(x1, y1, x2, y2, color1, color2);
	}
}
