/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.shader;

import java.util.function.Supplier;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * TODO(26.2): the fixed-color re-rendering trick (Chams-style tinting into a
 * shader framebuffer) needs a rebuild on the submit pipeline. Providers now
 * pass entities through untinted so ESP/BlockHighlight shader modes render
 * normally instead of colored.
 */
public class ColorVertexConsumerProvider {

	@SuppressWarnings("unused")
	private RenderTarget framebuffer;
	@SuppressWarnings("unused")
	private final Supplier<GlProgram> shader;

	public ColorVertexConsumerProvider(RenderTarget framebuffer, Supplier<GlProgram> shader) {
		this.framebuffer = framebuffer;
		this.shader = shader;
	}

	public SubmitNodeCollector createDualProvider(SubmitNodeCollector parent, int red, int green, int blue, int alpha) {
		return parent;
	}

	public SubmitNodeCollector createSingleProvider(SubmitNodeCollector parent, int red, int green, int blue, int alpha) {
		return parent;
	}

	public void setFramebuffer(RenderTarget framebuffer) {
		this.framebuffer = framebuffer;
	}

	public void draw() {
	}
}
