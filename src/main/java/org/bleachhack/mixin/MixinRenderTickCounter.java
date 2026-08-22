/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.DeltaTracker;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.Timer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 26.2: RenderTickCounter -> DeltaTracker interface; the timing logic lives in the Timer impl.
@Mixin(DeltaTracker.Timer.class)
public class MixinRenderTickCounter {

	@Shadow private float deltaTicks;
	@Shadow private float deltaTickResidual;
	@Shadow private long lastMs;
	@Shadow @Final private float msPerTick;
	@Shadow @Final private FloatUnaryOperator targetMsptProvider;

	@Inject(method = "advanceGameTime", at = @At("HEAD"), cancellable = true)
	private void advanceGameTime(long currentMs, CallbackInfoReturnable<Integer> ci) {
		if (ModuleManager.getModule(Timer.class).isEnabled()) {
			this.deltaTicks = (float) ((currentMs - this.lastMs) / this.targetMsptProvider.apply(this.msPerTick)
					* ModuleManager.getModule(Timer.class).getSetting(0).asSlider().getValue());
			this.lastMs = currentMs;
			this.deltaTickResidual += this.deltaTicks;
			int ticks = (int) this.deltaTickResidual;
			this.deltaTickResidual -= ticks;

			ci.setReturnValue(ticks);
		}
	}
}
