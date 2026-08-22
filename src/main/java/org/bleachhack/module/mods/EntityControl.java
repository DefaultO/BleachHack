/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventEntityControl;
import org.bleachhack.event.events.EventPacket;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.world.WorldUtils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class EntityControl extends Module {

	public EntityControl() {
		super("EntityControl", KEY_UNBOUND, ModuleCategory.MOVEMENT, "Manipulates Entities.",
				new SettingToggle("EntitySpeed", true).withDesc("Lets you control the speed of riding entities.").withChildren(
						new SettingSlider("Speed", 0, 5, 1.2, 2).withDesc("The speed of the entity.")),
				new SettingToggle("EntityFly", false).withDesc("Lets you fly with entities.").withChildren(
						new SettingSlider("Ascend", 0, 2, 0.3, 2).withDesc("Ascend speed."),
						new SettingSlider("Descend", 0, 2, 0.5, 2).withDesc("Descend speed.")),
				new SettingToggle("HorseJump", true).withDesc("Makes your horse always do the highest jump it can."),
				new SettingToggle("GroundSnap", false).withDesc("Snaps the entity to the ground when going down blocks."),
				new SettingToggle("AntiStuck", false).withDesc("Tries to prevent rubberbanding when going up blocks."),
				new SettingToggle("NoAI", true).withDesc("Disables the entities AI."),
				new SettingToggle("RotationLock", false).withDesc("Locks the rotation of the vehicle to a certain angle serverside.").withChildren(
						new SettingSlider("Yaw", -180, 180, 0, 0).withDesc("Yaw of the vehicle."),
						new SettingSlider("Pitch", -90, 90, 0, 0).withDesc("Pitch of the vehicle."),
						new SettingToggle("Player", true).withDesc("Also locks roation for player packets.")),
				new SettingToggle("AntiDismount", false).withDesc("Prevents you from getting distmounted by the server"));
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (mc.player.getVehicle() == null)
			return;

		Entity e = mc.player.getVehicle();
		double speed = getSetting(0).asToggle().getChild(0).asSlider().getValue();

		double forward = mc.player.zza;
		double strafe = mc.player.xxa;
		float yaw = mc.player.getYRot();

		e.setYRot(yaw);
		if (e instanceof Llama) {
			((Llama) e).yHeadRot = mc.player.yHeadRot;
		}

		if (getSetting(5).asToggle().getState() && forward == 0 && strafe == 0) {
			e.setDeltaMovement(new Vec3(0, e.getDeltaMovement().y, 0));
		}

		if (getSetting(0).asToggle().getState()) {
			if (forward != 0.0D) {
				if (strafe > 0.0D) {
					yaw += (forward > 0.0D ? -45 : 45);
				} else if (strafe < 0.0D) {
					yaw += (forward > 0.0D ? 45 : -45);
				}

				if (forward > 0.0D) {
					forward = 1.0D;
				} else if (forward < 0.0D) {
					forward = -1.0D;
				}

				strafe = 0.0D;
			}

			e.setDeltaMovement(forward * speed * Math.cos(Math.toRadians(yaw + 90.0F)) + strafe * speed * Math.sin(Math.toRadians(yaw + 90.0F)),
					e.getDeltaMovement().y,
					forward * speed * Math.sin(Math.toRadians(yaw + 90.0F)) - strafe * speed * Math.cos(Math.toRadians(yaw + 90.0F)));
		}

		if (getSetting(1).asToggle().getState()) {
			if (mc.options.keyJump.isDown()) {
				e.setDeltaMovement(e.getDeltaMovement().x, getSetting(1).asToggle().getChild(0).asSlider().getValue(), e.getDeltaMovement().z);
			} else {
				e.setDeltaMovement(e.getDeltaMovement().x, -getSetting(1).asToggle().getChild(1).asSlider().getValue(), e.getDeltaMovement().z);
			}
		}

		if (getSetting(3).asToggle().getState()) {
			BlockPos p = BlockPos.containing(e.position());
			if (!mc.level.getBlockState(p.below()).canBeReplaced() && e.fallDistance > 0.01) {
				e.setDeltaMovement(e.getDeltaMovement().x, -1, e.getDeltaMovement().z);
			}
		}

		if (getSetting(4).asToggle().getState()) {
			Vec3 vel = e.getDeltaMovement().scale(2);
			if (WorldUtils.doesBoxCollide(e.getBoundingBox().move(vel.x, 0, vel.z))) {
				for (int i = 2; i < 10; i++) {
					if (!WorldUtils.doesBoxCollide(e.getBoundingBox().move(vel.x / i, 0, vel.z / i))) {
						e.setDeltaMovement(vel.x / i / 2, vel.y, vel.z / i / 2);
						break;
					}
				}
			}
		}
	}

	@BleachSubscribe
	public void onSendPacket(EventPacket.Send event) {
		if (getSetting(6).asToggle().getState()) {
			if (event.getPacket() instanceof ServerboundMoveVehiclePacket) {
				ServerboundMoveVehiclePacket packet = (ServerboundMoveVehiclePacket) event.getPacket();
				packet.yRot = getSetting(6).asToggle().getChild(0).asSlider().getValueFloat();
				packet.xRot = getSetting(6).asToggle().getChild(1).asSlider().getValueFloat();
			} else if (event.getPacket() instanceof ServerboundMovePlayerPacket
					&& mc.player.isPassenger()
					&& getSetting(6).asToggle().getChild(2).asToggle().getState()) {
				ServerboundMovePlayerPacket packet = (ServerboundMovePlayerPacket) event.getPacket();
				packet.yRot = getSetting(6).asToggle().getChild(0).asSlider().getValueFloat();
				packet.xRot = getSetting(6).asToggle().getChild(1).asSlider().getValueFloat();
			}
		}

		if (getSetting(7).asToggle().getState() && event.getPacket() instanceof ServerboundMoveVehiclePacket && mc.player.isPassenger()) {
			mc.gameMode.interact(mc.player, mc.player.getVehicle(), new EntityHitResult(mc.player.getVehicle()), InteractionHand.MAIN_HAND);
		}
	}

	@BleachSubscribe
	public void onReadPacket(EventPacket.Read event) {
		if (getSetting(7).asToggle().getState() && mc.player != null && mc.player.isPassenger() && !mc.player.input.keyPresses.shift()
				&& (event.getPacket() instanceof ClientboundPlayerPositionPacket || event.getPacket() instanceof ClientboundSetPassengersPacket)) {
			event.setCancelled(true);
		}
	}

	@BleachSubscribe
	public void onEntityControl(EventEntityControl event) {
		if (mc.player.getVehicle() instanceof ItemSteerable && mc.player.zza == 0 && mc.player.xxa == 0) {
			return;
		}

		event.setControllable(true);
	}

	// HorseJump handled in MixinClientPlayerEntity.method_3151
}
