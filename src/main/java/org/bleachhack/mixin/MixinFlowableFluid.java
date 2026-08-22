/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.core.Direction;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.NoVelocity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;

@Mixin(FlowingFluid.class)
public class MixinFlowableFluid {

	/** Yeet the first iterator which handles the horizontal fluid movement **/
	@Redirect(method = "getFlow", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z", ordinal = 0))
	private boolean getVelocity_hasNext(Iterator<Direction> var9) {
		if (ModuleManager.getModule(NoVelocity.class).isEnabled()
				&& ModuleManager.getModule(NoVelocity.class).getSetting(3).asToggle().getState()) {
			return false;
		}

		return var9.hasNext();
	}

}
