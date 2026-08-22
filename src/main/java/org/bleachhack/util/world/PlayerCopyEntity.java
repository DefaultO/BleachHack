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
import net.minecraft.client.multiplayer.ClientLevel;
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

		ClientLevel level = Minecraft.getInstance().level;

		// 26.2 stopped auto-assigning entity ids - they arrive with the spawn packet, and
		// getId() throws until one is set. A client-only copy has no packet, so claim a
		// free id from the top of the range where the server won't be handing any out.
		int id = Integer.MAX_VALUE - 1;
		while (level.getEntity(id) != null) {
			id--;
		}

		setId(id);
		level.addEntity(this);
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
