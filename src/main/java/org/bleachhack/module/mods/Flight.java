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
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class Flight extends Module {

	private boolean flyTick = false;

	public Flight() {
		super("Flight", KEY_UNBOUND, ModuleCategory.MOVEMENT, "Allows you to fly.",
				new SettingMode("Mode", "Static", "Jetpack", "ec.me").withDesc("Flight mode."),
				new SettingSlider("Speed", 0, 5, 1, 1).withDesc("Flight speed."),
				new SettingMode("AntiKick", "Off", "Fall", "Bob", "Packet").withDesc("How to bypass \"you have been kicked for flying\" kicks."));
	}

	@Override
	public void onDisable(boolean inWorld) {
		if (inWorld)
			mc.player.getAbilities().flying = false;
		
		super.onDisable(inWorld);
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		float speed = getSetting(1).asSlider().getValueFloat();

		if (mc.player.tickCount % 20 == 0 && getSetting(2).asMode().getMode() == 3 && !(getSetting(0).asMode().getMode() == 1)) {
			mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() - 0.069, mc.player.getZ(), false, false));
			mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getZ() + 0.069, mc.player.getZ(), true, false));
		}

		if (getSetting(0).asMode().getMode() == 0) {
			Vec3 antiKickVel = Vec3.ZERO;

			if (getSetting(2).asMode().getMode() == 1
					&& mc.player.tickCount % 20 == 0
					&& mc.level.getBlockState(BlockPos.containing(mc.player.position().add(0, -0.069, 0))).canBeReplaced()) {
				antiKickVel = antiKickVel.add(0, -0.069, 0);
			} else if (getSetting(2).asMode().getMode() == 2) {
				if (mc.player.tickCount % 40 == 0) {
					if (mc.level.getBlockState(BlockPos.containing(mc.player.position().add(0, 0.15, 0))).canBeReplaced()) {
						antiKickVel = antiKickVel.add(0, 0.15, 0);
					}
				} else if (mc.player.tickCount % 20 == 0) {
					if (mc.level.getBlockState(BlockPos.containing(mc.player.position().add(0, -0.15, 0))).canBeReplaced()) {
						antiKickVel = antiKickVel.add(0, -0.15, 0);
					}
				}
			}

			mc.player.setDeltaMovement(antiKickVel);

			Vec3 forward = new Vec3(0, 0, speed).yRot(-(float) Math.toRadians(mc.player.getYRot()));
			Vec3 strafe = forward.yRot((float) Math.toRadians(90));

			if (mc.options.keyJump.isDown())
				mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(0, speed, 0));
			if (mc.options.keyShift.isDown())
				mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(0, -speed, 0));
			if (mc.options.keyDown.isDown())
				mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(-forward.x, 0, -forward.z));
			if (mc.options.keyUp.isDown())
				mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(forward.x, 0, forward.z));
			if (mc.options.keyLeft.isDown())
				mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(strafe.x, 0, strafe.z));
			if (mc.options.keyRight.isDown())
				mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(-strafe.x, 0, -strafe.z));

		} else if (getSetting(0).asMode().getMode() == 1) {
			if (!mc.options.keyJump.isDown())
				return;
			mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, speed / 3, mc.player.getDeltaMovement().z);
		} else if (getSetting(0).asMode().getMode() == 2) {
			if (InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyJump.saveString()).getValue())) {
				mc.player.jumpFromGround();
			} else {
				if (InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(mc.options.keyJump.saveString()).getValue())) {
					mc.player.setPos(mc.player.getX(), mc.player.getY() - speed / 10f, mc.player.getZ());
				}
			}
		}
	}

	@BleachSubscribe
	public void onSendPacket(EventPacket.Send event) {
		if (getSetting(0).asMode().getMode() == 2 && event.getPacket() instanceof ServerboundMovePlayerPacket) {
			if (!flyTick) {
				boolean onGround = true;// mc.player.fallDistance >= 0.1f;
				mc.player.setOnGround(onGround);
				// TODO(26.2): onGround is protected final now - rebuild the packet with the spoofed flag instead
				ServerboundMovePlayerPacket p = (ServerboundMovePlayerPacket) event.getPacket();
				if (p.hasPosition() && p.hasRotation()) {
					event.setPacket(new ServerboundMovePlayerPacket.PosRot(p.getX(0), p.getY(0), p.getZ(0), p.getYRot(0), p.getXRot(0), onGround, p.horizontalCollision()));
				} else if (p.hasPosition()) {
					event.setPacket(new ServerboundMovePlayerPacket.Pos(p.getX(0), p.getY(0), p.getZ(0), onGround, p.horizontalCollision()));
				} else if (p.hasRotation()) {
					event.setPacket(new ServerboundMovePlayerPacket.Rot(p.getYRot(0), p.getXRot(0), onGround, p.horizontalCollision()));
				} else {
					event.setPacket(new ServerboundMovePlayerPacket.StatusOnly(onGround, p.horizontalCollision()));
				}

				flyTick = true;
			} else {
				flyTick = false;
			}
		}
	}
}
