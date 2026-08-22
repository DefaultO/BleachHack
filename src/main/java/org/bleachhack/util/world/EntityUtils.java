/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.world;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import org.bleachhack.BleachHack;

public class EntityUtils {

	public static boolean isAnimal(Entity e) {
		return e instanceof AgeableMob
				|| e instanceof AmbientCreature
				|| e instanceof WaterAnimal
				|| e instanceof IronGolem
				|| e instanceof SnowGolem;
	}

	public static boolean isMob(Entity e) {
		return e instanceof Enemy;
	}

	public static boolean isPlayer(Entity e) {
		return e instanceof Player;
	}

	public static boolean isOtherServerPlayer(Entity e) {
		return e instanceof Player
				&& e != Minecraft.getInstance().player
				&& !(e instanceof PlayerCopyEntity);
	}

	public static boolean isAttackable(Entity e, boolean ignoreFriends) {
		return (e instanceof LivingEntity || e instanceof ShulkerBullet || e instanceof Fireball)
				&& e.isAlive()
				&& e != Minecraft.getInstance().player
				&& !e.isPassengerOfSameVehicle(Minecraft.getInstance().player)
				&& !(e instanceof PlayerCopyEntity)
				&& (!ignoreFriends || !BleachHack.friendMang.has(e));
	}
}
