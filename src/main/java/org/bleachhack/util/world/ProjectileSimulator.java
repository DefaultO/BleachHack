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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;

public class ProjectileSimulator {

	private static Minecraft mc = Minecraft.getInstance();

	public static Entity summonProjectile(Player thrower, boolean allowThrowables, boolean allowXp, boolean allowPotions) {
		ItemStack hand = (isThrowable(thrower.getMainHandItem().getItem(), allowThrowables, allowXp, allowPotions)
				? thrower.getMainHandItem()
				: isThrowable(thrower.getOffhandItem().getItem(), allowThrowables, allowXp, allowPotions)
				? thrower.getOffhandItem()
				: null);

		if (hand == null) {
			return null;
		}

		if (hand.getItem() instanceof ProjectileWeaponItem) {
			float charged = hand.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(hand) ? 1f
					: hand.getItem() == Items.CROSSBOW ? 0f : BowItem.getPowerForTime(thrower.getTicksUsingItem());

			if (charged > 0f) {
				Entity e = new Arrow(EntityTypes.ARROW, mc.level);
				initProjectile(e, thrower, 0f, charged * 3);
				return e;
			}
		} else if (hand.getItem() instanceof SnowballItem || hand.getItem() instanceof EggItem || hand.getItem() instanceof EnderpearlItem) {
			Entity e = new Snowball(mc.level, mc.player, hand);
			initProjectile(e, thrower, 0f, 1.5f);
			return e;
		} else if (hand.getItem() instanceof ExperienceBottleItem) {
			Entity e = new ThrownExperienceBottle(mc.level, mc.player, hand);
			initProjectile(e, thrower, -20f, 0.7f);
			return e;
		} else if (hand.getItem() instanceof ThrowablePotionItem) {
			Entity e = new ThrownSplashPotion(mc.level, mc.player, hand);
			initProjectile(e, thrower, -20f, 0.5f);
			return e;
		} else if (hand.getItem() instanceof TridentItem) {
			Entity e = new ThrownTrident(mc.level, mc.player, hand);
			initProjectile(e, thrower, 0f, 2.5f);
			return e;
		}

		return null;
	}

	public static boolean isThrowable(Item item) {
		return isThrowable(item, true, true, true);
	}

	public static boolean isThrowable(Item item, boolean allowThrowables, boolean allowXp, boolean allowPotions) {
		return item instanceof ProjectileWeaponItem
				|| (allowThrowables && (item instanceof EggItem || item instanceof SnowballItem || item instanceof EnderpearlItem))
				|| (allowXp && item instanceof ExperienceBottleItem)
				|| (allowPotions && item instanceof ThrowablePotionItem) || item instanceof TridentItem;
	}

	private static void initProjectile(Entity e, Entity thrower, float addPitch, float strength) {
		float velX = -Mth.sin(thrower.getYRot() * 0.017453292F) * Mth.cos(thrower.getXRot() * 0.017453292F);
		float velY = -Mth.sin((thrower.getXRot() + addPitch) * 0.017453292F);
		float velZ = Mth.cos(thrower.getYRot() * 0.017453292F) * Mth.cos(thrower.getXRot() * 0.017453292F);

		Vec3 velVec = new Vec3(velX, velY, velZ).normalize().scale(strength);
		e.setDeltaMovement(velVec);
		float float_3 = Mth.sqrt((float) velVec.horizontalDistanceSqr());
		e.setYRot((float) (Mth.atan2(velVec.x, velVec.z) * 57.2957763671875));
		e.setXRot((float) (Mth.atan2(velVec.y, float_3) * 57.2957763671875));
		e.yRotO = e.getYRot();
		e.xRotO = e.getXRot();

		e.setDeltaMovement(velVec.add(thrower.getDeltaMovement().x, thrower.onGround() ? 0.0D : thrower.getDeltaMovement().y, thrower.getDeltaMovement().z));
	}

	public static Triple<List<Vec3>, Entity, BlockPos> simulate(Entity e) {
		List<Vec3> vecs = new ArrayList<>();

		SimulatedProjectile spoofE = new SimulatedProjectile(e);
		for (int i = 0; i < 100; i++) {
			Vec3 vel = spoofE.velocity;
			Vec3 newVec = spoofE.getPos().add(vel);
			// EntityHitResult entityHit = ProjectileUtil.raycast(mc.player, e.getPos(),
			// newVec, e.getBoundingBox(), null, 1f);
			List<LivingEntity> entities = mc.level.getEntitiesOfClass(LivingEntity.class, spoofE.getBoundingBox().inflate(0.15),
					EntitySelector.LIVING_ENTITY_STILL_ALIVE.and(en -> en != mc.player && en != e));

			if (!entities.isEmpty()) {
				return Triple.of(vecs, entities.get(0), null);
			}

			BlockHitResult blockHit = mc.level.clip(
					new ClipContext(spoofE.getPos(), newVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
			if (blockHit.getType() != HitResult.Type.MISS) {
				vecs.add(blockHit.getLocation());
				return Triple.of(vecs, null, blockHit.getBlockPos());
			}

			float prevPitch = spoofE.pitch;
			spoofE.pitch = Mth.lerp(0.2F, spoofE.prevPitch, spoofE.pitch);
			spoofE.prevPitch = prevPitch;

			double gravity = e instanceof AbstractThrownPotion ? 0.05
					: e instanceof ThrownExperienceBottle ? 0.07 : e instanceof ThrowableProjectile ? 0.03 : 0.05000000074505806;
			spoofE.velocity = new Vec3(vel.x * 0.99, vel.y * 0.99 - gravity, vel.z * 0.99);
			spoofE.setPos(spoofE.getPos().x + spoofE.velocity.x,
					spoofE.getPos().y + spoofE.velocity.y, spoofE.getPos().z + spoofE.velocity.z);

			vecs.add(spoofE.getPos());
		}

		return Triple.of(vecs, null, null);
	}

	/**
	 * Lightweight projectile entity without having to create an entirely new entity
	 **/
	private static class SimulatedProjectile {

		public double x;
		public double y;
		public double z;

		public float pitch;
		public float prevPitch = 0f;

		public Vec3 velocity;

		private float width;
		private float height;

		public SimulatedProjectile(Entity realProjectile) {
			x = realProjectile.getX();
			y = realProjectile.getY();
			z = realProjectile.getZ();

			pitch = realProjectile.getXRot();

			velocity = realProjectile.getDeltaMovement();

			width = realProjectile.getBbWidth();
			height = realProjectile.getBbHeight();
		}

		public Vec3 getPos() {
			return new Vec3(x, y, z);
		}

		public void setPos(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public AABB getBoundingBox() {
			return new AABB(x - width / 2, y - height / 2, z - width / 2, x + width / 2, y + height / 2, z + width / 2);
		}
	}
}
