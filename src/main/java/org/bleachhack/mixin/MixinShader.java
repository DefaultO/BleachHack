/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import com.mojang.blaze3d.opengl.GlProgram;
import org.spongepowered.asm.mixin.Mixin;

// Tweaks to the shader class to make it compatible with OpenResourceManager
//
// TODO(26.2): the old JsonEffectShaderProgram (yarn "Shader") no longer exists. GlProgram has no `name`
// field, no Identifier-based constructor and no loadShader(ResourceProvider, ShaderStage.Type, String)
// - shader programs are now compiled data-driven through ShaderManager/PostChainConfig, which loads
// namespaced shader Identifiers natively, so the namespace-rewriting hack below likely has no 26.2
// equivalent (and may be obsolete). Injectors commented out to keep the class compiling; needs a
// coordinated rework together with OpenResourceManager/ShaderRender.
@Mixin(value = GlProgram.class, priority = 1100)
public class MixinShader {

	// TODO(26.2): GlProgram has no `name` field (only debugLabel).
	// @Shadow @Final private String name;

	// TODO(26.2): GlProgram's constructor no longer builds an Identifier from a String.
	// @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/Identifier;<init>(Ljava/lang/String;)V"), allow = 1)
	// private String modifyProgramId(String id) {
	// 	return replaceIdentifier(id, name);
	// }

	// TODO(26.2): loadShader/ShaderStage no longer exist (see GlShaderModule/ShaderManager).
	// @ModifyVariable(method = "loadShader", at = @At("STORE"), ordinal = 1)
	// private static String modifyStageId(String id, ResourceProvider factory, ShaderStage.Type type, String name) {
	// 	return replaceIdentifier(id, name);
	// }

	@SuppressWarnings("unused")
	private static String replaceIdentifier(String id, String name) {
		String[] split = name.split(":");
		if (split.length > 1 && id.indexOf('/') < id.indexOf(':')) {
			if ("__url__".equals(split[0]))
				return name;

			return split[0] + ":" + id.replace(name, split[1]);
		}

		return id;
	}
}
