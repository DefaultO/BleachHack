/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventPacket;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingToggle;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;

public class AutoTool extends Module {

	private int lastSlot = -1;
	private int queueSlot = -1;

	public AutoTool() {
		super("AutoTool", KEY_UNBOUND, ModuleCategory.PLAYER, "Automatically uses best tool when breaking blocks.",
				new SettingToggle("AntiBreak", false).withDesc("Doesn't use the tool if its about to break."),
				new SettingToggle("SwitchBack", true).withDesc("Switches back to your previous item when done breaking."),
				new SettingToggle("DurabilitySave", true).withDesc("Swiches to a non-damageable item when possible."));
	}

	@BleachSubscribe
	public void onPacketSend(EventPacket.Send event) {
		if (event.getPacket() instanceof ServerboundPlayerActionPacket) {
			ServerboundPlayerActionPacket p = (ServerboundPlayerActionPacket) event.getPacket();

			if (p.getAction() == Action.START_DESTROY_BLOCK) {
				if (mc.player.isCreative() || mc.player.isSpectator())
					return;

				queueSlot = -1;

				lastSlot = mc.player.getInventory().getSelectedSlot();

				int slot = getBestSlot(p.getPos());

				if (slot != mc.player.getInventory().getSelectedSlot()) {
					if (slot < 9) {
						mc.player.getInventory().setSelectedSlot(slot);
						mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
					} else if (mc.player.inventoryMenu == mc.player.containerMenu) {
						boolean itemInHand = !mc.player.getInventory().getSelectedItem().isEmpty();
						mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot, 0, ContainerInput.PICKUP, mc.player);
						mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 36 + mc.player.getInventory().getSelectedSlot(), 0, ContainerInput.PICKUP, mc.player);

						if (itemInHand)
							mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot, 0, ContainerInput.PICKUP, mc.player);
					}
				}
			} else if (p.getAction() == Action.STOP_DESTROY_BLOCK) {
				if (getSetting(1).asToggle().getState()) {
					ItemStack handSlot = mc.player.getMainHandItem();
					if (getSetting(0).asToggle().getState() && handSlot.isDamageableItem() && handSlot.getMaxDamage() - handSlot.getDamageValue() < 2
							&& queueSlot == mc.player.getInventory().getSelectedSlot()) {
						queueSlot = mc.player.getInventory().getSelectedSlot() == 0 ? 1 : mc.player.getInventory().getSelectedSlot() - 1;
					} else if (lastSlot >= 0 && lastSlot <= 8 && lastSlot != mc.player.getInventory().getSelectedSlot()) {
						queueSlot = lastSlot;
					}
				}
			}
		}
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (queueSlot != -1) {
			mc.player.getInventory().setSelectedSlot(queueSlot);
			mc.player.connection.send(new ServerboundSetCarriedItemPacket(queueSlot));
			queueSlot = -1;
		}
	}

	private int getBestSlot(BlockPos pos) {
		BlockState state = mc.level.getBlockState(pos);

		int bestSlot = mc.player.getInventory().getSelectedSlot();

		ItemStack handSlot = mc.player.getInventory().getItem(bestSlot);
		if (getSetting(0).asToggle().getState() && handSlot.isDamageableItem() && handSlot.getMaxDamage() - handSlot.getDamageValue() < 2) {
			bestSlot = bestSlot == 0 ? 1 : bestSlot - 1;
		}

		if (state.isAir())
			return mc.player.getInventory().getSelectedSlot();

		float bestSpeed = getMiningSpeed(mc.player.getInventory().getItem(bestSlot), state);

		for (int slot = 0; slot < 36; slot++) {
			if (slot == mc.player.getInventory().getSelectedSlot() || slot == bestSlot)
				continue;

			ItemStack stack = mc.player.getInventory().getItem(slot);
			if (getSetting(0).asToggle().getState() && stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() < 2) {
				continue;
			}

			float speed = getMiningSpeed(stack, state);
			if (speed > bestSpeed
					|| (getSetting(2).asToggle().getState()
							&& speed == bestSpeed && !stack.isDamageableItem()
							&& mc.player.getInventory().getItem(bestSlot).isDamageableItem()
							&& getEnchantmentLevel(Enchantments.SILK_TOUCH, mc.player.getInventory().getItem(bestSlot)) == 0)) {
				bestSpeed = speed;
				bestSlot = slot;
			}
		}

		return bestSlot;
	}

	private float getMiningSpeed(ItemStack stack, BlockState state) {
		if ((state.getBlock() == Blocks.BAMBOO || state.getBlock() == Blocks.BAMBOO_SAPLING) && stack.is(ItemTags.SWORDS)) {
			return Integer.MAX_VALUE;
		}

		float speed = stack.getDestroySpeed(state);

		if (speed > 1) {
			int efficiency = getEnchantmentLevel(Enchantments.EFFICIENCY, stack);
			if (efficiency > 0 && !stack.isEmpty())
				speed += efficiency * efficiency + 1;
		}

		return speed;
	}

	private int getEnchantmentLevel(ResourceKey<Enchantment> enchantment, ItemStack stack) {
		// 26.2: enchantments are data-driven, EnchantmentHelper needs a Holder
		return EnchantmentHelper.getItemEnchantmentLevel(
				mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), stack);
	}
}
