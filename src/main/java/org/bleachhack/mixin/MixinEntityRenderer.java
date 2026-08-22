/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventEntityRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {

	// TODO(26.2): renderLabelIfPresent no longer exists; name tags are extracted into the render state
	// (extractNameTags) and submitted later without access to the entity. The Label event is therefore
	// posted during extraction, with null matrices/vertex (no consumer uses them), and cancelling clears
	// the name tag from the render state.
	@Inject(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V", at = @At("TAIL"))
	private void extractNameTags(T entity, S state, float partialTicks, double nameTagDistance, double belowNameDistance, CallbackInfo info) {
		if (state.nameTag == null && state.scoreText == null) {
			return;
		}

		EventEntityRender.Single.Label event = new EventEntityRender.Single.Label(entity, null, null);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			state.nameTag = null;
			state.scoreText = null;
		}
	}
}
