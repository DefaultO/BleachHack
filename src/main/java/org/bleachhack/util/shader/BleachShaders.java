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

/** BleachHack's own post effects, injected into the ShaderManager by MixinShaderManager. */
public class BleachShaders {

	public static final Identifier ENTITY_OUTLINE = Identifier.fromNamespaceAndPath("bleachhack", "entity_outline");
	public static final Identifier ENTITY_OUTLINE_FRAGMENT = Identifier.fromNamespaceAndPath("bleachhack", "post/entity_outline");

	private static final Set<Identifier> REGISTERED = ConcurrentHashMap.newKeySet();

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
