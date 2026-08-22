/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.event.events;

import com.mojang.blaze3d.vertex.PoseStack;
import org.bleachhack.event.Event;

public class EventWorldRender extends Event {

	protected float partialTicks;
	protected PoseStack matrices;
	
	public static class Pre extends EventWorldRender {

		public Pre(float partialTicks, PoseStack matrices) {
			this.partialTicks = partialTicks;
			this.matrices = matrices;
		}
		
	}
	
	public static class Post extends EventWorldRender {

		public Post(float partialTicks, PoseStack matrices) {
			this.partialTicks = partialTicks;
			this.matrices = matrices;
		}
		
	}

	public float getPartialTicks() {
		return partialTicks;
	}
	
	public PoseStack getMatrices() {
		return matrices;
	}
}
