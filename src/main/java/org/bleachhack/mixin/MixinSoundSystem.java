/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventSoundPlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class MixinSoundSystem {

	// 26.2: play(SoundInstance) now returns SoundEngine.PlayResult, so cancel via setReturnValue(NOT_STARTED).
	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void play(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		EventSoundPlay.Normal event = new EventSoundPlay.Normal(soundInstance);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
		}
	}

	// 26.2: Yarn play(SoundInstance, int) renamed to playDelayed(SoundInstance, int).
	@Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
	private void play(SoundInstance soundInstance, int delay, CallbackInfo ci) {
		EventSoundPlay.Normal event = new EventSoundPlay.Normal(soundInstance);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			ci.cancel();
		}
	}

	// 26.2: Yarn playNextTick(TickableSoundInstance) renamed to queueTickingSound(TickableSoundInstance).
	@Inject(method = "queueTickingSound", at = @At("HEAD"), cancellable = true)
	private void playNextTick(TickableSoundInstance sound, CallbackInfo ci) {
		EventSoundPlay.Normal event = new EventSoundPlay.Normal(sound);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			ci.cancel();
		}
	}

	// 26.2: Yarn addPreloadedSound(Sound) renamed to requestPreload(Sound).
	@Inject(method = "requestPreload", at = @At("HEAD"), cancellable = true)
	private void addPreloadedSound(Sound sound, CallbackInfo ci) {
		EventSoundPlay.Preloaded event = new EventSoundPlay.Preloaded(sound);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			ci.cancel();
		}
	}
}
