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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;

// TODO(26.2): BackgroundRenderer no longer exists; blindness fog now comes from
// MobEffectFogEnvironment/BlindnessFogEnvironment. Making isApplicable return false
// disables the blindness fog (and its darkness modifier) like the old redirect did.
@Mixin(MobEffectFogEnvironment.class)
public abstract class MixinBackgroundRenderer {

	@Shadow public abstract Holder<MobEffect> getMobEffect();

	@Inject(method = "isApplicable(Lnet/minecraft/world/level/material/FogType;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
	private void isApplicable(FogType fogType, Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (getMobEffect() == MobEffects.BLINDNESS && ModuleManager.getModule(NoRender.class).isOverlayToggled(0)) {
			cir.setReturnValue(false);
		}
	}
}
