/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventRenderCrosshair;
import org.bleachhack.event.events.EventRenderInGameHud;
import org.bleachhack.event.events.EventRenderOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 26.2: the in-game HUD logic moved from Gui to Hud (Gui is now the screen/overlay manager).
@Mixin(Hud.class)
public class MixinInGameHud {

	@Unique private boolean bypassRenderOverlay = false;
	@Unique private boolean bypassRenderCrosshair = false;

	// 26.2: renderOverlay -> extractTextureOverlay, renderCrosshair -> extractCrosshair (extra DeltaTracker param)
	@Shadow private void extractTextureOverlay(GuiGraphicsExtractor graphics, Identifier texture, float alpha) {}
	@Shadow private void extractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {}

	// 26.2: render -> extractRenderState
	@Inject(method = "extractRenderState", at = @At("RETURN"), cancellable = true)
	private void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		EventRenderInGameHud event = new EventRenderInGameHud(graphics);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			ci.cancel();
		}
	}

	@Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
	private void renderOverlay(GuiGraphicsExtractor graphics, Identifier texture, float opacity, CallbackInfo ci) {
		if (!bypassRenderOverlay) {
			EventRenderOverlay event = new EventRenderOverlay(graphics, texture, opacity);
			BleachHack.eventBus.post(event);

			if (!event.isCancelled()) {
				bypassRenderOverlay = true;
				extractTextureOverlay(graphics, event.getTexture(), event.getOpacity());
				bypassRenderOverlay = false;
			}

			ci.cancel();
		}
	}


	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
	private void renderCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (!bypassRenderCrosshair) {
			EventRenderCrosshair event = new EventRenderCrosshair(graphics);
			BleachHack.eventBus.post(event);

			if (!event.isCancelled()) {
				bypassRenderCrosshair = true;
				extractCrosshair(graphics, deltaTracker);
				bypassRenderCrosshair = false;
			}

			ci.cancel();
		}
	}
}
