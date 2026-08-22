/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.setting.module;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics; // TODO(26.2): GuiGraphics removed; GUI draw pipeline is now GuiGraphicsExtractor + GuiRenderState (Screen.render -> extractRenderState). Needs window-framework migration.
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;

import net.minecraft.network.chat.Component;

import org.bleachhack.gui.clickgui.window.ModuleWindow;
import org.bleachhack.gui.window.Window;
import org.bleachhack.gui.window.WindowScreen;
import org.bleachhack.gui.window.widget.WindowButtonWidget;
import org.bleachhack.gui.window.widget.WindowScrollbarWidget;
import org.bleachhack.gui.window.widget.WindowTextFieldWidget;
import org.bleachhack.setting.SettingDataHandler;
import org.bleachhack.setting.SettingDataHandlers;
import org.bleachhack.util.io.BleachFileHelper;

import java.util.*;

public abstract class SettingList<T> extends ModuleSetting<LinkedHashSet<T>> {

	protected String windowText;
	protected Set<T> itemPool;

	@SuppressWarnings("unchecked")
	public SettingList(String text, String windowText, SettingDataHandler<T> itemHandler, Collection<T> itemPool, T... defaultItems) {
		super(text, new LinkedHashSet<>(Arrays.asList(defaultItems)), v -> (LinkedHashSet<T>) v.clone(), SettingDataHandlers.ofCollection(itemHandler, LinkedHashSet::new));
		this.windowText = windowText;
		this.itemPool = new LinkedHashSet<>(itemPool);
	}

