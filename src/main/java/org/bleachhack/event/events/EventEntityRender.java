/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.event.events;

import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import org.bleachhack.event.Event;

public class EventEntityRender extends Event {

	public static class Single extends EventEntityRender {

		protected Entity entity;
		protected PoseStack matrices;
		protected SubmitNodeCollector vertex;

		public Entity getEntity() {
			return entity;
		}

		public PoseStack getMatrix() {
			return matrices;
		}

		public SubmitNodeCollector getVertex() {
			return vertex;
		}

		public static class Pre extends Single {

			public Pre(Entity entity, PoseStack matrices, SubmitNodeCollector vertex) {
				this.entity = entity;
				this.matrices = matrices;
				this.vertex = vertex;
			}

			public void setMatrix(PoseStack matrices) {
				this.matrices = matrices;
			}

			public void setVertex(SubmitNodeCollector vertex) {
				this.vertex = vertex;
			}

			public void setEntity(Entity entity) {
				this.entity = entity;
			}
		}

		public static class Post extends Single {

			public Post(Entity entity, PoseStack matrices, SubmitNodeCollector vertex) {
				this.entity = entity;
				this.matrices = matrices;
				this.vertex = vertex;
			}
		}

		/**
		 * Fired while an entity's render state is extracted, so modules can give it a
		 * glow outline. 26.2 renders any entity with a non-zero outlineColor into the
		 * entity_outline framebuffer, which the sobel+blur post chain turns into a glow.
		 * Color must be opaque (the sobel pass keys off alpha).
		 */
		public static class Outline extends Single {

			private Integer color;

			public Outline(Entity entity) {
				this.entity = entity;
			}

			public Integer getColor() {
				return color;
			}

			public void setColor(int color) {
				this.color = color;
			}
		}

		public static class Label extends Single {

			public Label(Entity entity, PoseStack matrices, SubmitNodeCollector vertex) {
				this.entity = entity;
				this.matrices = matrices;
				this.vertex = vertex;
			}

			public void setMatrix(PoseStack matrices) {
				this.matrices = matrices;
			}

			public void setVertex(SubmitNodeCollector vertex) {
				this.vertex = vertex;
			}
		}
	}

	public static class PreAll extends EventEntityRender {
	}

	public static class PostAll extends EventEntityRender {
	}
}
