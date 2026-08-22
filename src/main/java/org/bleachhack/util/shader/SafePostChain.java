/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.shader;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.Level;
import org.bleachhack.util.BleachLogger;
import org.jspecify.annotations.Nullable;

/**
 * Guarded access to 26.2's post-effect chains.
 *
 * ShaderManager.getPostChain() looks like it returns null for an unknown id, but it
 * first hands the failure to the resource-pack recovery handler, which crashes the
 * game outright. Asking for any post effect that isn't installed is therefore fatal -
 * that killed both ShaderRender (vanilla dropped most of the old 1.7 effects) and
 * ESP's custom chain.
 *
 * So check the resource is actually present before letting vanilla load it, and
 * remember the misses so a per-frame caller doesn't re-check or spam the log.
 */
public class SafePostChain {

	private static final Set<Identifier> MISSING = ConcurrentHashMap.newKeySet();

	public static @Nullable PostChain get(Identifier id, Set<Identifier> allowedTargets) {
		if (MISSING.contains(id)) {
			return null;
		}

		Minecraft mc = Minecraft.getInstance();
		Identifier file = id.withPath("post_effect/" + id.getPath() + ".json");

		if (mc.getResourceManager().getResource(file).isEmpty()) {
			MISSING.add(id);
			BleachLogger.logger.log(Level.WARN, "Post effect not installed, skipping: %s (looked for %s)", id, file);
			return null;
		}

		return mc.getShaderManager().getPostChain(id, allowedTargets);
	}

	/** Forget cached misses, e.g. after a resource reload adds packs. */
	public static void clearCache() {
		MISSING.clear();
	}
}
