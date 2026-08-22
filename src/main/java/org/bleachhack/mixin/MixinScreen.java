/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventRenderScreenBackground;
import org.bleachhack.event.events.EventRenderTooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.Screen;

@Mixin(Screen.class)
public class MixinScreen {

	@Unique private int lastMX;
	@Unique private int lastMY;

	@Unique private boolean skipTooltip;

	// 26.2: renderWithTooltip -> extractRenderStateWithTooltipAndSubtitles
	@Shadow private void extractRenderStateWithTooltipAndSubtitles(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {}

	// 26.2: render -> extractRenderState
	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo callback) {
		lastMX = mouseX;
		lastMY = mouseY;
	}

	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), cancellable = true)
	private void renderWithTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!skipTooltip) {
			EventRenderTooltip event = new EventRenderTooltip((Screen) (Object) this, graphics, mouseX, mouseY, delta);
			BleachHack.eventBus.post(event);

			if (!event.isCancelled()) {
				skipTooltip = true;
				extractRenderStateWithTooltipAndSubtitles(graphics, event.getMouseX(), event.getMouseY(), event.getDelta());
				skipTooltip = false;
			}

			ci.cancel();
		} else {
			skipTooltip = false;
		}
	}

	// 26.2: renderBackground -> extractBackground
	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void renderBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		EventRenderScreenBackground event = new EventRenderScreenBackground(graphics);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			ci.cancel();
		}
	}
}
