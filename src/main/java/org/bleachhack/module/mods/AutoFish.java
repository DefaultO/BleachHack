/*
 * some licence stuff here
 */
package org.bleachhack.module.mods;

import java.util.Comparator;

import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.util.InventoryUtils;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;

public class AutoFish extends Module {

	private boolean threwRod;
	private boolean reeledFish;

	public AutoFish() {
		super("AutoFish", KEY_UNBOUND, ModuleCategory.PLAYER, "Automatically fishes for you.",
				new SettingMode("Mode", "Normal", "Aggressive", "Passive").withDesc("AutoFish mode."));
	}

	@Override
	public void onDisable(boolean inWorld) {
		threwRod = false;
		reeledFish = false;

		super.onDisable(inWorld);
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (mc.player.fishing != null) {
			threwRod = false;

			boolean caughtFish = mc.player.fishing.getEntityData().get(FishingHook.DATA_BITING);
			if (!reeledFish && caughtFish) {
				InteractionHand hand = getHandWithRod();
				if (hand != null) {
					// reel back
					mc.gameMode.useItem(mc.player, hand);
					reeledFish = true;
					return;
				}
			} else if (!caughtFish) {
				reeledFish = false;
			}
		}

		if (!threwRod && mc.player.fishing == null && getSetting(0).asMode().getMode() != 2) {
			InteractionHand newHand = getSetting(0).asMode().getMode() == 1 ? InventoryUtils.selectSlot(getBestRodSlot()) : getHandWithRod();
			if (newHand != null) {
				// throw again
				mc.gameMode.useItem(mc.player, newHand);
				threwRod = true;
				reeledFish = false;
			}
		}
	}

	private InteractionHand getHandWithRod() {
		return mc.player.getMainHandItem().getItem() == Items.FISHING_ROD ? InteractionHand.MAIN_HAND
				: mc.player.getOffhandItem().getItem() == Items.FISHING_ROD ? InteractionHand.OFF_HAND
						: null;
	}

	private int getBestRodSlot() {
		int slot = InventoryUtils.getSlot(true, true, Comparator.comparingInt(i -> {
			ItemStack is = mc.player.getInventory().getItem(i);
			if (is.getItem() != Items.FISHING_ROD)
				return -1;

			return EnchantmentHelper.getEnchantmentsForCrafting(is).entrySet().stream().mapToInt(Object2IntMap.Entry::getIntValue).sum();
		}));

		if (mc.player.getInventory().getItem(slot).getItem() == Items.FISHING_ROD) {
			return slot;
		}

		return -1;
	}
}
