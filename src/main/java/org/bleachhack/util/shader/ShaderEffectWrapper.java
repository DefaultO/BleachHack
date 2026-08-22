/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import org.jspecify.annotations.Nullable;

/**
 * TODO(26.2): post-processing chains are data-driven now (ShaderManager +
 * PostChainConfig); the old resource-constructed PostChain with named
 * framebuffers is gone. Inert wrapper so dependents compile; ShaderRender
 * and shader-based outlines do nothing until rebuilt.
 */
public class ShaderEffectWrapper {

	private PostChain shader;

	public ShaderEffectWrapper(PostChain effect) {
		this.shader = effect;
	}

	public void prepare() {
	}

	public void render() {
	}

	public @Nullable RenderTarget getFramebuffer(String framebuffer) {
		return null;
	}

	public void clearFramebuffer(String framebuffer) {
	}

	public void drawFramebufferToMain(String framebuffer) {
	}

	public PostChain getShader() {
		return shader;
	}

	public void setShader(PostChain shader) {
		this.shader = shader;
	}
}
