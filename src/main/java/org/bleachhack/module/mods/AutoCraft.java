package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingItemList;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.BleachLogger;

import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ContainerInput;

import java.util.List;

public class AutoCraft extends Module {

	private int crafted;

	public AutoCraft() {
		super("AutoCraft", KEY_UNBOUND, ModuleCategory.MISC, "Automatically craft things.",
				new SettingItemList("Edit Items", "Items you want to craft.").withDesc("Edit crafting items."),
				new SettingToggle("CraftAll", false).withDesc("Crafts maximum possible amount amount per craft (shift-clicking)."),
				new SettingToggle("Drop", false).withDesc("Automatically drops crafted items (useful for when not enough inventory space)."),
				new SettingToggle("MaxItems", false).withDesc("Turns AutoCraft off after crafting a certain amount of items.").withChildren(
						new SettingSlider("Items", 1, 512, 64, 0).withDesc("How many items to craft."),
						new SettingToggle("Notify", true).withDesc("Notifies you after it finished crafting the items.")));
	}

	@Override
	public void onEnable(boolean inWorld) {
		super.onEnable(inWorld);

		crafted = 0;
		if (getSetting(0).asList(Item.class).getValue().isEmpty()) {
			BleachLogger.error("AutoCraft items are empty.");
			setEnabled(false);
		}
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		SettingToggle maxItems = getSetting(3).asToggle();
		if (maxItems.getState() && crafted >= maxItems.getChild(0).asSlider().getValueInt()) {
			if (maxItems.getChild(1).asToggle().getState())
				BleachLogger.info("Disabled AutoCraft after crafting " + crafted + " items.");

			setEnabled(false);
			return;
		}

		if (!(mc.player.containerMenu instanceof CraftingMenu handler))
			return;

		// quick hack
		mc.player.getRecipeBook().setOpen(handler.getRecipeBookType(), true);

		boolean craftAll = getSetting(1).asToggle().getState();
		boolean drop = getSetting(2).asToggle().getState();

		ContextMap context = SlotDisplayContext.fromLevel(mc.level);

		for (var recipeCollection : mc.player.getRecipeBook().getCollections()) {
			for (var recipe : recipeCollection.getRecipes()) {
				List<ItemStack> results = recipe.resultItems(context);
				if (results.isEmpty())
					continue;

				if (getSetting(0).asList(Item.class).contains(results.get(0).getItem())) {
					mc.gameMode.handlePlaceRecipe(handler.containerId, recipe.id(), craftAll);
					mc.gameMode.handleContainerInput(handler.containerId, 0, 0,
							drop ? ContainerInput.THROW : ContainerInput.QUICK_MOVE, mc.player);

					crafted++;
					return;
				}
			}
		}
	}

}
