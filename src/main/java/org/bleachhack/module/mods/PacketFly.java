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

import java.util.HashSet;
import java.util.Set;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
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

		cachedPos = mc.player.getRootVehicle().position();
	}

	@BleachSubscribe
	public void onMovementPackets(EventSendMovementPackets event) {
		mc.player.setDeltaMovement(Vec3.ZERO);
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

			// TODO(26.2): the packet is an immutable record now, so replace it instead of mutating yaw/pitch.
			// NOTE: this only takes effect once MixinClientConnection processes event.getPacket() after posting
			// the event (it currently only honors isCancelled()).
			PositionMoveRotation change = p.change();
			Set<Relative> relatives = new HashSet<>(p.relatives());
			relatives.removeAll(Relative.ROTATION);
			event.setPacket(new ClientboundPlayerPositionPacket(p.id(),
					new PositionMoveRotation(change.position(), change.deltaMovement(), mc.player.getYRot(), mc.player.getXRot()), relatives));

			if (getSetting(4).asToggle().getState()) {
				event.setCancelled(true);
			}
		}

	}

	@BleachSubscribe
	public void onSendPacket(EventPacket.Send event) {
		if (event.getPacket() instanceof ServerboundMovePlayerPacket.Rot) {
			event.setCancelled(true);
			return;
		}

		if (event.getPacket() instanceof ServerboundMovePlayerPacket.PosRot) {
			event.setCancelled(true);
			ServerboundMovePlayerPacket p = (ServerboundMovePlayerPacket) event.getPacket();
			mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(p.getX(0), p.getY(0), p.getZ(0), p.isOnGround(), p.horizontalCollision()));
		}
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (!mc.player.isAlive())
			return;

		double hspeed = getSetting(1).asSlider().getValue();
		double vspeed = getSetting(2).asSlider().getValue();
		timer++;

		Vec3 forward = new Vec3(0, 0, hspeed).yRot(-(float) Math.toRadians(mc.player.getYRot()));
		Vec3 moveVec = Vec3.ZERO;

		if (mc.player.input.keyPresses.forward()) {
			moveVec = moveVec.add(forward);
		}
		if (mc.player.input.keyPresses.backward()) {
			moveVec = moveVec.add(forward.reverse());
		}
		if (mc.player.input.keyPresses.jump()) {
			moveVec = moveVec.add(0, vspeed, 0);
		}
		if (mc.player.input.keyPresses.shift()) {
			moveVec = moveVec.add(0, -vspeed, 0);
		}
		if (mc.player.input.keyPresses.left()) {
			moveVec = moveVec.add(forward.yRot((float) Math.toRadians(90)));
		}
		if (mc.player.input.keyPresses.right()) {
			moveVec = moveVec.add(forward.yRot((float) -Math.toRadians(90)));
		}

		Entity target = mc.player.getRootVehicle();
		if (getSetting(0).asMode().getMode() == 0) {
			if (timer > getSetting(3).asSlider().getValue()) {
				moveVec = moveVec.add(0, -vspeed, 0);
				timer = 0;
			}

			cachedPos = cachedPos.add(moveVec);

			//target.noClip = true;
			target.snapTo(cachedPos.x, cachedPos.y, cachedPos.z, mc.player.getYRot(), mc.player.getXRot());
			if (target != mc.player) {
				mc.player.connection.send(ServerboundMoveVehiclePacket.fromEntity(target));
			} else {
				mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(cachedPos.x, cachedPos.y, cachedPos.z, false, false));
				mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(cachedPos.x, cachedPos.y - 0.01, cachedPos.z, true, false));
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

			mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
					mc.player.getX() + moveVec.x, mc.player.getY() + moveVec.y, mc.player.getZ() + moveVec.z, false, false));

			mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
					mc.player.getX() + moveVec.x, mc.player.getY() - 420.69, mc.player.getZ() + moveVec.z, true, false));
		}
	}

}
