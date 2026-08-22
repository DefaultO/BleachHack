/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventSkyRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.util.ARGB;

// TODO(26.2): DimensionEffects (yarn) / DimensionSpecialEffects no longer exists. The fog color is now
// computed by AtmosphericFogEnvironment.getBaseColor from EnvironmentAttributes, so the FogColor event
// is posted there instead of the old getFogColorOverride. Cancelling the event has no equivalent anymore
// (there is no "no override" state); only a set color is applied.
@Mixin(AtmosphericFogEnvironment.class)
public class MixinDimensionEffects {

	@Inject(method = "getBaseColor(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/Camera;IF)I", at = @At("RETURN"), cancellable = true)
	private void getBaseColor(ClientLevel level, Camera camera, int renderDistance, float partialTicks, CallbackInfoReturnable<Integer> cir) {
		EventSkyRender.Color.FogColor event = new EventSkyRender.Color.FogColor(partialTicks);
		BleachHack.eventBus.post(event);

		if (!event.isCancelled() && event.getColor() != null) {
			cir.setReturnValue(ARGB.colorFromFloat(1f, (float) event.getColor().x, (float) event.getColor().y, (float) event.getColor().z));
		}
	}
}
