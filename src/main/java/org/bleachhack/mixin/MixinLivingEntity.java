/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventDamage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {

	private MixinLivingEntity(EntityType<?> type, Level world) {
		super(type, world);
	}

	// 26.2: takeKnockback -> knockback(DDD + DamageSource/float/boolean), setVelocity -> setDeltaMovement
	@Redirect(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"))
	private void knockback_setDeltaMovement(LivingEntity entity, double x, double y, double z) {
		EventDamage.Knockback event = new EventDamage.Knockback(x - getDeltaMovement().x(), y - getDeltaMovement().y(), z - getDeltaMovement().z());
		BleachHack.eventBus.post(event);

		if (!event.isCancelled()) {
			setDeltaMovement(event.getVelX() + getDeltaMovement().x(), event.getVelY() + getDeltaMovement().y(), event.getVelZ() + getDeltaMovement().z());
		}
	}

	// 26.2: damage -> hurtServer(ServerLevel, DamageSource, float).
	// TODO(26.2): client-side damage goes through Entity.hurtClient(DamageSource) which LivingEntity does not
	// override, so this event only fires on the integrated server side now.
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void hurtServer(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callbackInfo) {
		EventDamage.Normal event = new EventDamage.Normal(source, amount);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			callbackInfo.setReturnValue(false);
			callbackInfo.cancel();
		}
	}
}
