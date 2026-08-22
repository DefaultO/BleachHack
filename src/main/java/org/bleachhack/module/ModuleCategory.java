/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum ModuleCategory {
	PLAYER(Items.ARMOR_STAND),
	RENDER(Items.STAINED_GLASS.yellow()),
	COMBAT(Items.TOTEM_OF_UNDYING),
	MOVEMENT(Items.POTION),
	EXPLOITS(Items.REPEATING_COMMAND_BLOCK),
	MISC(Items.NAUTILUS_SHELL),
	WORLD(Items.GRASS_BLOCK);

	private final Item item;
	// ponytail: 26.2 forbids building an ItemStack before the data-component registry is bound
	// (this enum loads early), so defer construction to first use.
	private ItemStack stack;

	ModuleCategory(Item item) {
		this.item = item;
	}

	public ItemStack getItem() {
		if (stack == null || stack.isEmpty()) {
			stack = org.bleachhack.util.SafeItem.of(item);
		}
		return stack;
	}
}
