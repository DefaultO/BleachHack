/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventPacket;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingToggle;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class AntiHunger extends Module {

	private boolean bool = false;

	public AntiHunger() {
		super("AntiHunger", KEY_UNBOUND, ModuleCategory.PLAYER, "Minimizes the amount of hunger you use (Also makes you slide).",
				new SettingToggle("Relaxed", false).withDesc("Only activates every other ticks, might fix getting fly kicked."));
	}

	@BleachSubscribe
	public void onSendPacket(EventPacket.Send event) {
		if (event.getPacket() instanceof ServerboundMovePlayerPacket packet) {
			if (mc.player.getDeltaMovement().y != 0 && !mc.options.keyJump.isDown() && (!bool || !getSetting(0).asToggle().getState())) {
				// if (((ServerboundMovePlayerPacket) event.getPacket()).isOnGround())
				// event.setCancelled(true);
				boolean onGround = mc.player.fallDistance >= 0.1f;
				mc.player.setOnGround(onGround);

				// 26.2: packet fields are final now - cancel and resend a rebuilt packet instead of mutating.
				// The resent packet re-fires this handler but already matches, so it passes through untouched.
				if (packet.isOnGround() != onGround) {
					event.setCancelled(true);
					mc.player.connection.send(withOnGround(packet, onGround));
				}
				bool = true;
			} else {
				bool = false;
			}
		}
	}

	private static ServerboundMovePlayerPacket withOnGround(ServerboundMovePlayerPacket p, boolean onGround) {
		if (p.hasPosition() && p.hasRotation())
			return new ServerboundMovePlayerPacket.PosRot(p.getX(0), p.getY(0), p.getZ(0), p.getYRot(0), p.getXRot(0), onGround, p.horizontalCollision());
		if (p.hasPosition())
			return new ServerboundMovePlayerPacket.Pos(p.getX(0), p.getY(0), p.getZ(0), onGround, p.horizontalCollision());
		if (p.hasRotation())
			return new ServerboundMovePlayerPacket.Rot(p.getYRot(0), p.getXRot(0), onGround, p.horizontalCollision());
		return new ServerboundMovePlayerPacket.StatusOnly(onGround, p.horizontalCollision());
	}

}
