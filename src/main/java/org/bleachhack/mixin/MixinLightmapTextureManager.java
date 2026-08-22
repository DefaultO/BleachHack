/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventLightTex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO(26.2): LightmapTextureManager/LightTexture is gone - the lightmap is now computed on the GPU from
// LightmapRenderState, so the per-light-level getBrightness(DimensionType, int) hook no longer exists on
// the lightmap path (Lightmap.getBrightness is only used by the HUD now). As the closest equivalent, the
// Brightness event is posted once per lightmap extraction (light level 15) against the state's brightness
// knob, and the Gamma event redirects the options gamma read like before.
@Mixin(LightmapRenderStateExtractor.class)
public class MixinLightmapTextureManager {

	@Inject(method = "extract", at = @At("TAIL"))
	private void extract_brightness(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null) {
			EventLightTex.Brightness event = new EventLightTex.Brightness(level.dimensionType(), 15, renderState.brightness);
			BleachHack.eventBus.post(event);
			renderState.brightness = event.getBrightness();
		}
	}

	@Redirect(method = "extract", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0))
	private float extract_floatValue(Double instance) {
		EventLightTex.Gamma event = new EventLightTex.Gamma(instance.floatValue());
		BleachHack.eventBus.post(event);
		return event.getGamma();
	}
}
