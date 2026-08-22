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
import org.bleachhack.event.events.EventEntityRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {

	// TODO(26.2): EntityRenderDispatcher.render(Entity, ...) was replaced by submit(EntityRenderState, ...);
	// the actual Entity is no longer available on this path (only its render state), so the event is posted
	// with a null entity. No current consumer of Single.Post uses the entity.
	@Inject(method = "submit", at = @At("RETURN"))
	private <S extends EntityRenderState> void submit_return(S renderState, CameraRenderState camera, double x, double y, double z, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
		EventEntityRender.Single.Post event = new EventEntityRender.Single.Post(null, poseStack, submitNodeCollector);
		BleachHack.eventBus.post(event);
	}
}
