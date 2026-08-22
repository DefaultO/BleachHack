/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bleachhack.mixin.InvokerItemStack;

/**
 * 26.2 binds item data-components only on world/server load, so the public ItemStack
 * constructors throw "Components not bound yet" before then (e.g. at the title screen).
 * {@link #of(Item)} falls back to the private constructor with a minimal component map
 * carrying just {@code minecraft:item_model} (default = the item's own id), which is all
 * the gui item renderer needs to draw the icon.
 */
public class SafeItem {

	public static ItemStack of(Item item) {
		try {
			return new ItemStack(item);
		} catch (NullPointerException e) {
			Identifier id = item.builtInRegistryHolder().key().identifier();
			DataComponentMap base = DataComponentMap.builder().set(DataComponents.ITEM_MODEL, id).build();
			return InvokerItemStack.bleachhack$create(item.builtInRegistryHolder(), 1, new PatchedDataComponentMap(base));
		}
	}

	public static ItemStack resolve(Supplier<ItemStack> supplier) {
		if (supplier == null) {
			return ItemStack.EMPTY;
		}
		try {
			ItemStack stack = supplier.get();
			return stack == null ? ItemStack.EMPTY : stack;
		} catch (NullPointerException e) {
			return ItemStack.EMPTY;
		}
	}
}
