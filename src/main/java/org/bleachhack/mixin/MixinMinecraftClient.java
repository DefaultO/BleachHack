/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventWorldRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraftClient {

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V", shift = Shift.BEFORE))
	private void init(GameConfig args, CallbackInfo callback) {
		BleachHack.getInstance().postInit();
	}

	// Fired inside the per-frame gizmo collector scope so handlers can draw
	// world overlays through org.bleachhack.util.render.Renderer.
	@Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"))
	private void preWorldRender(boolean advanceGameTime, CallbackInfo callback) {
		Minecraft mc = (Minecraft) (Object) this;
		if (mc.level != null && mc.player != null) {
			BleachHack.eventBus.post(new EventWorldRender.Pre(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)));
		}
	}

	@Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V", shift = Shift.AFTER))
	private void postWorldRender(boolean advanceGameTime, CallbackInfo callback) {
		Minecraft mc = (Minecraft) (Object) this;
		if (mc.level != null && mc.player != null) {
			BleachHack.eventBus.post(new EventWorldRender.Post(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)));
		}
	}
}
