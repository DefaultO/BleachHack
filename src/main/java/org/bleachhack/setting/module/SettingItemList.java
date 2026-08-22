/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.setting.module;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.client.gui.GuiGraphics; // TODO(26.2): GuiGraphics removed; GUI draw pipeline is now GuiGraphicsExtractor + GuiRenderState. Needs window-framework migration.
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.bleachhack.setting.SettingDataHandlers;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

public class SettingItemList extends SettingList<Item> {

	public SettingItemList(String text, String windowText, Item... defaultItems) {
		this(text, windowText, null, defaultItems);
	}

	public SettingItemList(String text, String windowText, Predicate<Item> filter, Item... defaultItems) {
		super(text, windowText, SettingDataHandlers.ITEM, getAllItems(filter), defaultItems);
	}

	private static Collection<Item> getAllItems(Predicate<Item> filter) {
		return filter == null
				? BuiltInRegistries.ITEM.stream().collect(Collectors.toList())
						: BuiltInRegistries.ITEM.stream().filter(filter).collect(Collectors.toList());
	}

	@Override
	// TODO(26.2): render body uses removed APIs (GuiGraphics.drawItem, RenderSystem.getModelViewStack/applyModelViewMatrix). Migrate to GuiGraphicsExtractor + Matrix3x2fStack pose() with the window framework.
	public void renderItem(Minecraft mc, GuiGraphics drawContext, Item item, int x, int y, int w, int h) {
		if (item == null || item == Items.AIR) {
			super.renderItem(mc, drawContext, item, x, y, w, h);
		} else {
			RenderSystem.getModelViewStack().push();

			float scale = (h - 2) / 16f;
			float offset = 1f / scale;

			RenderSystem.getModelViewStack().scale(scale, scale, 1f);

			drawContext.drawItem(new ItemStack(item), (int) ((x + 1) * offset), (int) ((y + 1) * offset));

			RenderSystem.getModelViewStack().pop();
			RenderSystem.applyModelViewMatrix();
		}
	}

	@Override
	public Component getName(Item item) {
		return item.getName(item.getDefaultInstance());
	}
}
