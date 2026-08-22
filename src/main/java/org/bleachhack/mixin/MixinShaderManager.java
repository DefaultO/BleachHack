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

		String fragment = bleachhack$read("/assets/bleachhack/shaders/post/entity_outline.fsh");
		if (fragment != null) {
			sources.put(new ShaderManager.ShaderSourceKey(BleachShaders.ENTITY_OUTLINE_FRAGMENT, ShaderType.FRAGMENT), fragment);
		}

		PostChainConfig chain = bleachhack$readChain("/assets/bleachhack/post_effect/entity_outline.json");
		if (chain != null) {
			chains.put(BleachShaders.ENTITY_OUTLINE, chain);
			BleachShaders.markRegistered(BleachShaders.ENTITY_OUTLINE);
		}

		BleachLogger.logger.log(Level.INFO, "Registered BleachHack shaders (fragment: %s, chain: %s)",
				fragment != null, chain != null);

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
	private static @Nullable PostChainConfig bleachhack$readChain(String path) {
		String json = bleachhack$read(path);

		if (json == null) {
			return null;
		}

		return PostChainConfig.CODEC
				.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
				.resultOrPartial(error -> BleachLogger.logger.log(Level.WARN, "Failed parsing %s: %s", path, error))
				.orElse(null);
	}
}
