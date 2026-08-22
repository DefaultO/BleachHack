/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.event.events;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.bleachhack.event.Event;
import org.joml.Matrix3x2fStack;

public class EventRenderScreenBackground extends Event {
	
	public GuiGraphicsExtractor context;

	public EventRenderScreenBackground(GuiGraphicsExtractor context) {
		this.context = context;
	}

	// TODO(26.2): GUI transform is now a 2D Matrix3x2fStack (was 3D PoseStack); callers using z-translate need adapting.
	public Matrix3x2fStack getMatrices() {
		return context.pose();
	}
}
