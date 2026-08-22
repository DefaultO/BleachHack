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
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.util.PlayerInteractEntityC2SUtils;
import org.bleachhack.util.PlayerInteractEntityC2SUtils.InteractType;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Mode;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * @author sl, Bleach
 */
public class Criticals extends Module {

	public Criticals() {
		super("Criticals", KEY_UNBOUND, ModuleCategory.COMBAT, "Attempts to force Critical hits on entities you hit.",
				new SettingMode("Mode", "MiniJump", "FullJump").withDesc("Criticals mode, MiniJump does the smallest posible jump, FullJump simulates a full jump."));
	}

	@BleachSubscribe
	public void sendPacket(EventPacket.Send event) {
		if (event.getPacket() instanceof ServerboundInteractPacket) {
			ServerboundInteractPacket packet = (ServerboundInteractPacket) event.getPacket();
			if (PlayerInteractEntityC2SUtils.getInteractType(packet) == InteractType.ATTACK
					&& PlayerInteractEntityC2SUtils.getEntity(packet) instanceof LivingEntity) {
				sendCritPackets();
			}
		}
	}

	private void sendCritPackets() {
		if (mc.player.isClimbing() || mc.player.isTouchingWater()
				|| mc.player.hasStatusEffect(MobEffects.BLINDNESS) || mc.player.hasVehicle()) {
			return;
		}

		boolean sprinting = mc.player.isSprinting();
		if (sprinting) {
			mc.player.setSprinting(false);
			mc.player.networkHandler.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Mode.STOP_SPRINTING));
		}

		if (mc.player.isOnGround()) {
			double x = mc.player.getX();
			double y = mc.player.getY();
			double z = mc.player.getZ();
			if (getSetting(0).asMode().getMode() == 0) {
				mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(x, y + 0.0633, z, false));
				mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(x, y, z, false));
			} else {
				mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(x, y + 0.42, z, false));
				mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(x, y + 0.65, z, false));
				mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(x, y + 0.72, z, false));
				mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(x, y + 0.53, z, false));
				mc.player.networkHandler.sendPacket(new ServerboundMovePlayerPacket.PositionAndOnGround(x, y + 0.32, z, false));
			}
		}

		if (sprinting) {
			mc.player.setSprinting(true);
			mc.player.networkHandler.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Mode.START_SPRINTING));
		}
	}
}
