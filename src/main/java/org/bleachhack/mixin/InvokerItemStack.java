/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 26.2: item data-components only bind on world/server load (ReloadableServerResources),
 * so the public ItemStack constructors throw pre-world. This invokes the private
 * (Holder, count, components) constructor so gui icons can exist at the title screen.
 */
@Mixin(ItemStack.class)
public interface InvokerItemStack {

	@Invoker("<init>")
	static ItemStack bleachhack$create(Holder<Item> item, int count, PatchedDataComponentMap components) {
		throw new AssertionError();
	}
}
