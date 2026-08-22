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
import org.bleachhack.event.events.EventSendMovementPackets;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;

import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.phys.Vec3;

public class PacketFly extends Module {

	private Vec3 cachedPos;
	private int timer = 0;

	public PacketFly() {
		super("PacketFly", KEY_UNBOUND, ModuleCategory.MOVEMENT, "Allows you to fly with packets.",
				new SettingMode("Mode", "Phase", "Packet").withDesc("Packetfly mode."),
				new SettingSlider("HSpeed", 0.05, 2, 0.5, 2).withDesc("The horizontal speed."),
				new SettingSlider("VSpeed", 0.05, 2, 0.5, 2).withDesc("The vertical speed."),
				new SettingSlider("Fall", 0, 40, 20, 0).withDesc("How often to fall (antikick)."),
				new SettingToggle("Packet Cancel", false).withDesc("Cancel rubberband packets clientside."));
	}

	@Override
	public void onEnable(boolean inWorld) {
		if (!inWorld)
			return;

		super.onEnable(inWorld);

		cachedPos = mc.player.getRootVehicle().getPos();
	}

	@BleachSubscribe
	public void onMovementPackets(EventSendMovementPackets event) {
		mc.player.setVelocity(Vec3.ZERO);
		event.setCancelled(true);
	}

	@BleachSubscribe
	public void onClientMove(EventClientMove event) {
		event.setCancelled(true);
	}

	@BleachSubscribe
	public void onReadPacket(EventPacket.Read event) {
		if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
			ClientboundPlayerPositionPacket p = (ClientboundPlayerPositionPacket) event.getPacket();

			p.yaw = mc.player.getYaw();
			p.pitch = mc.player.getPitch();

			if (getSetting(4).asToggle().getState()) {
				event.setCancelled(true);
			}
		}

	}

	@BleachSubscribe
	public void onSendPacket(EventPacket.Send event) {
		if (event.getPacket() instanceof ServerboundMovePlayerPacket.LookAndOnGround) {
			event.setCancelled(true);
			return;
		}

		if (event.getPacket() instanceof ServerboundMovePlayerPacket.Full) {
			event.setCancelled(true);
			ServerboundMovePlayerPacket p = (ServerboundMovePlayerPacket) event.getPacket();
			mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(p.getX(0), p.getY(0), p.getZ(0), p.isOnGround()));
		}
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (!mc.player.isAlive())
			return;

		double hspeed = getSetting(1).asSlider().getValue();
		double vspeed = getSetting(2).asSlider().getValue();
		timer++;

		Vec3 forward = new Vec3(0, 0, hspeed).rotateY(-(float) Math.toRadians(mc.player.getYaw()));
		Vec3 moveVec = Vec3.ZERO;

		if (mc.player.input.pressingForward) {
			moveVec = moveVec.add(forward);
		}
		if (mc.player.input.pressingBack) {
			moveVec = moveVec.add(forward.negate());
		}
		if (mc.player.input.jumping) {
			moveVec = moveVec.add(0, vspeed, 0);
		}
		if (mc.player.input.sneaking) {
			moveVec = moveVec.add(0, -vspeed, 0);
		}
		if (mc.player.input.pressingLeft) {
			moveVec = moveVec.add(forward.rotateY((float) Math.toRadians(90)));
		}
		if (mc.player.input.pressingRight) {
			moveVec = moveVec.add(forward.rotateY((float) -Math.toRadians(90)));
		}

		Entity target = mc.player.getRootVehicle();
		if (getSetting(0).asMode().getMode() == 0) {
			if (timer > getSetting(3).asSlider().getValue()) {
				moveVec = moveVec.add(0, -vspeed, 0);
				timer = 0;
			}

			cachedPos = cachedPos.add(moveVec);

			//target.noClip = true;
			target.updatePositionAndAngles(cachedPos.x, cachedPos.y, cachedPos.z, mc.player.getYaw(), mc.player.getPitch());
			if (target != mc.player) {
				mc.player.networkHandler.sendPacket(new ServerboundMoveVehiclePacket(target));
			} else {
				mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(cachedPos.x, cachedPos.y, cachedPos.z, false));
				mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(cachedPos.x, cachedPos.y - 0.01, cachedPos.z, true));
			}
		} else if (getSetting(0).asMode().getMode() == 1) {
			//moveVec = Vec3.ZERO;
			/*if (mc.player.headYaw != mc.player.yaw) {
				mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.LookOnly(
						mc.player.headYaw, mc.player.pitch, mc.player.isOnGround()));
				return;
			}*/

			/*if (mc.options.jumpKey.isPressed())
				mouseY = 0.062;
			if (mc.options.sneakKey.isPressed())
				mouseY = -0.062;*/

			if (timer > getSetting(3).asSlider().getValue()) {
				moveVec = new Vec3(0, -vspeed, 0);
				timer = 0;
			}

			mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(
					mc.player.getX() + moveVec.x, mc.player.getY() + moveVec.y, mc.player.getZ() + moveVec.z, false));

			mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(
					mc.player.getX() + moveVec.x, mc.player.getY() - 420.69, mc.player.getZ() + moveVec.z, true));
		}
	}

}
