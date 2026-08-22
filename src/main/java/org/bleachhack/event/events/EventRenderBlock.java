/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.event.events;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import org.bleachhack.event.Event;

public class EventRenderBlock extends Event {

	private BlockState state;

	public EventRenderBlock(BlockState state) {
		this.state = state;
	}

	public BlockState getState() {
		return state;
	}
	
	public static class Light extends EventRenderBlock {

		private Float light;

		public Light(BlockState state) {
			super(state);
		}

		public Float getLight() {
			return light;
		}

		public void setLight(float light) {
			this.light = light;
		}
	}

	public static class Opaque extends EventRenderBlock {

		private Boolean opaque;

		public Opaque(BlockState state) {
			super(state);
		}

		public Boolean isOpaque() {
			return opaque;
		}

		public void setOpaque(boolean opaque) {
			this.opaque = opaque;
		}
	}

	public static class ShouldDrawSide extends EventRenderBlock {

		private Boolean drawSide;

		public ShouldDrawSide(BlockState state) {
			super(state);
		}

		public Boolean shouldDrawSide() {
			return drawSide;
		}

		public void setDrawSide(boolean drawSide) {
			this.drawSide = drawSide;
		}
	}

	public static class Tesselate extends EventRenderBlock {

		private BlockPos pos;

		// 26.2: tesselation is cancelled by skipping the tesselateBlock call; no PoseStack/VertexConsumer flows here anymore.
		public Tesselate(BlockState state, BlockPos pos) {
			super(state);
			this.pos = pos;
		}

		public BlockPos getPos() {
			return pos;
		}
	}

	public static class Layer extends EventRenderBlock {

		private RenderType layer;

		public Layer(BlockState state) {
			super(state);
		}

		public RenderType getLayer() {
			return layer;
		}

		public void setLayer(RenderType layer) {
			this.layer = layer;
		}
	}
}
