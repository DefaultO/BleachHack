/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.level.material.FogType;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.BetterCamera;
import org.bleachhack.module.mods.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class MixinCamera {

	@Unique private boolean bypassCameraClip;

	// 26.2: clipToSpace -> getMaxZoom(float)
	@Shadow private float getMaxZoom(float desiredCameraDistance) { return 0; }

	// 26.2: getSubmersionType -> getFluidInCamera, CameraSubmersionType -> FogType
	@Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
	private void getFluidInCamera(CallbackInfoReturnable<FogType> ci) {
		if (ModuleManager.getModule(NoRender.class).isOverlayToggled(3)) {
			ci.setReturnValue(FogType.NONE);
		}
	}

	@Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
	private void onGetMaxZoom(float desiredCameraDistance, CallbackInfoReturnable<Float> info) {
		if (bypassCameraClip) {
			bypassCameraClip = false;
		} else {
			Module betterCamera = ModuleManager.getModule(BetterCamera.class);

			if (betterCamera.isEnabled()) {
				if (betterCamera.getSetting(0).asToggle().getState()) {
					info.setReturnValue(betterCamera.getSetting(1).asToggle().getState()
							? betterCamera.getSetting(1).asToggle().getChild(0).asSlider().getValue().floatValue() : desiredCameraDistance);
				} else if (betterCamera.getSetting(1).asToggle().getState()) {
					bypassCameraClip = true;
					info.setReturnValue(getMaxZoom(betterCamera.getSetting(1).asToggle().getChild(0).asSlider().getValue().floatValue()));
				}
			}
		}
	}
}
