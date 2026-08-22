/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.Level;
import org.bleachhack.BleachHack;
import org.bleachhack.util.BleachLogger;
import org.bleachhack.util.shader.BleachShaders;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes BleachHack's own shaders visible to the game.
 *
 * Mounting a mod's assets/ as a resource pack is a Fabric API feature, and BleachHack
 * ships standalone - so the ShaderManager scans "vanilla" only and never sees
 * assets/bleachhack/**. (The pre-26.2 client worked around the same gap with its
 * OpenResourceManager wrapper.)
 *
 * Rather than take on Fabric API for one folder, read our shader files straight off the
 * mod's classpath and append them to the config the ShaderManager just built.
 */
@Mixin(ShaderManager.class)
public class MixinShaderManager {

	@Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/client/renderer/ShaderManager$Configs;", at = @At("RETURN"), cancellable = true)
	private void prepare(ResourceManager manager, ProfilerFiller profiler, CallbackInfoReturnable<ShaderManager.Configs> callback) {
		ShaderManager.Configs configs = callback.getReturnValue();

		if (configs == null) {
			return;
		}

		Map<ShaderManager.ShaderSourceKey, String> sources = new HashMap<>(configs.shaderSources());
		Map<Identifier, PostChainConfig> chains = new HashMap<>(configs.postChains());

		int shaders = 0;
		shaders += addShader(sources, BleachShaders.ENTITY_OUTLINE_DILATE, "/assets/bleachhack/shaders/post/outline_dilate_h.fsh");
		shaders += addShader(sources, BleachShaders.ENTITY_OUTLINE_COMBINE, "/assets/bleachhack/shaders/post/outline_combine.fsh");

		// One template, baked into a grid of variants: PostPass uniforms are fixed once
		// the pass is built, so each fill/thickness combination needs its own config.
		int variants = 0;
		String template = bleachhack$read("/assets/bleachhack/post_effect/entity_outline.json");

		if (template != null) {
			for (int fill = 0; fill <= BleachShaders.FILL_STEPS; fill++) {
				for (int radius = BleachShaders.MIN_RADIUS; radius <= BleachShaders.MAX_RADIUS; radius++) {
					String json = template
							.replace("${FILL}", String.valueOf((float) fill / BleachShaders.FILL_STEPS))
							.replace("${RADIUS}", String.valueOf((float) radius));

					PostChainConfig config = bleachhack$parseChain(json);

					if (config != null) {
						Identifier id = BleachShaders.variant(fill, radius);
						chains.put(id, config);
						BleachShaders.markRegistered(id);
						variants++;
					}
				}
			}
		}

		BleachLogger.logger.log(Level.INFO, "Registered BleachHack shaders (%d sources, %d post-effect variants)", shaders, variants);

		callback.setReturnValue(new ShaderManager.Configs(Map.copyOf(sources), Map.copyOf(chains)));
	}

	@Unique
	private static @Nullable String bleachhack$read(String path) {
		try (InputStream stream = BleachHack.class.getResourceAsStream(path)) {
			if (stream == null) {
				BleachLogger.logger.log(Level.WARN, "BleachHack shader resource missing: %s", path);
				return null;
			}

			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			BleachLogger.logger.log(Level.WARN, "Failed reading BleachHack shader resource %s: %s", path, e);
			return null;
		}
	}

	@Unique
	private static int addShader(Map<ShaderManager.ShaderSourceKey, String> sources, Identifier id, String path) {
		String source = bleachhack$read(path);

		if (source == null) {
			return 0;
		}

		sources.put(new ShaderManager.ShaderSourceKey(id, ShaderType.FRAGMENT), source);
		return 1;
	}

	@Unique
	private static @Nullable PostChainConfig bleachhack$parseChain(String json) {
		return PostChainConfig.CODEC
				.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
				.resultOrPartial(error -> BleachLogger.logger.log(Level.WARN, "Failed parsing BleachHack post effect: %s", error))
				.orElse(null);
	}
}
