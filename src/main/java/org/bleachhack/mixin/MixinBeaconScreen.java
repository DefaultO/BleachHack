/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconScreen.class)
public abstract class MixinBeaconScreen extends AbstractContainerScreen<BeaconMenu> {

	@Unique private boolean unlocked = false;

	private MixinBeaconScreen(BeaconMenu handler, Inventory inventory, Component title) {
		super(handler, inventory, title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void init(CallbackInfo callback) {
		addDrawableChild(Button.builder(Component.literal("Unlock"), button -> unlocked = true)
				.position((width - backgroundWidth) / 2 + 2, (height - backgroundHeight) / 2 - 15).size(46, 14).build());
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void render(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (unlocked) {
			for (Renderable b: ((AccessorScreen) this).getDrawables()) {
				if (b instanceof AbstractWidget) {
					((AbstractWidget) b).active = true;
				}
			}
		}
	}
}
