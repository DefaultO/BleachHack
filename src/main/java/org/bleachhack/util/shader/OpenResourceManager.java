/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.shader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * A resource manager wrapper that used to serve shader resources from Fabric
 * mod namespaces and __url__-encoded URLs.
 *
 * TODO(26.2): the custom-namespace resolution (mod containers via FabricLoader
 * + URL streams, see git history) needs re-plumbing when the shader system is
 * rebuilt on ShaderManager - the Resource construction it relied on changed.
 * Until then this is a pure delegate; only the dormant ShaderLoader used it.
 */
public class OpenResourceManager implements ResourceManager {

	@SuppressWarnings("unused")
	private static final Pattern DECODE_PATTERN = Pattern.compile("_([0-9]+)_");

	private final ResourceManager parent;

	public OpenResourceManager(ResourceManager parent) {
		this.parent = parent;
	}

	@Override
	public Optional<Resource> getResource(Identifier id) {
		return parent.getResource(id);
	}

	@Override
	public Set<String> getNamespaces() {
		return parent.getNamespaces();
	}

	@Override
	public List<Resource> getResourceStack(Identifier id) {
		return parent.getResourceStack(id);
	}

	@Override
	public Map<Identifier, Resource> listResources(String directory, Predicate<Identifier> filter) {
		return parent.listResources(directory, filter);
	}

	@Override
	public Map<Identifier, List<Resource>> listResourceStacks(String directory, Predicate<Identifier> filter) {
		return parent.listResourceStacks(directory, filter);
	}

	@Override
	public Stream<PackResources> listPacks() {
		return parent.listPacks();
	}

	@SuppressWarnings("unused")
	private InputStream parseURL(String path) throws IOException {
		String decoded = DECODE_PATTERN.matcher(path).replaceAll(m -> Character.toString(Integer.parseInt(m.group(1))));
		return new URL(decoded).openStream();
	}
}
