/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.util.BleachQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientWorld {

	@Inject(method = "tickEntities", at = @At("HEAD"), cancellable = true)
	private void tickEntities(CallbackInfo info) {
		BleachQueue.nextQueue();

		EventTick event = new EventTick();
		BleachHack.eventBus.post(event);
		if (event.isCancelled())
			info.cancel();
	}

	// TODO(26.2): ClientLevel.getSkyColor(Vec3, float) and getCloudsColor(float) no longer exist —
	// sky/cloud colors are now int-ARGB values computed by the EnvironmentAttributes system
	// (ClientLevel.environmentAttributes().getValue(EnvironmentAttributes.SKY_COLOR/CLOUD_COLOR, pos)).
	// DimensionEffects (DimensionSpecialEffects) was removed entirely, so getDimensionEffects and the
	// EventSkyRender.Properties hook have no target. EventSkyRender needs a rework against the new
	// attribute system (e.g. mixing into EnvironmentAttributeSystem or SkyRenderer).
	/*
	@Shadow @Final private DimensionEffects dimensionEffects;

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
		if (Minecraft.getInstance().level == null) {
			return dimensionEffects;
		}

		EventSkyRender.Properties event = new EventSkyRender.Properties(dimensionEffects);
		BleachHack.eventBus.post(event);

		return event.getSky();
	}
	*/
}
