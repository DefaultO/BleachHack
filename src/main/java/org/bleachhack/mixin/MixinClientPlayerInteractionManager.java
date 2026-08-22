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
import org.bleachhack.event.events.EventBlockBreakCooldown;
import org.bleachhack.event.events.EventInteract;
import org.bleachhack.event.events.EventReach;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

@Mixin(MultiPlayerGameMode.class)
public class MixinClientPlayerInteractionManager {

	@Shadow private int destroyDelay;

	@Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", ordinal = 3),
			require = 0 /* TODO: meteor compatibility */)
	private void updateBlockBreakingProgress(MultiPlayerGameMode clientPlayerInteractionManager, int newCooldown) {
		EventBlockBreakCooldown event = new EventBlockBreakCooldown(newCooldown);
		BleachHack.eventBus.post(event);

		this.destroyDelay = event.getCooldown();
	}

	@Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", ordinal = 4),
			require = 0 /* TODO: meteor compatibility */)
	private void updateBlockBreakingProgress2(MultiPlayerGameMode clientPlayerInteractionManager, int newCooldown) {
		EventBlockBreakCooldown event = new EventBlockBreakCooldown(newCooldown);
		BleachHack.eventBus.post(event);

		this.destroyDelay = event.getCooldown();
	}

	@Redirect(method = "startDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I"),
			require = 0 /* TODO: meteor compatibility */)
	private void attackBlock(MultiPlayerGameMode clientPlayerInteractionManager, int newCooldown) {
		EventBlockBreakCooldown event = new EventBlockBreakCooldown(newCooldown);
		BleachHack.eventBus.post(event);

		this.destroyDelay = event.getCooldown();
	}

	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
	private void breakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
		EventInteract.BreakBlock event = new EventInteract.BreakBlock(pos);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			callback.setReturnValue(false);
		}
	}

	@Inject(method = { "startDestroyBlock", "continueDestroyBlock" }, at = @At("HEAD"), cancellable = true)
	private void attackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> callback) {
		EventInteract.AttackBlock event = new EventInteract.AttackBlock(pos, direction);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			callback.setReturnValue(false);
		}
	}

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void interactBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> callback) {
		EventInteract.InteractBlock event = new EventInteract.InteractBlock(hand, hitResult);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			callback.setReturnValue(InteractionResult.PASS);
		}
	}

	@Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
	private void interactItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
		EventInteract.InteractItem event = new EventInteract.InteractItem(hand);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			callback.setReturnValue(InteractionResult.PASS);
		}
	}

	// TODO(26.2): MultiPlayerGameMode#getReachDistance was removed; reach is now a Player attribute
	// (Attributes.BLOCK_INTERACTION_RANGE / ENTITY_INTERACTION_RANGE). EventReach must hook there instead.
	// @Inject(method = "getReachDistance", at = @At("RETURN"), cancellable = true)
	// private void getReachDistance(CallbackInfoReturnable<Float> callback) {
	// 	EventReach event = new EventReach(callback.getReturnValueF());
	// 	BleachHack.eventBus.post(event);
	//
	// 	callback.setReturnValue(event.getReach());
	// }
}
