/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PlayerCopyEntity extends RemotePlayer {

	private boolean ghost;

	public PlayerCopyEntity() {
		this(Minecraft.getInstance().player);
	}

	public PlayerCopyEntity(Player player) {
		this(player, player.getX(), player.getY(), player.getZ());
	}

	public PlayerCopyEntity(Player player, double x, double y, double z) {
		super(Minecraft.getInstance().level, player.getGameProfile());

		restoreFrom(player);

		// Cache the player textures, then switch to a random uuid
		// because the world doesn't allow duplicate uuids in 1.17+
		getPlayerInfo();
		getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
		setUUID(UUID.randomUUID());
	}

	public void spawn() {
		unsetRemoved();
		Minecraft.getInstance().level.addEntity(this);
	}

	public void despawn() {
		Minecraft.getInstance().level.removeEntity(this.getId(), RemovalReason.DISCARDED);
	}

	public void setGhost(boolean ghost) {
		this.ghost = ghost;
	}

	@Override
	public boolean isInvisible() {
		return ghost ? true : super.isInvisible();
	}

	@Override
	public boolean isInvisibleTo(Player player) {
		return ghost ? false : super.isInvisibleTo(player);
	}
}
