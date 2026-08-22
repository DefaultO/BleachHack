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
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.phys.Vec3;
import org.bleachhack.setting.module.SettingToggle;

public class Speed extends Module {

	private boolean jumping;

	public Speed() {
		super("Speed", KEY_UNBOUND, ModuleCategory.MOVEMENT, "Allows you to go faster, what did you expect?",
				new SettingMode("Mode", "StrafeHop", "Strafe", "OnGround", "MiniHop", "Bhop").withDesc("Speed mode."),
				new SettingSlider("Strafe", 0.15, 0.55, 0.27, 2).withDesc("Strafe speed."),
				new SettingSlider("OnGround", 0.1, 10, 2, 1).withDesc("OnGround speed."),
				new SettingSlider("MiniHop", 0.1, 10, 2, 1).withDesc("MiniHop speed."),
				new SettingSlider("Bhop", 0.1, 10, 2, 1).withDesc("Bhop speed."),
				new SettingToggle("NoInertia", false).withDesc("Prevents you from moving forcefully."));
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		//System.out.println(mc.player.zza + " | " + mc.player.xxa);
		if (mc.options.keyShift.isDown())
			return;

			/* Strafe */
		if (getSetting(0).asMode().getMode() <= 1) {
			if ((mc.player.zza != 0 || mc.player.xxa != 0) /*&& mc.player.onGround()*/) {
				if (!mc.player.isSprinting()) {
					mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
				}

				mc.player.setDeltaMovement(new Vec3(0, mc.player.getDeltaMovement().y, 0));
				mc.player.moveRelative(getSetting(1).asSlider().getValueFloat(),
						new Vec3(mc.player.xxa, 0, mc.player.zza));
				
				double vel = Math.abs(mc.player.getDeltaMovement().x) + Math.abs(mc.player.getDeltaMovement().z);
				
				if (getSetting(0).asMode().getMode() == 0 && vel >= 0.12 && mc.player.onGround()) {
					mc.player.moveRelative(vel >= 0.3 ? 0.0f : 0.15f, new Vec3(mc.player.xxa, 0, mc.player.zza));
					mc.player.jumpFromGround();
				}
			}
			
			/* OnGround */
		} else if (getSetting(0).asMode().getMode() == 2) {
			if (mc.options.keyJump.isDown() || mc.player.fallDistance > 0.25)
				return;
			
			double speeds = 0.85 + getSetting(2).asSlider().getValue() / 30;

			if (jumping && mc.player.getY() >= mc.player.yo + 0.399994D) {
				mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -0.9, mc.player.getDeltaMovement().z);
				mc.player.setPos(mc.player.getX(), mc.player.yo, mc.player.getZ());
				jumping = false;
			}

			if (mc.player.zza != 0.0F && !mc.player.horizontalCollision) {
				if (mc.player.verticalCollision) {
					mc.player.setDeltaMovement(mc.player.getDeltaMovement().x * speeds, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z * speeds);
					jumping = true;
					mc.player.jumpFromGround();
					// 1.0379
				}

				if (jumping && mc.player.getY() >= mc.player.yo + 0.399994D) {
					mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -100, mc.player.getDeltaMovement().z);
					jumping = false;
				}

			}

			/* MiniHop */
		} else if (getSetting(0).asMode().getMode() == 3) {
			if (mc.player.horizontalCollision || mc.options.keyJump.isDown() || mc.player.zza == 0)
				return;
			
			double speeds = 0.9 + getSetting(3).asSlider().getValue() / 30;
			
			if (mc.player.onGround()) {
				mc.player.jumpFromGround();
			} else if (mc.player.getDeltaMovement().y > 0) {
				mc.player.setDeltaMovement(mc.player.getDeltaMovement().x * speeds, -1, mc.player.getDeltaMovement().z * speeds);
				mc.player.xxa += 1.5F; // TODO(26.2): ClientInput no longer exposes movementSideways; bumping xxa directly instead
			}

			/* Bhop */
		} else if (getSetting(0).asMode().getMode() == 4) {
			if (mc.player.zza > 0 && mc.player.onGround()) {
				double speeds = 0.65 + getSetting(4).asSlider().getValue() / 30;
				
				mc.player.jumpFromGround();
				mc.player.setDeltaMovement(mc.player.getDeltaMovement().x * speeds, 0.255556, mc.player.getDeltaMovement().z * speeds);
				mc.player.xxa += 3.0F;
				mc.player.jumpFromGround();
				mc.player.setSprinting(true);
			}
		}
	}

	@BleachSubscribe
	public void onMove(EventClientMove event) {
		if (mc.player.zza == 0 && mc.player.xxa == 0 && getSetting(5).asToggle().getState()) {
			event.setVec(new Vec3(0, event.getVec().y, 0));
		}
	}

}
