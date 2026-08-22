/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.ChatFormatting;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class FriendManager {

	private Set<String> friends = new TreeSet<>();

	public FriendManager() {
	}

	public FriendManager(Collection<String> names) {
		friends.addAll(names);
	}
	
	public void add(Entity entity) {
		if (entity instanceof Player)
			add(entity.getName().getString());
	}

	public void add(String name) {
		name = ChatFormatting.stripFormatting(name).toLowerCase(Locale.ENGLISH);

		if (!name.isEmpty()) {
			friends.add(name);
		}
	}

	public void addAll(Collection<String> names) {
		names.forEach(this::add);
	}
	
	public void remove(Entity entity) {
		if (entity instanceof Player)
			remove(entity.getName().getString());
	}

	public void remove(String name) {
		name = ChatFormatting.stripFormatting(name).toLowerCase(Locale.ENGLISH);

		if (!name.isEmpty()) {
			friends.remove(name);
		}
	}

	public void removeAll(Collection<String> names) {
		names.forEach(this::remove);
	}
	
	public boolean has(Entity entity) {
		if (entity instanceof Player)
			return has(entity.getName().getString());
		
		return false;
	}

	public boolean has(String name) {
		name = ChatFormatting.stripFormatting(name).toLowerCase(Locale.ENGLISH);

		if (!name.isEmpty()) {
			return friends.contains(name);
		}

		return false;
	}

	public Set<String> getFriends() {
		return friends;
	}
}
