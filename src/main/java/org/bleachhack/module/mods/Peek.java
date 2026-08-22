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

import net.minecraft.client.gui.GuiGraphics;
import org.bleachhack.event.events.EventRenderTooltip;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.ItemContentUtils;

import com.mojang.blaze3d.systems.RenderSystem;

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
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.render.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public class Peek extends Module {

	private static final RenderType MAP_BACKGROUND_CHECKERBOARD = RenderType.getText(new Identifier("textures/map/map_background_checkerboard.png"));

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

		Slot slot = ((AbstractContainerScreen<?>) event.getScreen()).focusedSlot;
		if (slot == null)
			return;

		if (slot.x != slotX || slot.y != slotY) {
			pageCount = 0;
			pages = null;

			slotX = slot.x;
			slotY = slot.y;
		}

		event.drawContext().getMatrices().push();
		event.drawContext().getMatrices().translate(0, 0, 400);

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

		event.drawContext().getMatrices().pop();
	}

	public List<ClientTooltipComponent> drawShulkerToolTip(GuiGraphics context, Slot slot, int mouseX, int mouseY) {
		if (!(slot.getStack().getItem() instanceof BlockItem)) {
			return null;
		}

		Block block = ((BlockItem) slot.getStack().getItem()).getBlock();

		if (!(block instanceof ShulkerBoxBlock)
				&& !(block instanceof ChestBlock)
				&& !(block instanceof BarrelBlock)
				&& !(block instanceof DispenserBlock)
				&& !(block instanceof HopperBlock)
				&& !(block instanceof AbstractFurnaceBlock)) {
			return null;
		}

		List<ItemStack> items = ItemContentUtils.getItemsInContainer(slot.getStack());

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

			// mc.getItemRenderer().zOffset = 400;
			context.drawItem(i, x, y);
			context.drawItemTooltip(mc.textRenderer, i, x, y);
			// mc.getItemRenderer().zOffset = 300;
			count++;
		}

		if (mode == 1) {
			return Arrays.asList(ClientTooltipComponent.of(slot.getStack().getName().asOrderedText()));
		} else if (mode == 2) {
			return List.of();
		}

		return null;
	}

	public void drawBookToolTip(GuiGraphics drawContext, Slot slot, int mouseX, int mouseY) {
		if (slot.getStack().getItem() != Items.WRITABLE_BOOK && slot.getStack().getItem() != Items.WRITTEN_BOOK)
			return;

		if (pages == null) {
			pages = ItemContentUtils.getTextInBook(slot.getStack());
		}

		if (pages.isEmpty()) {
			return;
		}

		/* Cycle through pages */
		if (mc.player.age % 80 == 0 && !shown) {
			shown = true;
			if (pageCount == pages.size() - 1) {
				pageCount = 0;
			} else {
				pageCount++;
			}
		} else if (mc.player.age % 80 != 0) {
			shown = false;
		}

		RenderSystem.setShader(GameRenderer::getPositionTexProgram);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, BookViewScreen.BOOK_TEXTURE);
		drawContext.drawTexture(BookViewScreen.BOOK_TEXTURE, mouseX, mouseY - 143, 0,
				0, 0,
				134, 134,
				179, 179);

		Component pageIndexText = Component.translatable("book.pageIndicator", pageCount + 1, pages.size());
		int pageIndexLength = mc.textRenderer.getWidth(pageIndexText);

		drawContext.getMatrices().push();
		drawContext.getMatrices().scale(0.7f, 0.7f, 1f);

		//MultiBufferSource.Immediate immediate = MultiBufferSource.immediate(Tesselator.getInstance().getBuffer());
		//mc.textRenderer.draw(pageIndexText.toString(), (mouseX + 123 - pageIndexLength) * 1.43f, (mouseY - 133) * 1.43f, 0x000000, false, drawContext.getMatrices(), immediate, TextRenderer.TextLayerType.NORMAL, 0, 0);


		/*int count = 0;
		for (String s : pages.get(pageCount)) {
			mc.textRenderer.draw(
					matrices,
					s,
					(mouseX + 24) * 1.43f,
					(mouseY - 123 + count * 7) * 1.43f,
					0x000000);

			count++;
		}*/

		drawContext.getMatrices().pop();

	}

	public void drawMapToolTip(GuiGraphics drawContext, Slot slot, int mouseX, int mouseY) {
		if (slot.getStack().getItem() != Items.FILLED_MAP) {
			return;
		}

		Integer id = MapItem.getMapId(slot.getStack());
		MapItemSavedData mapState = MapItem.getMapState(id, mc.world);

		if (mapState == null) {
			return;
		}

		float scale = getSetting(2).asToggle().getChild(0).asSlider().getValueFloat() / 1.25f;

		drawContext.getMatrices().push();
		drawContext.getMatrices().translate(mouseX + 14, mouseY - 18 - 135 * scale, 0);
		drawContext.getMatrices().scale(scale, scale, 0.0078125f);

		MultiBufferSource.Immediate immediate = MultiBufferSource.immediate(Tesselator.getInstance().getBuffer());
		VertexConsumer backgroundVertexer = immediate.getBuffer(MAP_BACKGROUND_CHECKERBOARD);
		Matrix4f matrix4f = drawContext.getMatrices().peek().getPositionMatrix();
		backgroundVertexer.vertex(matrix4f, -7f, 135f, -10f).color(255, 255, 255, 255).texture(0f, 1f).light(0xf000f0).next();
		backgroundVertexer.vertex(matrix4f, 135f, 135f, -10f).color(255, 255, 255, 255).texture(1f, 1f).light(0xf000f0).next();
		backgroundVertexer.vertex(matrix4f, 135f, -7f, -10f).color(255, 255, 255, 255).texture(1f, 0f).light(0xf000f0).next();
		backgroundVertexer.vertex(matrix4f, -7f, -7f, -10f).color(255, 255, 255, 255).texture(0f, 0f).light(0xf000f0).next();

		mc.gameRenderer.getMapRenderer().draw(drawContext.getMatrices(), immediate, id, mapState, false, 0xf000f0);
		immediate.draw();

		drawContext.getMatrices().pop();

	}

	private void renderTooltipBox(GuiGraphics drawContext, int x1, int y1, int x2, int y2, boolean wrap) {
		int xStart = x1 + 12;
		int yStart = y1 - 12;
		if (wrap) {
			if (xStart + x2 > mc.currentScreen.width)
				xStart -= 28 + x2;
			if (yStart + y2 + 6 > mc.currentScreen.height)
				yStart = mc.currentScreen.height - y2 - 6;
		}

		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		Matrix4f matrix4f = drawContext.getMatrices().peek().getPositionMatrix();
		fillGradient(matrix4f, bufferBuilder, xStart - 3, yStart - 4, xStart + x2 + 3, yStart - 3, -267386864, -267386864);
		fillGradient(matrix4f, bufferBuilder, xStart - 3, yStart + y2 + 3, xStart + x2 + 3, yStart + y2 + 4, -267386864, -267386864);
		fillGradient(matrix4f, bufferBuilder, xStart - 3, yStart - 3, xStart + x2 + 3, yStart + y2 + 3, -267386864, -267386864);
		fillGradient(matrix4f, bufferBuilder, xStart - 4, yStart - 3, xStart - 3, yStart + y2 + 3, -267386864, -267386864);
		fillGradient(matrix4f, bufferBuilder, xStart + x2 + 3, yStart - 3, xStart + x2 + 4, yStart + y2 + 3, -267386864, -267386864);
		fillGradient(matrix4f, bufferBuilder, xStart - 3, yStart - 3 + 1, xStart - 3 + 1, yStart + y2 + 3 - 1, 1347420415, 1344798847);
		fillGradient(matrix4f, bufferBuilder, xStart + x2 + 2, yStart - 3 + 1, xStart + x2 + 3, yStart + y2 + 3 - 1, 1347420415, 1344798847);
		fillGradient(matrix4f, bufferBuilder, xStart - 3, yStart - 3, xStart + x2 + 3, yStart - 3 + 1, 1347420415, 1347420415);
		fillGradient(matrix4f, bufferBuilder, xStart - 3, yStart + y2 + 2, xStart + x2 + 3, yStart + y2 + 3, 1344798847, 1344798847);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
		RenderSystem.disableBlend();
	}

	private void fillGradient(Matrix4f matrices, BufferBuilder bufferBuilder, int xStart, int yStart, int xEnd, int yEnd, int colorStart, int colorEnd) {
		float f = (float)(colorStart >> 24 & 255) / 255.0F;
		float g = (float)(colorStart >> 16 & 255) / 255.0F;
		float h = (float)(colorStart >> 8 & 255) / 255.0F;
		float i = (float)(colorStart & 255) / 255.0F;
		float j = (float)(colorEnd >> 24 & 255) / 255.0F;
		float k = (float)(colorEnd >> 16 & 255) / 255.0F;
		float l = (float)(colorEnd >> 8 & 255) / 255.0F;
		float m = (float)(colorEnd & 255) / 255.0F;
		bufferBuilder.vertex(matrices, (float) xEnd, (float) yStart, 0f).color(g, h, i, f).next();
		bufferBuilder.vertex(matrices, (float) xStart, (float) yStart, 0f).color(g, h, i, f).next();
		bufferBuilder.vertex(matrices, (float) xStart, (float) yEnd, 0f).color(k, l, m, j).next();
		bufferBuilder.vertex(matrices, (float) xEnd, (float) yEnd, 0f).color(k, l, m, j).next();
	}
}
