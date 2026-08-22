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
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.InventoryUtils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.InteractionHand;

public class AutoEat extends Module {

	private boolean eating;

	public AutoEat() {
		super("AutoEat", KEY_UNBOUND, ModuleCategory.PLAYER, "Automatically eats food for you.",
				new SettingToggle("Hunger", true).withDesc("Eats when you're bewlow a certain amount of hunger.").withChildren(
						new SettingSlider("Hunger", 0, 19, 14, 0).withDesc("The maximum hunger to eat at.")),
				new SettingToggle("Health", false).withDesc("Eats when you're bewlow a certain amount of health.").withChildren(
						new SettingSlider("Health", 0, 19, 14, 0).withDesc("The maximum health to eat at.")),
				new SettingToggle("Gapples", true).withDesc("Eats golden apples.").withChildren(
						new SettingToggle("Prefer", false).withDesc("Prefers golden apples avobe regular food.")),
				new SettingToggle("Chorus", false).withDesc("Eats chorus fruit."),
				new SettingToggle("Poisonous", false).withDesc("Eats poisonous food."));
	}

	@Override
	public void onDisable(boolean inWorld) {
		mc.options.keyUse.setDown(false);

		super.onDisable(inWorld);
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (eating && mc.options.keyUse.isDown() && !mc.player.isUsingItem()) {
			eating = false;
			mc.options.keyUse.setDown(false);
		}

		if (getSetting(0).asToggle().getState() && mc.player.getFoodData().getFoodLevel() <= getSetting(0).asToggle().getChild(0).asSlider().getValueInt()) {
			startEating();
		} else if (getSetting(1).asToggle().getState() && (int) mc.player.getHealth() + (int) mc.player.getAbsorptionAmount() <= getSetting(1).asToggle().getChild(0).asSlider().getValueInt()) {
			startEating();
		}
	}

	private void startEating() {
		boolean gapples = getSetting(2).asToggle().getState();
		boolean preferGapples = getSetting(2).asToggle().getChild(0).asToggle().getState();
		boolean chorus = getSetting(3).asToggle().getState();
		boolean poison = getSetting(4).asToggle().getState();

		int slot = -1;
		int hunger = -1;
		for (int s: InventoryUtils.getInventorySlots(true)) {
			ItemStack stack = mc.player.getInventory().getItem(s);
			FoodProperties food = stack.get(DataComponents.FOOD);

			if (food == null)
				continue;

			boolean isGapple = stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);

			int h = preferGapples && isGapple ? Integer.MAX_VALUE : food.nutrition();

			if (h <= hunger
					|| (!gapples && isGapple)
					|| (!chorus && stack.is(Items.CHORUS_FRUIT))
					|| (!poison && isPoisonous(stack)))
				continue;

			slot = s;
			hunger = h;
		}

		if (hunger != -1) {
			if (slot == mc.player.getInventory().getSelectedSlot() || slot == 40) {
				mc.options.keyUse.setDown(true);
				mc.gameMode.useItem(mc.player, slot == 40 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
				eating = true;
			} else {
				InventoryUtils.selectSlot(slot);
			}
		}
	}

	private boolean isPoisonous(ItemStack stack) {
		// 26.2: food status effects moved from FoodProperties to the CONSUMABLE component
		Consumable consumable = stack.get(DataComponents.CONSUMABLE);
		return consumable != null && consumable.onConsumeEffects().stream()
				.filter(e -> e instanceof ApplyStatusEffectsConsumeEffect)
				.flatMap(e -> ((ApplyStatusEffectsConsumeEffect) e).effects().stream())
				.anyMatch(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
	}
}
