/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Strider;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventEntityControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 26.2: isSaddled() is declared on Mob now (the concrete equine/pig/strider classes only
// inherit it), so we inject on Mob and gate by type to keep the original scope.
@Mixin(Mob.class)
public class MixinLlamaPigStriderEntity {

	@Inject(method = "isSaddled", at = @At("HEAD"), cancellable = true)
	private void isSaddled(CallbackInfoReturnable<Boolean> info) {
		Entity self = (Entity) (Object) this;
		if (!(self instanceof AbstractHorse || self instanceof Pig || self instanceof Strider)) {
			return;
		}

		EventEntityControl event = new EventEntityControl();
		BleachHack.eventBus.post(event);

		if (event.canBeControlled() != null) {
			info.setReturnValue(event.canBeControlled());
		}
	}
}
