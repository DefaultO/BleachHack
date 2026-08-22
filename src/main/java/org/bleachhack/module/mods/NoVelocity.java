/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import java.util.Optional;

import org.bleachhack.event.events.EventPlayerPushed;
import org.bleachhack.event.events.EventPacket;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;

/**
 * @author sl First Module utilizing EventBus!
 */
public class NoVelocity extends Module {

	public NoVelocity() {
		super("NoVelocity", KEY_UNBOUND, ModuleCategory.PLAYER, "If you take some damage, you don't move.",
				new SettingToggle("Knockback", true).withDesc("Reduces knockback from other entities.").withChildren(
						new SettingSlider("VelXZ", 0, 100, 0, 1).withDesc("How much horizontal velocity to keep."),
						new SettingSlider("VelY", 0, 100, 0, 1).withDesc("How much vertical velocity  to keep.")),
				new SettingToggle("Explosions", true).withDesc("Reduces explosion velocity.").withChildren(
						new SettingSlider("VelXZ", 0, 100, 0, 1).withDesc("How much horizontal velocity to keep."),
						new SettingSlider("VelY", 0, 100, 0, 1).withDesc("How much vertical velocity to keep.")),
				new SettingToggle("Pushing", true).withDesc("Reduces how much you get pushed by entitie.s").withChildren(
						new SettingSlider("Amount", 0, 100, 0, 1).withDesc("How much pushing to keep.")),
				new SettingToggle("Fluids", true).withDesc("Reduces how much you get pushed from fluids."));
	}

	@BleachSubscribe
	public void onPlayerPushed(EventPlayerPushed event) {
		if (getSetting(2).asToggle().getState()) {
			double amount = getSetting(2).asToggle().getChild(0).asSlider().getValue() / 100d;
			event.setPushX(event.getPushX() * amount);
			event.setPushY(event.getPushY() * amount);
			event.setPushZ(event.getPushZ() * amount);
		}
	}

	@BleachSubscribe
	public void readPacket(EventPacket.Read event) {
		if (mc.player == null)
			return;

		// 26.2: both packets are immutable records now, so replace the packet instead of mutating fields.
		if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && getSetting(0).asToggle().getState()) {
			if (packet.id() == mc.player.getId()) {
				double velXZ = getSetting(0).asToggle().getChild(0).asSlider().getValue() / 100;
				double velY = getSetting(0).asToggle().getChild(1).asSlider().getValue() / 100;

				Vec3 playerVel = mc.player.getDeltaMovement();
				Vec3 packetVel = packet.movement();

				event.setPacket(new ClientboundSetEntityMotionPacket(packet.id(), new Vec3(
						playerVel.x + (packetVel.x - playerVel.x) * velXZ,
						playerVel.y + (packetVel.y - playerVel.y) * velY,
						playerVel.z + (packetVel.z - playerVel.z) * velXZ)));
			}
		} else if (event.getPacket() instanceof ClientboundExplodePacket packet && getSetting(1).asToggle().getState()) {
			if (packet.playerKnockback().isPresent()) {
				double velXZ = getSetting(1).asToggle().getChild(0).asSlider().getValue() / 100;
				double velY = getSetting(1).asToggle().getChild(1).asSlider().getValue() / 100;

				Vec3 kb = packet.playerKnockback().get();
				event.setPacket(new ClientboundExplodePacket(packet.center(), packet.radius(), packet.blockCount(),
						Optional.of(new Vec3(kb.x * velXZ, kb.y * velY, kb.z * velXZ)),
						packet.explosionParticle(), packet.explosionSound(), packet.blockParticles()));
			}
		}
	}

	// Fluid handling in MixinFlowableFluid.getVelocity_hasNext()
}
