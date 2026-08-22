/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Triple;
import org.bleachhack.BleachHack;
import org.bleachhack.gui.*;
import org.bleachhack.gui.clickgui.ModuleClickGuiScreen;
import org.bleachhack.gui.window.WindowManagerScreen;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.ClickGui;
import org.bleachhack.setting.option.Option;
import org.bleachhack.util.io.BleachFileHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {

	@Unique private static boolean firstLoad = true;

	private MixinTitleScreen(Component title) {
		super(title);
	}

	@Inject(method = "init()V", at = @At("HEAD"))
	private void init(CallbackInfo info) {
		if (firstLoad) {
			if (Option.GENERAL_SHOW_UPDATE_SCREEN.getValue()) {
				JsonObject updateJson = BleachHack.getUpdateJson();
				if (updateJson != null && updateJson.has("version") && updateJson.get("version").getAsInt() > BleachHack.INTVERSION)
					minecraft.gui.setScreen(new UpdateScreen(null, updateJson));
			}

			firstLoad = false;
			return;
		}

		if (BleachTitleScreen.customTitleScreen) {
			Minecraft.getInstance().gui.setScreen(
					new WindowManagerScreen(
							Triple.of(new BleachTitleScreen(), "BleachHack", (java.util.function.Supplier<ItemStack>) () -> org.bleachhack.util.SafeItem.of(Items.MUSIC_DISC_CAT)),
							Triple.of(new AccountManagerScreen(), "Accounts", (java.util.function.Supplier<ItemStack>) () -> org.bleachhack.util.SafeItem.of(Items.PAPER)),
							Triple.of(ModuleClickGuiScreen.INSTANCE, "ClickGui", (java.util.function.Supplier<ItemStack>) () -> org.bleachhack.util.SafeItem.of(Items.TOTEM_OF_UNDYING)),
							Triple.of(new BleachOptionsScreen(null), "Options", (java.util.function.Supplier<ItemStack>) () -> org.bleachhack.util.SafeItem.of(Items.REDSTONE)),
							Triple.of(new BleachCreditsScreen(), "Credits", (java.util.function.Supplier<ItemStack>) () -> org.bleachhack.util.SafeItem.of(Items.DRAGON_HEAD))) {

						public boolean keyPressed(KeyEvent event) {
							if (event.key() == ModuleManager.getModule(ClickGui.class).getKey()) {
								selectWindow(2);
							}

							return super.keyPressed(event);
						}
					});
		} else {
			addRenderableWidget(Button.builder(Component.literal("BH"), button -> {
				BleachTitleScreen.customTitleScreen = !BleachTitleScreen.customTitleScreen;
				BleachFileHelper.saveMiscSetting("customTitleScreen", new JsonPrimitive(true));
				minecraft.gui.setScreen(new TitleScreen(false));
			}).pos(width / 2 - 124, height / 4 + 96).size(20, 20).build());
		}
	}
}
