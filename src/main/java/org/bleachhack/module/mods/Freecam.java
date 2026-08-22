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
import org.bleachhack.event.events.EventOpenScreen;
import org.bleachhack.event.events.EventPacket;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.world.PlayerCopyEntity;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class Freecam extends Module {

	private PlayerCopyEntity dummy;
	private double[] playerPos;
	private float[] playerRot;
	private Entity riding;

	private boolean prevFlying;
	private float prevFlySpeed;

	public Freecam() {
		super("Freecam", KEY_UNBOUND, ModuleCategory.PLAYER, "Its freecam, you know what it does.",
				new SettingSlider("Speed", 0, 3, 0.5, 2).withDesc("Moving speed in freecam."),
				new SettingToggle("HorseInv", true).withDesc("Opens your Horse inventory when riding a horse."));
	}

	@Override
	public void onEnable(boolean inWorld) {
		if (!inWorld)
			return;

		super.onEnable(inWorld);

		mc.smartCull = false;

		playerPos = new double[] { mc.player.getX(), mc.player.getY(), mc.player.getZ() };
		playerRot = new float[] { mc.player.getYRot(), mc.player.getXRot() };

		dummy = new PlayerCopyEntity(mc.player);

		dummy.spawn();

		if (mc.player.getVehicle() != null) {
			riding = mc.player.getVehicle();
			mc.player.getVehicle().ejectPassengers();
		}

		if (mc.player.isSprinting()) {
			mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
		}

		prevFlying = mc.player.getAbilities().flying;
		prevFlySpeed = mc.player.getAbilities().getFlyingSpeed();
	}

	@Override
	public void onDisable(boolean inWorld) {
		if (inWorld) {
			mc.smartCull = true;

			dummy.despawn();
			mc.player.noPhysics = false;
			mc.player.getAbilities().flying = prevFlying;
			mc.player.getAbilities().setFlyingSpeed(prevFlySpeed);

			mc.player.snapTo(playerPos[0], playerPos[1], playerPos[2], playerRot[0], playerRot[1]);
			mc.player.setDeltaMovement(Vec3.ZERO);

			if (riding != null && mc.level.getEntity(riding.getId()) != null) {
				mc.player.startRiding(riding);
			}
		}

		super.onDisable(inWorld);
	}

	@BleachSubscribe
	public void sendPacket(EventPacket.Send event) {
		if (event.getPacket() instanceof ServerboundPlayerCommandPacket || event.getPacket() instanceof ServerboundMovePlayerPacket) {
			event.setCancelled(true);
		}
	}

	@BleachSubscribe
	public void onOpenScreen(EventOpenScreen event) {
		if (getSetting(1).asToggle().getState() && riding instanceof AbstractHorse) {
			if (event.getScreen() instanceof InventoryScreen) {
				mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
				event.setCancelled(true);
			}
		}
	}

	@BleachSubscribe
	public void onClientMove(EventClientMove event) {
		mc.player.noPhysics = true;
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		mc.player.setOnGround(false);
		mc.player.getAbilities().setFlyingSpeed((float) (getSetting(0).asSlider().getValue() / 5));
		mc.player.getAbilities().flying = true;
		mc.player.setPose(Pose.STANDING);
	}
}
