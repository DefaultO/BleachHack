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

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 26.2 binds item data-components late; constructing an ItemStack before then throws
 * "Components not bound yet". These builders return {@link ItemStack#EMPTY} until the
 * registry is ready, so callers can retry next frame instead of crashing.
 * ponytail: catches the NPE rather than probing bind state — Holder.isBound() doesn't
 * track the separate component binding, so the exception is the only reliable signal.
 */
public class SafeItem {

	public static ItemStack of(Item item) {
		try {
			return new ItemStack(item);
		} catch (NullPointerException e) {
			return ItemStack.EMPTY;
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
