/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class Nofall extends Module {

	public Nofall() {
		super("Nofall", KEY_UNBOUND, ModuleCategory.PLAYER, "Prevents you from taking fall damage.",
				new SettingMode("Mode", "Simple", "Packet").withDesc("What Nofall mode to use."));
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (mc.player.fallDistance > 2.5f && getSetting(0).asMode().getMode() == 0) {
			if (mc.player.isFallFlying())
				return;
			mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
		}

		if (mc.player.fallDistance > 2.5f && getSetting(0).asMode().getMode() == 1 &&
				mc.level.getBlockState(mc.player.blockPosition().offset(
						0, (int) (-1.5 + (mc.player.getDeltaMovement().y * 0.1)), 0)).getBlock() != Blocks.AIR) {
			mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(false, mc.player.horizontalCollision));
			mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
					mc.player.getX(), mc.player.getY() - 420.69, mc.player.getZ(), true, mc.player.horizontalCollision));
			mc.player.fallDistance = 0;
		}
	}
}
