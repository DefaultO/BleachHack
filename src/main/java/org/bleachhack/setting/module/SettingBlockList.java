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

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.bleachhack.setting.SettingDataHandlers;

import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

public class SettingBlockList extends SettingList<Block> {

	public SettingBlockList(String text, String windowText, Block... defaultBlocks) {
		this(text, windowText, null, defaultBlocks);
	}

	public SettingBlockList(String text, String windowText, Predicate<Block> filter, Block... defaultBlocks) {
		super(text, windowText, SettingDataHandlers.BLOCK, getAllBlocks(filter), defaultBlocks);
	}

	private static Collection<Block> getAllBlocks(Predicate<Block> filter) {
		return filter == null
				? BuiltInRegistries.BLOCK.stream().collect(Collectors.toList())
						: BuiltInRegistries.BLOCK.stream().filter(filter).collect(Collectors.toList());
	}

	@Override
	public void renderItem(Minecraft mc, GuiGraphicsExtractor drawContext, Block item, int x, int y, int w, int h) {
		if (item == null || item.asItem() == Items.AIR) {
			super.renderItem(mc, drawContext, item, x, y, w, h);
		} else {
			drawContext.pose().pushMatrix();

			float scale = (h - 2) / 16f;
			float offset = 1f / scale;

			drawContext.pose().scale(scale, scale);

			drawContext.item(new ItemStack(item.asItem()), (int) ((x + 1) * offset), (int) ((y + 1) * offset));

			drawContext.pose().popMatrix();
		}
	}

	@Override
	public Component getName(Block item) {
		return item.getName();
	}
}
