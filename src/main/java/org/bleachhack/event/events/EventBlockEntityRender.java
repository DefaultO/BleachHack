/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.event.events;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import org.bleachhack.event.Event;

public class EventBlockEntityRender extends Event {

	public static class Single extends EventBlockEntityRender {

		protected BlockEntity blockEntity;
		protected PoseStack matrices;
		protected SubmitNodeCollector vertex;

		public BlockEntity getBlockEntity() {
			return blockEntity;
		}

		public PoseStack getMatrices() {
			return matrices;
		}

		public SubmitNodeCollector getVertex() {
			return vertex;
		}

		public static class Pre extends Single {

			public Pre(BlockEntity blockEntity, PoseStack matrices, SubmitNodeCollector vertex) {
				this.blockEntity = blockEntity;
				this.matrices = matrices;
				this.vertex = vertex;
			}

			public void setBlockEntity(BlockEntity blockEntity) {
				this.blockEntity = blockEntity;
			}

			public void setMatrices(PoseStack matrices) {
				this.matrices = matrices;
			}

			public void setVertex(SubmitNodeCollector vertex) {
				this.vertex = vertex;
			}
		}

		public static class Post extends Single {
			public Post(BlockEntity blockEntity, PoseStack matrices, SubmitNodeCollector vertexConsumers) {
				this.blockEntity = blockEntity;
				this.matrices = matrices;
				this.vertex = vertexConsumers;
			}
		}
	}

	public static class PreAll extends EventEntityRender {
	}

	public static class PostAll extends EventEntityRender {
	}
}
