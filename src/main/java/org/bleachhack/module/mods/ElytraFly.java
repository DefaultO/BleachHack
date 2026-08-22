/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.apache.commons.lang3.RandomUtils;
import org.bleachhack.event.events.EventClientMove;
import org.bleachhack.event.events.EventPacket;
import org.bleachhack.event.events.EventSendMovementPackets;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;

import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

public class ElytraFly extends Module {

	public ElytraFly() {
		super("ElytraFly", KEY_UNBOUND, ModuleCategory.MOVEMENT, "Improves the elytra.",
				new SettingMode("Mode", "AutoBoost", "Boost", "Control", "BruhFly", "Pak\u00e8tFly").withDesc("Elytrafly mode."),
				new SettingSlider("Boost", 0, 0.15, 0.05, 2).withDesc("Boost speed."),
				new SettingSlider("MaxBoost", 0, 5, 2.5, 1).withDesc("Max boost speed."),
				new SettingSlider("Speed", 0, 5, 0.8, 2).withDesc("Speed for all the other modes."),
				new SettingSlider("Packets", 1, 10, 2, 0).withDesc("How many packets to send in packet mode."));
	}

	@BleachSubscribe
	public void onClientMove(EventClientMove event) {
		/* Cancel the retarded auto elytra movement */
		if (getSetting(0).asMode().getMode() == 2 && mc.player.isFallFlying()) {
			if (!mc.options.keyJump.isDown() && !mc.options.keyShift.isDown()) {
				event.setVec(new Vec3(event.getVec().x, 0, event.getVec().z));
			}

			if (!mc.options.keyDown.isDown() && !mc.options.keyLeft.isDown()
					&& !mc.options.keyRight.isDown() && !mc.options.keyUp.isDown()) {
				event.setVec(new Vec3(0, event.getVec().y, 0));
			}
		}
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		Vec3 vec3d = new Vec3(0, 0, getSetting(3).asSlider().getValue())
				.yRot(-(float) Math.toRadians(mc.player.getYRot()));

		double currentVel = Math.abs(mc.player.getDeltaMovement().x) + Math.abs(mc.player.getDeltaMovement().y) + Math.abs(mc.player.getDeltaMovement().z);
		float radianYaw = (float) Math.toRadians(mc.player.getYRot());
		float boost = getSetting(1).asSlider().getValueFloat();

		switch (getSetting(0).asMode().getMode()) {
			case 0:
				if (mc.player.isFallFlying() && currentVel <= getSetting(2).asSlider().getValue()) {
					if (mc.options.keyDown.isDown()) {
						mc.player.push(Mth.sin(radianYaw) * boost, 0, Mth.cos(radianYaw) * -boost);
					} else if (mc.player.getXRot() > 0) {
						mc.player.push(Mth.sin(radianYaw) * -boost, 0, Mth.cos(radianYaw) * boost);
					}
				}

				break;
			case 1:
				if (mc.player.isFallFlying() && currentVel <= getSetting(2).asSlider().getValue()) {
					if (mc.options.keyUp.isDown()) {
						mc.player.push(Mth.sin(radianYaw) * -boost, 0, Mth.cos(radianYaw) * boost);
					} else if (mc.options.keyDown.isDown()) {
						mc.player.push(Mth.sin(radianYaw) * boost, 0, Mth.cos(radianYaw) * -boost);
					}
				}

				break;
			case 2:
				if (mc.player.isFallFlying()) {
					if (mc.options.keyDown.isDown()) vec3d = vec3d.reverse();
					if (mc.options.keyLeft.isDown()) vec3d = vec3d.yRot((float) Math.toRadians(90));
					else if (mc.options.keyRight.isDown()) vec3d = vec3d.yRot(-(float) Math.toRadians(90));
					if (mc.options.keyJump.isDown()) vec3d = vec3d.add(0, getSetting(3).asSlider().getValue(), 0);
					if (mc.options.keyShift.isDown()) vec3d = vec3d.add(0, -getSetting(3).asSlider().getValue(), 0);

					mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
							mc.player.getX() + vec3d.x, mc.player.getY() - 0.01, mc.player.getZ() + vec3d.z, false, false));

					mc.player.setDeltaMovement(vec3d.x, vec3d.y, vec3d.z);
				}

				break;
			case 3:
				if (shouldPacketFly()) {
					mc.player.setDeltaMovement(vec3d);
					mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
					mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
							mc.player.getX() + vec3d.x, mc.player.getY() + vec3d.y, mc.player.getZ() + vec3d.z, true, false));
				}

				break;
			case 4:
				if (shouldPacketFly()) {
					mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
					double randMult = RandomUtils.nextDouble(0.9, 1.1);

					mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
							mc.player.getX() + vec3d.x * randMult,
							mc.player.getY(),
							mc.player.getZ() + vec3d.z * randMult,
							false, false));

					for (int i = 0; i < 6; i++) {
						mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
								mc.player.getX() + vec3d.x * (randMult + i),
								mc.player.getY() - 0.0001,
								mc.player.getZ() + vec3d.z * (randMult + i),
								true, false));
					}
				}
		}
	}

	// Packet moment

	@BleachSubscribe
	public void onMovement(EventSendMovementPackets event) {
		if (getSetting(0).asMode().getMode() == 4 && shouldPacketFly()) {
			mc.player.setDeltaMovement(Vec3.ZERO);
			event.setCancelled(true);
		}
	}

	@BleachSubscribe
	public void onMovement(EventClientMove event) {
		if (getSetting(0).asMode().getMode() == 4 && shouldPacketFly()) {
			event.setCancelled(true);
		}
	}

	@BleachSubscribe
	public void onReadPacket(EventPacket.Read event) {
		if (getSetting(0).asMode().getMode() == 4 && shouldPacketFly() && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
			ClientboundPlayerPositionPacket p = (ClientboundPlayerPositionPacket) event.getPacket();

			// TODO(26.2): the packet is an immutable record now, so we swap it via setPacket instead of
			// mutating yaw/pitch in place - only takes effect if MixinClientConnection honors setPacket.
			event.setPacket(new ClientboundPlayerPositionPacket(p.id(),
					p.change().withRotation(mc.player.getYRot(), mc.player.getXRot()), p.relatives()));
		}
	}

	@BleachSubscribe
	public void onSendPacket(EventPacket.Send event) {
		if (getSetting(0).asMode().getMode() == 4 && shouldPacketFly()) {
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
	}

	private boolean shouldPacketFly() {
		return !mc.player.onGround()
				&& !mc.options.keyShift.isDown()
				&& mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
	}
}
