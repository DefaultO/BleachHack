/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.ScreenEffectRenderer;

@Mixin(ScreenEffectRenderer.class)
public class MixinInGameOverlayRenderer {

	// 26.2: renderFireOverlay -> submitFire (deferred-submit pipeline)
	@Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
	private static void onRenderFireOverlay(CallbackInfo ci) {
		if (ModuleManager.getModule(NoRender.class).isOverlayToggled(1)) {
			ci.cancel();
		}
	}

	// 26.2: renderUnderwaterOverlay -> submitWater
	@Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
	private static void onRenderUnderwaterOverlay(CallbackInfo ci) {
		if (ModuleManager.getModule(NoRender.class).isOverlayToggled(3)) {
			ci.cancel();
		}
	}
}
