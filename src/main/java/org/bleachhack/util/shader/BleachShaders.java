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

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * BleachHack's own post effects, injected into the ShaderManager by MixinShaderManager.
 *
 * A PostPass bakes its uniforms when it's built and there's no supported way to rewrite
 * them per frame, so the entity highlight is registered as a grid of pre-baked variants
 * (fill opacity x outline thickness) and the module picks the closest one. Configs are
 * just records - only the variants actually used ever compile a shader.
 */
public class BleachShaders {

	public static final Identifier ENTITY_OUTLINE_DILATE = Identifier.fromNamespaceAndPath("bleachhack", "post/outline_dilate_h");
	public static final Identifier ENTITY_OUTLINE_COMBINE = Identifier.fromNamespaceAndPath("bleachhack", "post/outline_combine");

	/** Fill steps, 0..FILL_STEPS maps to 0%..100% interior opacity. */
	public static final int FILL_STEPS = 10;

	/** Outline shading across its width: solid, inline (dark band inside), gradient. */
	public static final int STYLES = 3;

	/** Outline thickness in texels. */
	public static final int MIN_RADIUS = 1;
	public static final int MAX_RADIUS = 5;

	private static final Set<Identifier> REGISTERED = ConcurrentHashMap.newKeySet();

	public static Identifier variant(int fillStep, int radius, int style) {
		return Identifier.fromNamespaceAndPath("bleachhack",
				"entity_outline_f" + Mth.clamp(fillStep, 0, FILL_STEPS)
						+ "_r" + Mth.clamp(radius, MIN_RADIUS, MAX_RADIUS)
						+ "_s" + Mth.clamp(style, 0, STYLES - 1));
	}

	/** Picks the variant matching a 0-255 fill, a thickness in texels and an outline style. */
	public static Identifier variantFor(int fill255, int radius, int style) {
		int step = Math.round(Mth.clamp(fill255, 0, 255) / 255f * FILL_STEPS);
		return variant(step, radius, style);
	}

	public static void markRegistered(Identifier id) {
		REGISTERED.add(id);
	}

	/**
	 * Our chains live in the ShaderManager's config, not in the resource manager
	 * (BleachHack has no Fabric API, so its assets aren't a resource pack), so
	 * SafePostChain has to trust this instead of a resource lookup.
	 */
	public static boolean isRegistered(Identifier id) {
		return REGISTERED.contains(id);
	}
}
