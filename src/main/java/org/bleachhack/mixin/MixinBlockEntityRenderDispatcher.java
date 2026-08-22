/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventBlockEntityRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {

	// 26.2: block entities render via extract->submit; cancelling extraction (return null)
	// suppresses the block entity entirely. PoseStack/vertex no longer exist at this point.
	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private <E extends BlockEntity, S extends BlockEntityRenderState> void tryExtractRenderState(E blockEntity, float partialTicks,
			ModelFeatureRenderer.CrumblingOverlay breakProgress, boolean isGloballyRendered, CallbackInfoReturnable<S> cir) {
		EventBlockEntityRender.Single.Pre event = new EventBlockEntityRender.Single.Pre(blockEntity, null, null);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			cir.setReturnValue(null);
		}
	}

	@Inject(method = "tryExtractRenderState", at = @At("RETURN"))
	private <E extends BlockEntity, S extends BlockEntityRenderState> void tryExtractRenderState_return(E blockEntity, float partialTicks,
			ModelFeatureRenderer.CrumblingOverlay breakProgress, boolean isGloballyRendered, CallbackInfoReturnable<S> cir) {
		if (cir.getReturnValue() != null) {
			BleachHack.eventBus.post(new EventBlockEntityRender.Single.Post(blockEntity, null, null));
		}
	}
}
