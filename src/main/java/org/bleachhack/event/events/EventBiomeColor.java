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
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import org.bleachhack.event.Event;

public class EventBiomeColor extends Event {

	protected BlockAndTintGetter world;
	protected BlockPos pos;
	protected int color;

	public static class Grass extends EventBiomeColor {

		public Grass(BlockAndTintGetter world, BlockPos pos, int color) {
			this.world = world;
			this.pos = pos;
			this.color = color;
		}

	}

	public static class Foilage extends EventBiomeColor {

		public Foilage(BlockAndTintGetter world, BlockPos pos, int color) {
			this.world = world;
			this.pos = pos;
			this.color = color;
		}

	}

	public static class Water extends EventBiomeColor {

		public Water(BlockAndTintGetter world, BlockPos pos, int color) {
			this.world = world;
			this.pos = pos;
			this.color = color;
		}

	}

	public BlockAndTintGetter getWorld() {
		return world;
	}

	public void setWorld(BlockAndTintGetter world) {
		this.world = world;
	}

	public BlockPos getPos() {
		return pos;
	}

	public void setPos(BlockPos pos) {
		this.pos = pos;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

}
