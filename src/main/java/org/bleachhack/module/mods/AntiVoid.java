/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventClientMove;
import org.bleachhack.event.events.EventPacket;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.world.WorldUtils;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class AntiVoid extends Module {

	public AntiVoid() {
		super("AntiVoid", KEY_UNBOUND, ModuleCategory.MOVEMENT, "Prevents you from falling in the void.",
				new SettingMode("Mode", "Jump", "Floor", "Vanilla").withDesc("What mode to use when you're in the void."),
				new SettingToggle("AntiTP", true).withDesc("Prevents you from accidentally tping in to the void (i.e., using PacketFly)."));
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (mc.player.getY() < mc.level.getMinY()) {
			switch (getSetting(0).asMode().getMode()) {
				case 0:
					mc.player.jumpFromGround();
					break;
				case 1:
					mc.player.setOnGround(true);
					break;
				case 2:
					for (int i = mc.level.getMinY() + 3; i < mc.level.getMaxY() + 2; i++) {
						if (!WorldUtils.doesBoxCollide(mc.player.getBoundingBox().move(0, -mc.player.getY() + i, 0))) {
							mc.player.snapTo(mc.player.getX(), i, mc.player.getZ());
							break;
						}
					}

					break;
			}
		}
	}

	@BleachSubscribe
	public void onSendPacket(EventPacket.Send event) {
		if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
			ServerboundMovePlayerPacket packet = (ServerboundMovePlayerPacket) event.getPacket();

			if (getSetting(1).asToggle().getState()
					&& mc.player.getY() >= mc.level.getMinY() && packet.getY(mc.player.getY()) < mc.level.getMinY()) {
				event.setCancelled(true);
				return;
			}

			if (getSetting(0).asMode().getMode() == 1 && mc.player.getY() < mc.level.getMinY() && packet.getY(mc.player.getY()) < mc.player.getY()) {
				// TODO(26.2): packet fields are final now - replace the packet with a y-corrected copy instead of mutating it
				event.setCancelled(true);
				if (packet instanceof ServerboundMovePlayerPacket.PosRot) {
					mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(packet.getX(0), mc.player.getY(), packet.getZ(0),
							packet.getYRot(0), packet.getXRot(0), packet.isOnGround(), packet.horizontalCollision()));
				} else {
					mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(packet.getX(0), mc.player.getY(), packet.getZ(0),
							packet.isOnGround(), packet.horizontalCollision()));
				}
			}
		}
	}

	@BleachSubscribe
	public void onClientMove(EventClientMove event) {
		if (getSetting(1).asToggle().getState() && mc.player.getY() >= mc.level.getMinY() && mc.player.getY() - event.getVec().y < mc.level.getMinY()) {
			event.setCancelled(true);
			return;
		}

		if (getSetting(0).asMode().getMode() == 1 && mc.player.getY() < mc.level.getMinY() && event.getVec().y < 0) {
			event.setVec(new Vec3(event.getVec().x, 0, event.getVec().z));
			mc.player.push(0, -mc.player.getDeltaMovement().y, 0);
		}
	}

}
