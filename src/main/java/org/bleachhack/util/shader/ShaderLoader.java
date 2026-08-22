/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.shader;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * TODO(26.2): GlProgram/PostChain lost their resource-constructed forms;
 * shaders are data-driven via ShaderManager/PostChainConfig now. Loading
 * custom shaders from mod resources (the MixinJsonEffectShaderProgram +
 * OpenResourceManager trick) needs a full rebuild. Inert until then.
 */
public class ShaderLoader {

	public static @Nullable GlProgram load(VertexFormat format, Identifier id) {
		return null;
	}

	public static @Nullable PostChain loadEffect(RenderTarget framebuffer, Identifier id) {
		return null;
	}
}
