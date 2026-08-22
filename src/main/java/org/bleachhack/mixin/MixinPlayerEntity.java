/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.SpeedMine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayerEntity extends LivingEntity {

	@Shadow @Final private Inventory inventory;

	private MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, Level world) {
		super(entityType, world);
	}

	// 26.2: getBlockBreakingSpeed -> getDestroySpeed; efficiency/aqua affinity now apply via attributes
	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
	private void getDestroySpeed(BlockState block, CallbackInfoReturnable<Float> ci) {
		Module speedMine = ModuleManager.getModule(SpeedMine.class);

		if (speedMine.isEnabled()) {
			float breakingSpeed = inventory.getSelectedItem().getDestroySpeed(block);
			if (breakingSpeed > 1.0F) {
				breakingSpeed += (float) this.getAttributeValue(Attributes.MINING_EFFICIENCY);
			}

			if (MobEffectUtil.hasDigSpeed(this)) {
				breakingSpeed *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
			}

			if (!speedMine.getSetting(4).asToggle().getState()) {
				if (this.hasEffect(MobEffects.MINING_FATIGUE)) {
					float fatigueMult = switch (this.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
						case 0 -> 0.3F;
						case 1 -> 0.09F;
						case 2 -> 0.0027F;
						default -> 8.1E-4F;
					};

					breakingSpeed *= fatigueMult;
				}
			}

			breakingSpeed *= (float) this.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);

			if (!speedMine.getSetting(5).asToggle().getState()) {
				if (this.isEyeInFluid(FluidTags.WATER)) {
					breakingSpeed *= (float) this.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue();
				}

				if (!this.onGround()) {
					breakingSpeed /= 5.0F;
				}
			}

			if (speedMine.getSetting(0).asMode().getMode() == 1)
				breakingSpeed *= speedMine.getSetting(3).asSlider().getValueFloat();

			ci.setReturnValue(breakingSpeed);
		}
	}
}
