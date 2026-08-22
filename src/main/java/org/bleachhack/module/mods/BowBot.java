/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import java.util.Comparator;

import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.world.EntityUtils;
import org.bleachhack.util.world.WorldUtils;

import com.google.common.collect.Streams;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class BowBot extends Module {

	public BowBot() {
		super("BowBot", KEY_UNBOUND, ModuleCategory.COMBAT, "Automatically aims and shoots at entities.",
				new SettingToggle("Shoot", true).withDesc("Automatically shoots arrows.").withChildren(
						new SettingSlider("Charge", 0.1, 1, 0.5, 2).withDesc("How much to charge the bow before shooting.")),
				new SettingToggle("Aim", false).withDesc("Automatically aims.").withChildren(
						new SettingToggle("Players", true).withDesc("Aims at players."),
						new SettingToggle("Mobs", false).withDesc("Aims at mobs."),
						new SettingToggle("Animals", false).withDesc("Aims at animals."),
						new SettingToggle("Raycast", true).withDesc("Doesn't aim at entites you can't see.")));
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (!(mc.player.getMainHandItem().getItem() instanceof ProjectileWeaponItem) || !mc.player.isUsingItem())
			return;

		if (getSetting(0).asToggle().getState()) {
			if (mc.player.getMainHandItem().getItem() == Items.CROSSBOW
					&& (float) mc.player.getTicksUsingItem() / (float) CrossbowItem.getChargeDuration(mc.player.getMainHandItem(), mc.player) >= 1f) {
				mc.player.releaseUsingItem();
				mc.player.connection.send(new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.UP));
				mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
			} else if (mc.player.getMainHandItem().getItem() == Items.BOW
					&& BowItem.getPowerForTime(mc.player.getTicksUsingItem()) >= getSetting(0).asToggle().getChild(0).asSlider().getValueFloat()) {
				mc.player.releaseUsingItem();
				mc.player.connection.send(new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.UP));
			}
		}

		// Credit: https://github.com/Wurst-Imperium/Wurst7/blob/master/src/main/java/net/wurstclient/hacks/BowAimbotHack.java
		SettingToggle aimToggle = getSetting(1).asToggle();
		if (aimToggle.getState()) {
			LivingEntity target = Streams.stream(mc.level.entitiesForRendering())
					.filter(e -> EntityUtils.isAttackable(e, true)
							&& (!aimToggle.getChild(3).asToggle().getState() || mc.player.hasLineOfSight(e)))
					.filter(e -> (aimToggle.getChild(0).asToggle().getState() && EntityUtils.isPlayer(e))
							|| (aimToggle.getChild(1).asToggle().getState() && EntityUtils.isMob(e))
							|| (aimToggle.getChild(2).asToggle().getState() && EntityUtils.isAnimal(e)))
					.sorted(Comparator.comparing(mc.player::distanceTo))
					.map(e -> (LivingEntity) e)
					.findFirst().orElse(null);

			if (target == null)
				return;

			// set velocity
			float velocity = (72000 - mc.player.getUseItemRemainingTicks()) / 20F;
			velocity = Math.min(1f, (velocity * velocity + velocity * 2) / 3);

			// set position to aim at
			Vec3 newTargetVec = target.position().add(target.getDeltaMovement());
			double d = mc.player.getEyePosition().distanceTo(target.getBoundingBox().move(target.getDeltaMovement()).getCenter());
			double x = newTargetVec.x + (newTargetVec.x - target.getX()) * d - mc.player.getX();
			double y = newTargetVec.y + (newTargetVec.y - target.getY()) * d + target.getBbHeight() * 0.5 - mc.player.getY() - mc.player.getEyeHeight(mc.player.getPose());
			double z = newTargetVec.z + (newTargetVec.z - target.getZ()) * d - mc.player.getZ();

			// set yaw
			mc.player.setYRot((float) Math.toDegrees(Math.atan2(z, x)) - 90);

			// calculate needed pitch
			double hDistance = Math.sqrt(x * x + z * z);
			double hDistanceSq = hDistance * hDistance;
			float g = 0.006F;
			float velocitySq = velocity * velocity;
			float velocityPow4 = velocitySq * velocitySq;
			float neededPitch = (float) -Math.toDegrees(Math.atan((velocitySq - Math
					.sqrt(velocityPow4 - g * (g * hDistanceSq + 2 * y * velocitySq)))
					/ (g * hDistance)));

			// set pitch
			if (Float.isNaN(neededPitch)) {
				WorldUtils.facePos(target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ());
			} else {
				mc.player.setXRot(neededPitch);
			}
		}
	}
}
