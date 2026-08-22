/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.InteractionHand;

import java.util.Comparator;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class InventoryUtils {
	
	private static final Minecraft mc = Minecraft.getInstance();

	/** Returns the slot with the <b>lowest</b> comparator value **/
	public static int getSlot(boolean offhand, boolean reverse, Comparator<Integer> comparator) {
		return IntStream.of(getInventorySlots(offhand))
				.boxed()
				.min(reverse ? comparator.reversed() : comparator).get();
	}

	/** Selects the slot with the <b>lowest</b> comparator value and returns the hand it selected **/
	public static InteractionHand selectSlot(boolean offhand, boolean reverse, Comparator<Integer> comparator) {
		return selectSlot(getSlot(offhand, reverse, comparator));
	}
	
	/** Returns the first slot that matches the Predicate **/
	public static int getSlot(boolean offhand, IntPredicate filter) {
		return IntStream.of(getInventorySlots(offhand))
				.filter(filter)
				.findFirst().orElse(-1);
	}
	
	/** Selects the first slot that matches the Predicate and returns the hand it selected **/
	public static InteractionHand selectSlot(boolean offhand, IntPredicate filter) {
		return selectSlot(getSlot(offhand, filter));
	}
	
	public static InteractionHand selectSlot(int slot) {
		if (slot >= 0 && slot <= 36) {
			if (slot < 9) {
				if (slot != mc.player.getInventory().getSelectedSlot()) {
					mc.player.getInventory().setSelectedSlot(slot);
					mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
				}

				return InteractionHand.MAIN_HAND;
			} else if (mc.player.inventoryMenu == mc.player.containerMenu) {
				for (int i = 0; i <= 8; i++) {
					if (mc.player.getInventory().getItem(i).isEmpty()) {
						mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot, 0, ContainerInput.QUICK_MOVE, mc.player);

						if (i != mc.player.getInventory().getSelectedSlot()) {
							mc.player.getInventory().setSelectedSlot(i);
							mc.player.connection.send(new ServerboundSetCarriedItemPacket(i));
						}

						return InteractionHand.MAIN_HAND;
					}
				}

				mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot, 0, ContainerInput.PICKUP, mc.player);
				mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 36 + mc.player.getInventory().getSelectedSlot(), 0, ContainerInput.PICKUP, mc.player);
				mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot, 0, ContainerInput.PICKUP, mc.player);
				return InteractionHand.MAIN_HAND;
			}
		} else if (slot == 40) {
			return InteractionHand.OFF_HAND;
		}

		return null;
	}
	
	public static int[] getInventorySlots(boolean offhand) {
		int[] i = new int[offhand ? 38 : 37];
		
		// Add hand slots first
		i[0] = mc.player.getInventory().getSelectedSlot();
		i[1] = 40;

		for (int j = 0; j < 36; j++) {
			if (j != mc.player.getInventory().getSelectedSlot()) {
				i[offhand ? j + 2 : j + 1] = j;
			}
		}
		
		return i;
	}
}
