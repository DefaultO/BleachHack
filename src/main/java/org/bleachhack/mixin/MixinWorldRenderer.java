/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import java.util.Set;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.ESP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.jspecify.annotations.Nullable;

/**
 * TODO(26.2): the other old WorldRenderer hooks still need re-homing in the
 * extract/submit render architecture (see git history on the 1.20.4 branch):
 * EventEntityRender.Single.Pre per-entity redirect, PreAll/PostAll phases,
 * EventRenderBlockOutline, EventSkyRender.Color.EndSkyColor.
 */
@Mixin(LevelRenderer.class)
public class MixinWorldRenderer {

	@Unique
	private static final Identifier BLEACHHACK_OUTLINE = Identifier.fromNamespaceAndPath("bleachhack", "entity_outline");

	/**
	 * Swap vanilla's entity-outline post chain (edge-only glow) for BleachHack's own
	 * (solid rim + translucent fill) while ESP is in Shader mode. Falls back to
	 * vanilla if our chain fails to load, so a broken shader can't black-box the game.
	 */
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderManager;getPostChain(Lnet/minecraft/resources/Identifier;Ljava/util/Set;)Lnet/minecraft/client/renderer/PostChain;"))
	private @Nullable PostChain render_getPostChain(ShaderManager shaderManager, Identifier id, Set<Identifier> allowedTargets) {
		if (ESP.isShaderModeActive()) {
			PostChain custom = shaderManager.getPostChain(BLEACHHACK_OUTLINE, allowedTargets);

			if (custom != null) {
				return custom;
			}
		}

		return shaderManager.getPostChain(id, allowedTargets);
	}
}
