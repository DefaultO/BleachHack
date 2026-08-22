/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ContainerInput;

public class ElytraReplace extends Module {

	private boolean jump = false;

	public ElytraReplace() {
		super("ElytraReplace", KEY_UNBOUND, ModuleCategory.PLAYER, "Automatically replaces your elytra when its broken and continues flying.");
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (mc.player.inventoryMenu != mc.player.containerMenu)
			return;

		int chestSlot = 38;
		ItemStack chest = mc.player.getInventory().getItem(chestSlot);
		if (chest.getItem() == Items.ELYTRA && chest.getDamageValue() == (chest.getMaxDamage() - 1)) {
			// search inventory for elytra

			Integer elytraSlot = null;
			for (int slot = 0; slot < 36; slot++) {
				ItemStack stack = mc.player.getInventory().getItem(slot);
				if (stack.getItem() == Items.ELYTRA && stack.getDamageValue() != (stack.getMaxDamage() - 1)) {
					elytraSlot = slot;
					break;
				}
			}

			if (elytraSlot == null) {
				return;
			}

			mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 6, 0, ContainerInput.PICKUP, mc.player);
			mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, elytraSlot < 9 ? (elytraSlot + 36) : (elytraSlot), 0, ContainerInput.PICKUP,
					mc.player);
			mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 6, 0, ContainerInput.PICKUP, mc.player);

			mc.options.keyJump.setDown(true); // Make them fly again
			jump = true;
		} else if (jump) {
			mc.options.keyJump.setDown(false); // Make them fly again
			jump = false;
		}
	}
}
