/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.bleachhack.event.events.EventRenderTooltip;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.ItemContentUtils;

import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class Peek extends Module {

	private static final Identifier MAP_BACKGROUND_CHECKERBOARD = Identifier.withDefaultNamespace("textures/map/map_background_checkerboard.png");

	private final MapRenderState mapRenderState = new MapRenderState();

	private List<List<String>> pages;
	private int slotX = -1;
	private int slotY = -1;
	private int pageCount = 0;
	private boolean shown = false;

	public Peek() {
		super("Peek", KEY_UNBOUND, ModuleCategory.MISC, "Shows whats inside containers.",
				new SettingToggle("Containers", true).withDesc("Shows a tooltip for containers.").withChildren(
						new SettingMode("Info", "All", "Name", "None").withDesc("How to show the old tooltip.")),
				new SettingToggle("Books", true).withDesc("Show tooltips for books."),
				new SettingToggle("Maps", true).withDesc("Show tooltips for maps.").withChildren(
						new SettingSlider("Map Size", 0.25, 1.5, 0.85, 2).withDesc("How big to make the map.")));
	}

	@BleachSubscribe
	public void drawScreen(EventRenderTooltip event) {
		if (!(event.getScreen() instanceof AbstractContainerScreen)) {
			return;
		}

		Slot slot = ((AbstractContainerScreen<?>) event.getScreen()).hoveredSlot;
		if (slot == null)
			return;

		if (slot.x != slotX || slot.y != slotY) {
			pageCount = 0;
			pages = null;

			slotX = slot.x;
			slotY = slot.y;
		}

		// TODO(26.2): was matrices.translate(0, 0, 400); strata replace z-translation and can't be popped
		event.drawContext().nextStratum();

		if (getSetting(0).asToggle().getState()) {
			List<ClientTooltipComponent> components = drawShulkerToolTip(event.drawContext(), slot, event.getMouseX(), event.getMouseY());
			if (components != null) {
				if (components.isEmpty()) {
					event.setCancelled(true);
				} else {
					event.setComponents(components);
				}
			}
		}

		if (getSetting(1).asToggle().getState()) drawBookToolTip(event.drawContext(), slot, event.getMouseX(), event.getMouseY());
		if (getSetting(2).asToggle().getState()) drawMapToolTip(event.drawContext(), slot, event.getMouseX(), event.getMouseY());
	}

	public List<ClientTooltipComponent> drawShulkerToolTip(GuiGraphicsExtractor context, Slot slot, int mouseX, int mouseY) {
		if (!(slot.getItem().getItem() instanceof BlockItem)) {
			return null;
		}

		Block block = ((BlockItem) slot.getItem().getItem()).getBlock();

		if (!(block instanceof ShulkerBoxBlock)
				&& !(block instanceof ChestBlock)
				&& !(block instanceof BarrelBlock)
				&& !(block instanceof DispenserBlock)
				&& !(block instanceof HopperBlock)
				&& !(block instanceof AbstractFurnaceBlock)) {
			return null;
		}

		List<ItemStack> items = ItemContentUtils.getItemsInContainer(slot.getItem());

		if (items.stream().allMatch(ItemStack::isEmpty)) {
			return null;
		}

		int mode = getSetting(0).asToggle().getChild(0).asMode().getMode();
		int realY = mode == 2 ? mouseY + 24 : mouseY;
		int tooltipWidth = block instanceof AbstractFurnaceBlock ? 47 : block instanceof HopperBlock ? 82 : 150;
		int tooltipHeight = block instanceof AbstractFurnaceBlock || block instanceof HopperBlock || block instanceof DispenserBlock ? 13 : 47;

		renderTooltipBox(context, mouseX, realY - tooltipHeight - 7, tooltipWidth, tooltipHeight, true);

		int count = block instanceof HopperBlock || block instanceof DispenserBlock || block instanceof AbstractFurnaceBlock ? 18 : 0;

		for (ItemStack i : items) {
			if (count > 26) {
				break;
			}

			int x = mouseX + 17 * (count % 9);
			int y = realY - 67 + 17 * (count / 9);

			context.item(i, x, y);
			context.itemDecorations(mc.font, i, x, y);
			count++;
		}

		if (mode == 1) {
			return Arrays.asList(ClientTooltipComponent.create(slot.getItem().getHoverName().getVisualOrderText()));
		} else if (mode == 2) {
			return List.of();
		}

		return null;
	}

	public void drawBookToolTip(GuiGraphicsExtractor drawContext, Slot slot, int mouseX, int mouseY) {
		if (slot.getItem().getItem() != Items.WRITABLE_BOOK && slot.getItem().getItem() != Items.WRITTEN_BOOK)
			return;

		if (pages == null) {
			pages = ItemContentUtils.getTextInBook(slot.getItem());
		}

		if (pages.isEmpty()) {
			return;
		}

		/* Cycle through pages */
		if (mc.player.tickCount % 80 == 0 && !shown) {
			shown = true;
			if (pageCount == pages.size() - 1) {
				pageCount = 0;
			} else {
				pageCount++;
			}
		} else if (mc.player.tickCount % 80 != 0) {
			shown = false;
		}

		drawContext.blit(RenderPipelines.GUI_TEXTURED, BookViewScreen.BOOK_LOCATION, mouseX, mouseY - 143,
				0f, 0f,
				134, 134,
				179, 179);

		Component pageIndexText = Component.translatable("book.pageIndicator", pageCount + 1, pages.size());
		int pageIndexLength = mc.font.width(pageIndexText);

		drawContext.pose().pushMatrix();
		drawContext.pose().scale(0.7f, 0.7f);

		// TODO(26.2): page text rendering was already commented out pre-migration
		//drawContext.text(mc.font, pageIndexText, (int) ((mouseX + 123 - pageIndexLength) * 1.43f), (int) ((mouseY - 133) * 1.43f), 0x000000, false);

		/*int count = 0;
		for (String s : pages.get(pageCount)) {
			drawContext.text(mc.font, s, (int) ((mouseX + 24) * 1.43f), (int) ((mouseY - 123 + count * 7) * 1.43f), 0x000000, false);

			count++;
		}*/

		drawContext.pose().popMatrix();

	}

	public void drawMapToolTip(GuiGraphicsExtractor drawContext, Slot slot, int mouseX, int mouseY) {
		if (slot.getItem().getItem() != Items.FILLED_MAP) {
			return;
		}

		MapId id = slot.getItem().get(DataComponents.MAP_ID);
		MapItemSavedData mapState = MapItem.getSavedData(id, mc.level);

		if (mapState == null) {
			return;
		}

		float scale = getSetting(2).asToggle().getChild(0).asSlider().getValueFloat() / 1.25f;

		drawContext.pose().pushMatrix();
		drawContext.pose().translate(mouseX + 14, mouseY - 18 - 135 * scale);
		drawContext.pose().scale(scale, scale);

		drawContext.blit(MAP_BACKGROUND_CHECKERBOARD, -7, -7, 135, 135, 0f, 1f, 0f, 1f);

		mc.getMapRenderer().extractRenderState(id, mapState, mapRenderState);
		drawContext.map(mapRenderState);

		drawContext.pose().popMatrix();

	}

	private void renderTooltipBox(GuiGraphicsExtractor drawContext, int x1, int y1, int x2, int y2, boolean wrap) {
		int xStart = x1 + 12;
		int yStart = y1 - 12;
		if (wrap) {
			if (xStart + x2 > drawContext.guiWidth())
				xStart -= 28 + x2;
			if (yStart + y2 + 6 > drawContext.guiHeight())
				yStart = drawContext.guiHeight() - y2 - 6;
		}

		drawContext.fillGradient(xStart - 3, yStart - 4, xStart + x2 + 3, yStart - 3, -267386864, -267386864);
		drawContext.fillGradient(xStart - 3, yStart + y2 + 3, xStart + x2 + 3, yStart + y2 + 4, -267386864, -267386864);
		drawContext.fillGradient(xStart - 3, yStart - 3, xStart + x2 + 3, yStart + y2 + 3, -267386864, -267386864);
		drawContext.fillGradient(xStart - 4, yStart - 3, xStart - 3, yStart + y2 + 3, -267386864, -267386864);
		drawContext.fillGradient(xStart + x2 + 3, yStart - 3, xStart + x2 + 4, yStart + y2 + 3, -267386864, -267386864);
		drawContext.fillGradient(xStart - 3, yStart - 3 + 1, xStart - 3 + 1, yStart + y2 + 3 - 1, 1347420415, 1344798847);
		drawContext.fillGradient(xStart + x2 + 2, yStart - 3 + 1, xStart + x2 + 3, yStart + y2 + 3 - 1, 1347420415, 1344798847);
		drawContext.fillGradient(xStart - 3, yStart - 3, xStart + x2 + 3, yStart - 3 + 1, 1347420415, 1347420415);
		drawContext.fillGradient(xStart - 3, yStart + y2 + 2, xStart + x2 + 3, yStart + y2 + 3, 1344798847, 1344798847);
	}
}