	// TODO(26.2): render body uses removed immediate-mode GuiGraphics API (fill/drawTextWithShadow). Migrate to GuiGraphicsExtractor/GuiRenderState with the window framework.
	public void render(ModuleWindow window, GuiGraphics drawContext, int x, int y, int len) {
		if (window.mouseOver(x, y, x + len, y + 12)) {
			drawContext.fill(x + 1, y, x + len, y + 12, 0x70303070);
		}

		drawContext.drawTextWithShadow(Minecraft.getInstance().font, getName(), x + 3, y + 2, 0xcfe0cf);
		drawContext.drawTextWithShadow(Minecraft.getInstance().font, "...", x + len - 7, y + 2, 0xcfd0cf);

		if (window.mouseOver(x, y, x + len, y + 12) && window.lmDown) {
			window.mouseReleased(window.mouseX, window.mouseY, 1);
			// TODO(26.2): Screen.mouseReleased(double,double,int) is now mouseReleased(MouseButtonEvent). Rework this click-forwarding hack with the mouse-event framework migration.
			Minecraft.getInstance().gui.screen().mouseReleased(window.mouseX, window.mouseY, 0);
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.3F));
			Minecraft.getInstance().setScreenAndShow(new ListWidowScreen(Minecraft.getInstance().gui.screen()));
		}
	}

	public boolean contains(T item) {
		return getValue().contains(item);
	}

	// TODO(26.2): render body uses removed APIs (GuiGraphics.getMatrices/drawTextWithShadow). Migrate to GuiGraphicsExtractor + Matrix3x2fStack pose() with the window framework.
	public void renderItem(Minecraft mc, GuiGraphics drawContext, T item, int x, int y, int w, int h) {
		drawContext.getMatrices().push();

		float scale = (h - 2) / 10f;
		float offset = 1f / scale;

		drawContext.getMatrices().scale(scale, scale, 1f);

		drawContext.drawTextWithShadow(mc.font, "?", (int) ((x + 5) * offset), (int) ((y + 4) * offset), -1);

		drawContext.getMatrices().pop();
	}

	/**
	 * The human readable name for this item, the internal name is used for read/writing.
	 */
	public abstract Component getName(T item);

	public SettingList<T> withDesc(String desc) {
		setTooltip(desc);
		return this;
	}

	public int getHeight(int len) {
		return 12;
	}

	private class ListWidowScreen extends WindowScreen {

		private Screen parent;
		private WindowTextFieldWidget inputField;
		private WindowScrollbarWidget scrollbar;

		private T toDeleteItem;
		private T toAddItem;

		public ListWidowScreen(Screen parent) {
			super(Component.literal(windowText));
			this.parent = parent;
		}

		public void init() {
			super.init();

			clearWindows();

			addWindow(new Window(
					(int) (width / 3.25),
					height / 12,
					(int) (width - width / 3.25),
					height - height / 12,
					windowText, new ItemStack(Items.OAK_SIGN)));

			int x2 = getWindow(0).x2 - getWindow(0).x1;
			int y2 = getWindow(0).y2 - getWindow(0).y1;

			getWindow(0).addWidget(new WindowButtonWidget(x2 - 50, y2 - 22, x2 - 5, y2 - 5, "Reset", () -> {
				getValue().clear();
				getValue().addAll(defaultValue);
				BleachFileHelper.SCHEDULE_SAVE_MODULES.set(true);
			}));

			getWindow(0).addWidget(new WindowButtonWidget(x2 - 100, y2 - 22, x2 - 55, y2 - 5, "Clear", () -> {
				getValue().clear();
				BleachFileHelper.SCHEDULE_SAVE_MODULES.set(true);
			}));

			getWindow(0).addWidget(new WindowButtonWidget(x2 - 150, y2 - 22, x2 - 105, y2 - 5, "Add All", () -> {
				getValue().clear();
				getValue().addAll(itemPool);
				BleachFileHelper.SCHEDULE_SAVE_MODULES.set(true);
			}));

			inputField = getWindow(0).addWidget(new WindowTextFieldWidget(5, y2 - 22, x2 / 3, 17, inputField != null ? inputField.textField.getValue() : ""));

			scrollbar = getWindow(0).addWidget(new WindowScrollbarWidget(x2 - 11, 12, 0, y2 - 39, scrollbar == null ? 0 : scrollbar.getPageOffset()));
		}

		// TODO(26.2): Screen.render(GuiGraphics,...) removed; replaced by extractRenderState(GuiGraphicsExtractor,...). Migrate override with the window framework (WindowScreen).
		public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
			renderBackground(drawContext, mouseX, mouseY, delta);
			super.render(drawContext, mouseX, mouseY, delta);
		}

		// TODO(26.2): GuiGraphics param + body (RenderSystem model-view stack, GuiGraphics.getMatrices) removed. Migrate with the window framework.
		public void onRenderWindow(GuiGraphics drawContext, int window, int mouseX, int mouseY) {
			super.onRenderWindow(drawContext, window, mouseX, mouseY);

			toAddItem = null;
			toDeleteItem = null;

			if (window == 0) {
				int x1 = getWindow(0).x1;
				int y1 = getWindow(0).y1;
				int x2 = getWindow(0).x2;
				int y2 = getWindow(0).y2;

				int maxEntries = Math.max(1, (y2 - y1) / 21 - 1);
				int renderEntries = 0;
				int entries = 0;

				scrollbar.setTotalHeight(getValue().size() * 21);
				int offset = scrollbar.getPageOffset();

				for (T e: getValue()) {
					if (entries >= offset / 21 && renderEntries < maxEntries) {
						drawEntry(drawContext, e, x1 + 6, y1 + 15 + entries * 21 - offset, x2 - x1 - 19, 20, mouseX, mouseY);
						renderEntries++;
					}

					entries++;
				}

				//Window.horizontalGradient(matrix, x1 + 1, y2 - 25, x2 - 1, y2 - 1, 0x70606090, 0x00606090);
				Window.horizontalGradient(x1 + 1, y2 - 27, x2 - 1, y2 - 26, 0xff606090, 0x50606090);

				if (inputField.textField.isFocused()) {
					Set<T> toDraw = new LinkedHashSet<>();

					for (T e: itemPool) {
						if (toDraw.size() >= 10)
							break;

						if (!getValue().contains(e) && getName(e).getString().toLowerCase(Locale.ENGLISH).contains(inputField.textField.getValue().toLowerCase(Locale.ENGLISH))) {
							toDraw.add(e);
						}
					}

					int curY = y1 + inputField.y1 - 4 - toDraw.size() * 17;
					int longest = toDraw.stream().mapToInt(e -> textRenderer.getWidth(getName(e))).max().orElse(0);

					RenderSystem.getModelViewStack().push();
					RenderSystem.getModelViewStack().translate(0, 0, 150);

					drawContext.getMatrices().push();
					drawContext.getMatrices().translate(0, 0, 150);

					for (T e: toDraw) {
						drawSearchEntry(drawContext, e, x1 + inputField.x1, curY, longest + 23, 16, mouseX, mouseY);
						curY += 17;
					}

					drawContext.getMatrices().pop();
					RenderSystem.getModelViewStack().pop();
					RenderSystem.applyModelViewMatrix();
				}
			}
		}

		private void drawEntry(GuiGraphics drawContext, T item, int x, int y, int width, int height, int mouseX, int mouseY) {
			boolean mouseOverDelete = mouseX >= x + width - 14 && mouseX <= x + width - 1 && mouseY >= y + 2 && mouseY <= y + height - 2;
			Window.fill(drawContext, x + width - 14, y + 2, x + width - 1, y + height - 2, mouseOverDelete ? 0x4fb070f0 : 0x60606090);

			if (mouseOverDelete) {
				toDeleteItem = item;
			}

			renderItem(minecraft, drawContext, item, x, y, height, height);

			drawContext.drawTextWithShadow(textRenderer, getName(item), x + height + 4, y + 4, -1);
			drawContext.drawTextWithShadow(textRenderer, "§cx", x + width - 10, y + 5, -1);
		}

		private void drawSearchEntry(GuiGraphics drawContext, T item, int x, int y, int width, int height, int mouseX, int mouseY) {
			boolean mouseOver = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
			drawContext.fill(x, y - 1, x + width, y + height, mouseOver ? 0xdf8070d0 : 0xb0606090);

			if (mouseOver) {
				toAddItem = item;
			}

			renderItem(minecraft, drawContext, item, x, y, height, height);
			drawContext.drawTextWithShadow(textRenderer, getName(item), x + height + 4, y + 4, -1);
		}

		@Override
		public void onClose() {
			this.minecraft.setScreenAndShow(parent);
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (toAddItem != null) {
				getValue().add(toAddItem);
				inputField.textField.setFocused(true);
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.3F));
				BleachFileHelper.SCHEDULE_SAVE_MODULES.set(true);
				return false;
			} else if (toDeleteItem != null) {
				getValue().remove(toDeleteItem);
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.3F));
				BleachFileHelper.SCHEDULE_SAVE_MODULES.set(true);
			}

			return super.mouseClicked(mouseX, mouseY, button);
		}

		public boolean mouseScrolled(double mouseX, double mouseY, double amountH, double amountV) {
			if (!inputField.textField.isFocused() || inputField.textField.getValue().isEmpty()) {
				scrollbar.scroll(amountV);
			}

			return super.mouseScrolled(mouseX, mouseY, amountH, amountV);
		}
	}
}
