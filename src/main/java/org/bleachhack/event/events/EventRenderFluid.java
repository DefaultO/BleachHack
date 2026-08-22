/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.event.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.bleachhack.event.Event;

public class EventRenderFluid extends Event {

	private FluidState state;
	private BlockPos pos;

	// 26.2: cancellation skips the FluidRenderer.tesselate call; no VertexConsumer flows here anymore.
	public EventRenderFluid(FluidState state, BlockPos pos) {
		this.state = state;
		this.pos = pos;
	}

	public FluidState getState() {
		return state;
	}

	public BlockPos getPos() {
		return pos;
	}
}
