/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventKeyPress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class MixinChatScreen {

	// 26.2: keyPressed(int,int,int) -> keyPressed(KeyEvent)
	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> callback) {
		EventKeyPress.InChat event = new EventKeyPress.InChat(keyEvent.key(), keyEvent.scancode(), EventKeyPress.Status.PRESSED, keyEvent.modifiers());
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			callback.setReturnValue(false);
		}
	}
}
