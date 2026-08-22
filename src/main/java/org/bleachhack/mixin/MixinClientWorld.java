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
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventSkyRender;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.util.BleachQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class MixinClientWorld {

	@Shadow @Final private DimensionEffects dimensionEffects;

	@Inject(method = "tickEntities", at = @At("HEAD"), cancellable = true)
	private void tickEntities(CallbackInfo info) {
		BleachQueue.nextQueue();

		EventTick event = new EventTick();
		BleachHack.eventBus.post(event);
		if (event.isCancelled())
			info.cancel();
	}

	@Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
	public void getSkyColor(Vec3 cameraPos, float tickDelta, CallbackInfoReturnable<Vec3> ci) {
		EventSkyRender.Color.SkyColor event = new EventSkyRender.Color.SkyColor(tickDelta);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			ci.setReturnValue(Vec3.ZERO);
		} else if (event.getColor() != null) {
			ci.setReturnValue(event.getColor());
		}
	}

	@Inject(method = "getCloudsColor", at = @At("HEAD"), cancellable = true)
	private void getCloudsColor(float f, CallbackInfoReturnable<Vec3> ci) {
		EventSkyRender.Color.CloudColor event = new EventSkyRender.Color.CloudColor(f);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			ci.setReturnValue(Vec3.ZERO);
		} else if (event.getColor() != null) {
			ci.setReturnValue(event.getColor());
		}
	}

	@Overwrite
	public DimensionEffects getDimensionEffects() {
		if (Minecraft.getInstance().world == null) {
			return dimensionEffects;
		}

		EventSkyRender.Properties event = new EventSkyRender.Properties(dimensionEffects);
		BleachHack.eventBus.post(event);

		return event.getSky();
	}
}
