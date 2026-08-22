/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.BleachQueue;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

public class AutoArmor extends Module {

	private int tickDelay = 0;

	public AutoArmor() {
		super("AutoArmor", KEY_UNBOUND, ModuleCategory.PLAYER, "Automatically equips armor.",
				new SettingToggle("AntiBreak", false).withDesc("Unequips your armor when its about to break."),
				new SettingToggle("PreferElytra", false).withDesc("Equips elytras instead of chestplates when possible."),
				new SettingToggle("Delay", true).withDesc("Adds a delay between equipping armor pieces.").withChildren(
						new SettingSlider("Delay", 0, 20, 1, 0).withDesc("How many ticks between putting on armor pieces.")));
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (mc.player.inventoryMenu != mc.player.containerMenu || !BleachQueue.isEmpty("autoarmor_equip"))
			return;

		if (tickDelay > 0) {
			tickDelay--;
			return;
		}

		tickDelay = (getSetting(2).asToggle().getState() ? getSetting(2).asToggle().getChild(0).asSlider().getValueInt() : 0);

		/* [Slot type, [Armor slot, Armor prot, New armor slot, New armor prot]] */
		Map<EquipmentSlot, int[]> armorMap = new HashMap<>(4);
		armorMap.put(EquipmentSlot.FEET, new int[] { 36, getProtection(mc.player.getInventory().getItem(36)), -1, -1 });
		armorMap.put(EquipmentSlot.LEGS, new int[] { 37, getProtection(mc.player.getInventory().getItem(37)), -1, -1 });
		armorMap.put(EquipmentSlot.CHEST, new int[] { 38, getProtection(mc.player.getInventory().getItem(38)), -1, -1 });
		armorMap.put(EquipmentSlot.HEAD, new int[] { 39, getProtection(mc.player.getInventory().getItem(39)), -1, -1 });

		/* Anti Break */
		if (getSetting(0).asToggle().getState()) {
			for (Entry<EquipmentSlot, int[]> e: armorMap.entrySet()) {
				ItemStack is = mc.player.getInventory().getItem(e.getValue()[0]);
				int armorSlot = (e.getValue()[0] - 34) + (39 - e.getValue()[0]) * 2;

				if (is.isDamageableItem() && is.getMaxDamage() - is.getDamageValue() < 7) {
					/* Look for an empty slot to quick move to */
					int forceMoveSlot = -1;
					for (int s = 0; s < 36; s++) {
						if (mc.player.getInventory().getItem(s).isEmpty()) {
							mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, armorSlot, 1, ContainerInput.QUICK_MOVE, mc.player);
							return;
						} else if (!mc.player.getInventory().getItem(s).has(DataComponents.TOOL)
								&& !isArmor(mc.player.getInventory().getItem(s))
								&& mc.player.getInventory().getItem(s).getItem() != Items.ELYTRA
								&& mc.player.getInventory().getItem(s).getItem() != Items.TOTEM_OF_UNDYING && forceMoveSlot == -1) {
							forceMoveSlot = s;
						}
					}

					/* Bruh no empty spots, then force move to a non-totem/tool/armor item */
					if (forceMoveSlot != -1) {
						//System.out.println(forceMoveSlot);
						mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,
								forceMoveSlot < 9 ? 36 + forceMoveSlot : forceMoveSlot, 1, ContainerInput.THROW, mc.player);
						mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, armorSlot, 1, ContainerInput.QUICK_MOVE, mc.player);
						return;
					}

					/* No spots to move to, yeet the armor to not cause any bruh moments */
					mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, armorSlot, 1, ContainerInput.THROW, mc.player);
					return;
				}
			}
		}

		for (int s = 0; s < 36; s++) {
			int prot = getProtection(mc.player.getInventory().getItem(s));

			if (prot > 0) {
				ItemStack st = mc.player.getInventory().getItem(s);
				EquipmentSlot slot = (st.getItem() == Items.ELYTRA
						? EquipmentSlot.CHEST : st.get(DataComponents.EQUIPPABLE).slot());

				for (Entry<EquipmentSlot, int[]> e: armorMap.entrySet()) {
					if (e.getKey() == slot) {
						if (prot > e.getValue()[1] && prot > e.getValue()[3]) {
							e.getValue()[2] = s;
							e.getValue()[3] = prot;
						}
					}
				}
			}
		}

		for (Entry<EquipmentSlot, int[]> e: armorMap.entrySet()) {
			if (e.getValue()[2] != -1) {
				if (e.getValue()[1] == -1 && e.getValue()[2] < 9) {
					if (e.getValue()[2] != mc.player.getInventory().getSelectedSlot()) {
						mc.player.getInventory().setSelectedSlot(e.getValue()[2]);
						mc.player.connection.send(new ServerboundSetCarriedItemPacket(e.getValue()[2]));
					}

					mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 36 + e.getValue()[2], 1, ContainerInput.QUICK_MOVE, mc.player);
				} else if (mc.player.inventoryMenu == mc.player.containerMenu) {
					/* Convert inventory slots to container slots */
					int armorSlot = (e.getValue()[0] - 34) + (39 - e.getValue()[0]) * 2;
					int newArmorslot = e.getValue()[2] < 9 ? 36 + e.getValue()[2] : e.getValue()[2];

					mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, newArmorslot, 0, ContainerInput.PICKUP, mc.player);
					mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, armorSlot, 0, ContainerInput.PICKUP, mc.player);

					if (e.getValue()[1] != -1)
						mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, newArmorslot, 0, ContainerInput.PICKUP, mc.player);
				}

				return;
			}
		}
	}

	private int getProtection(ItemStack is) {
		if (isArmor(is) || is.getItem() == Items.ELYTRA) {
			int prot = 0;

			if (is.getItem() == Items.ELYTRA) {
				// 26.2: ElytraItem.isUsable(stack) -> !stack.nextDamageWillBreak()
				if (is.nextDamageWillBreak())
					return 0;

				if (getSetting(1).asToggle().getState()) {
					prot = 32767;
				} else {
					prot = 1;
				}
			} else if (is.getMaxDamage() - is.getDamageValue() < 7 && getSetting(0).asToggle().getState()) {
				return 0;
			}

			if (is.isEnchanted()) {
				// 26.2: enchantments are data components now, protection enchants matched by key
				ItemEnchantments ench = is.getEnchantments();
				for (Holder<Enchantment> e: ench.keySet()) {
					if (e.is(Enchantments.PROTECTION) || e.is(Enchantments.FIRE_PROTECTION)
							|| e.is(Enchantments.BLAST_PROTECTION) || e.is(Enchantments.PROJECTILE_PROTECTION)
							|| e.is(Enchantments.FEATHER_FALLING))
						prot += ench.getLevel(e);
				}
			}

			return (isArmor(is) ? getArmorPoints(is) : 0) + prot;
		} else if (!is.isEmpty()) {
			return 0;
		}

		return -1;
	}

	/** 26.2: ArmorItem is gone, armor = item with an EQUIPPABLE component in a humanoid armor slot. */
	private static boolean isArmor(ItemStack is) {
		Equippable eq = is.get(DataComponents.EQUIPPABLE);
		return eq != null && eq.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
	}

	/** 26.2: ArmorItem.getProtection() -> armor attribute from the ATTRIBUTE_MODIFIERS component. */
	private static int getArmorPoints(ItemStack is) {
		int armor = 0;
		for (ItemAttributeModifiers.Entry entry: is.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
			if (entry.attribute().is(Attributes.ARMOR))
				armor += (int) entry.modifier().amount();
		}
		return armor;
	}
}
