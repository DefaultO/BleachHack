package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingItemList;
import org.bleachhack.util.BleachLogger;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.ContainerInput;

public class AutoGrind extends Module {

	public AutoGrind() {
		super("AutoGrind", KEY_UNBOUND, ModuleCategory.MISC, "Automatically grind enchants off items.",
				new SettingItemList("Edit Items", "Items you want to grind.").withDesc("Edit items to grind."));
	}

	@Override
	public void onEnable(boolean inWorld) {
		super.onEnable(inWorld);

		if (getSetting(0).asList(Item.class).getValue().isEmpty()) {
			BleachLogger.error("AutoGrind items are empty.");
			setEnabled(false);
		}
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (!(mc.player.containerMenu instanceof GrindstoneMenu))
			return;

		GrindstoneMenu handler = (GrindstoneMenu) mc.player.containerMenu;

		// if there's already an item in the grindstone, don't do anything
		if (!(handler.getSlot(0).getItem().isEmpty() && handler.getSlot(1).getItem().isEmpty()))
			return;

		for (int slot = 3; slot <= 38; slot++) {
			ItemStack stack = handler.getSlot(slot).getItem();
			// if this is one of the items on our list
			if (shouldGrind(stack)) {
				// if item has grindable enchants
				if (canGrind(stack)) {
					// shift-click item into gridstone slot
					mc.gameMode.handleContainerInput(handler.containerId, slot, 0, ContainerInput.QUICK_MOVE, mc.player);
					// grind
					doGrind(handler, slot);
					// wait til next tick before grinding another one
					return;
				}
				// continue to next item
			}
		}
	}

	private boolean shouldGrind(ItemStack stack) {
		return getSetting(0).asList(Item.class).contains(stack.getItem());
	}

	private boolean canGrind(ItemStack stack) {
		int enchants = getEnchantCount(stack);
		int curses = getCurseCount(stack);
		return (enchants - curses > 0);
	}

	private void doGrind(GrindstoneMenu handler, int destinationSlot) {
		// pick up from grindstone output slot (2)
		mc.gameMode.handleContainerInput(handler.containerId, 2, 0, ContainerInput.PICKUP, mc.player);
		// click the original slot to put the de-enchanted item back
		mc.gameMode.handleContainerInput(handler.containerId, destinationSlot, 0, ContainerInput.PICKUP, mc.player);
	}

	private int getEnchantCount(ItemStack stack) {
		// 26.2: getEnchantmentsForCrafting also covers stored enchantments on enchanted books
		return EnchantmentHelper.getEnchantmentsForCrafting(stack).size();
	}

	private int getCurseCount(ItemStack stack) {
		// needed as EnchantmentHelper's curse checks are bugged for Enchanted Books
		ItemEnchantments ench = EnchantmentHelper.getEnchantmentsForCrafting(stack);
		int curses = 0;
		for (Holder<Enchantment> h: ench.keySet()) {
			if (h.is(Enchantments.BINDING_CURSE) || h.is(Enchantments.VANISHING_CURSE)) {
				curses += ench.getLevel(h);
			}
		}
		return curses;
	}
}
