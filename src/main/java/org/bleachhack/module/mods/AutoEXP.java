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
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.InventoryUtils;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class AutoEXP extends Module {

	private int delay;
	private int xpNeeded;
	private int slot = -1;

	public AutoEXP() {
		super("AutoEXP", KEY_UNBOUND, ModuleCategory.PLAYER, "Automatically uses XP bottles to repair items with mending.",
				new SettingToggle("Armor", true).withDesc("Uses XP when your armor durability is low."),
				new SettingToggle("MainHand", true).withDesc("Uses XP when your mainhand item durability is low."),
				new SettingToggle("OffHand", true).withDesc("Uses XP when your offhand item durability is low."),
				new SettingSlider("Durability", 0, 20, 5, 0).withDesc("How low the item dirability has to be before repairing."),
				new SettingSlider("Repair", 0, 1, 1, 2).withDesc("How much durability to repair."),
				new SettingSlider("XP/tick", 1, 10, 1, 0).withDesc("How many xp bottles to throw each batch."),
				new SettingSlider("Delay", 0, 10, 0, 0).withDesc("How long to wait before throwing each batch (in ticks)."));
	}

	@Override
	public void onDisable(boolean inWorld) {
		delay = 0;
		xpNeeded = 0;
		slot = -1;

		super.onDisable(inWorld);
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (mc.player.containerMenu != mc.player.inventoryMenu)
			return;

		int xpSlot = InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.EXPERIENCE_BOTTLE);
		if (xpSlot == -1)
			return;

		int damage = getSetting(3).asSlider().getValueInt();
		double target = getSetting(4).asSlider().getValue();

		if (slot != -1) {
			if (xpNeeded == 0) {
				ItemStack item = slot < 45 ? mc.player.containerMenu.getSlot(slot).getItem() : mc.player.getOffhandItem();
				if (item.isDamaged() && item.getMaxDamage() - item.getDamageValue() <= damage)
					return;

				for (int i = 1; i <= 4; i++) {
					ItemStack stack = mc.player.containerMenu.getSlot(i).getItem();
					if (!stack.isEmpty()) {
						for (int j = 5; j <= 8; j++) {
							if (mc.player.containerMenu.getSlot(j).mayPlace(stack)) {
								mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, i, 0, ContainerInput.PICKUP, mc.player);
								mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, j, 0, ContainerInput.PICKUP, mc.player);
								return;
							}
						}
					}
				}

				if (slot >= 46) {
					if (slot - 46 != mc.player.getInventory().getSelectedSlot()) {
						mc.player.getInventory().setSelectedSlot(slot - 46);
						mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot - 46));
					}

					mc.player.connection.send(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
				}

				delay = 0;
				slot = -1;
				return;
			}

			Holder<Enchantment> mending = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);

			for (int i = 5; i <= 8; i++) {
				if (i != slot && EnchantmentHelper.getItemEnchantmentLevel(mending, mc.player.containerMenu.getSlot(i).getItem()) != 0) {
					for (int j = 1; j <= 4; j++) {
						ItemStack craftingStack = mc.player.containerMenu.getSlot(j).getItem();
						if (!craftingStack.isDamageableItem()) {
							mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, i, 0, ContainerInput.PICKUP, mc.player);
							mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, j, 0, ContainerInput.PICKUP, mc.player);
							if (!craftingStack.isEmpty())
								mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, j, 1, ContainerInput.THROW, mc.player);

							return;
						}
					}
				}
			}

			if (slot > 8 && slot < 45) {
				if (slot - 36 != mc.player.getInventory().getSelectedSlot()) {
					mc.player.getInventory().setSelectedSlot(slot - 36);
					mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot - 36));
				}

				mc.player.connection.send(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
				slot += 10; // hack
				return;
			}

			delay++;
			if (delay >= getSetting(6).asSlider().getValueInt()) {
				delay = 0;
				int toThrow = Math.min(getSetting(5).asSlider().getValueInt(), xpNeeded);

				if (toThrow != 0) {
					mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), 90, mc.player.onGround(), mc.player.horizontalCollision));
					for (int t = 0; t < toThrow; t++) {
						if (InventoryUtils.selectSlot(false, i -> mc.player.getInventory().getItem(i).getItem() == Items.EXPERIENCE_BOTTLE) == InteractionHand.MAIN_HAND) {
							// Trying to use without bruh
							mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), 90));
							InteractionResult result = mc.player.getMainHandItem().use(mc.level, mc.player, InteractionHand.MAIN_HAND);
							if (result instanceof InteractionResult.Success success && success.heldItemTransformedTo() != null) {
								mc.player.setItemInHand(InteractionHand.MAIN_HAND, success.heldItemTransformedTo());
							}

							xpNeeded--;
						}
					}
				}
			}

			return;
		}

		IntSet slots = new IntArraySet();
		if (getSetting(0).asToggle().getState()) {
			slots.add(5);
			slots.add(6);
			slots.add(7);
			slots.add(8);
		}

		if (getSetting(1).asToggle().getState())
			slots.add(36 + mc.player.getInventory().getSelectedSlot());

		if (getSetting(2).asToggle().getState())
			slots.add(45);

		if (getSetting(0).asToggle().getState()) {
			for (int s: slots) {
				ItemStack item = mc.player.containerMenu.getSlot(s).getItem();

				if (item.isDamageableItem() && item.getMaxDamage() - item.getDamageValue() <= damage
						&& item.getMaxDamage() - item.getDamageValue() < item.getMaxDamage() * target) {
					slot = s;
					xpNeeded = (int) Math.ceil((item.getMaxDamage() * target - (item.getMaxDamage() - item.getDamageValue())) / 14d);
					return;
				}
			}
		}
	}

	@BleachSubscribe
	public void onSendPacket(EventPacket.Send event) {
		if (slot != -1 && event.getPacket() instanceof ServerboundContainerClosePacket) {
			event.setCancelled(true);
		}
	}
}
