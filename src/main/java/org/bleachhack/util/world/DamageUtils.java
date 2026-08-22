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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ServerExplosion;

public class DamageUtils {

	private static final Minecraft mc = Minecraft.getInstance();

	public static float getItemAttackDamage(ItemStack stack) {
		float damage = 1f
				+ (float) stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
				.compute(Attributes.ATTACK_DAMAGE, 0.0, EquipmentSlot.MAINHAND);

		// TODO(26.2): EnchantmentHelper.getAttackDamage(stack, EntityGroup) was removed together with MobType/EntityGroup.
		// The melee enchant bonus (Sharpness/Smite/BaneOfArthropods) is now computed via EnchantmentHelper.modifyDamage(ServerLevel, ...),
		// which needs a ServerLevel + DamageSource + victim that aren't available client-side. Base weapon damage is preserved.
		return damage;
	}

	public static float getAttackDamage(Player attacker, Entity target) {
		float cooldown = attacker.getAttackStrengthScale(0.5F);

		float damage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE)
				+ (float) attacker.getMainHandItem().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
				.compute(Attributes.ATTACK_DAMAGE, 0.0, EquipmentSlot.MAINHAND);

		damage *= 0.2f + cooldown * cooldown * 0.8f;
		// TODO(26.2): melee enchant bonus excluded from prediction - EnchantmentHelper.getAttackDamage(stack, group) was removed
		// (MobType/EntityGroup gone) and its replacement (modifyDamage) needs a ServerLevel not available on the client.
		float enchDamage = 0f;

		if (damage <= 0f && enchDamage <= 0f) {
			return 0f;
		}

		// Crits
		if (cooldown > 0.9
				&& attacker.fallDistance > 0.0F
				&& !attacker.onGround()
				&& !attacker.onClimbable()
				&& !attacker.isInWater()
				&& !attacker.hasEffect(MobEffects.BLINDNESS)
				&& !attacker.isPassenger()
				&& !attacker.isSprinting()
				&& target instanceof LivingEntity) {
			damage *= 1.5f;
		}

		damage += enchDamage;

		if (target instanceof LivingEntity) {
			LivingEntity livingTarget = (LivingEntity) target;
			DamageSource source = mc.level.damageSources().playerAttack(attacker);

			// Armor
			damage = CombatRules.getDamageAfterAbsorb(livingTarget, damage, source, livingTarget.getArmorValue(),
					(float) livingTarget.getAttributeValue(Attributes.ARMOR_TOUGHNESS));

			// Enchantments
			if (livingTarget.hasEffect(MobEffects.RESISTANCE)) {
				int resistance = 25 - (livingTarget.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
				float resistance_1 = damage * resistance;
				damage = Math.max(resistance_1 / 25f, 0f);
			}
		}

		if (damage <= 0f) {
			damage = 0f;
		}
		// TODO(26.2): protection-enchant reduction excluded - EnchantmentHelper.getProtectionAmount(armor, source) was removed;
		// its replacement getDamageProtection(ServerLevel, victim, source) needs a ServerLevel not available on the client.

		return damage;
	}

	public static float getExplosionDamage(Vec3 explosionPos, float power, LivingEntity target) {
		if (mc.level.getDifficulty() == Difficulty.PEACEFUL)
			return 0f;

		double maxDist = power * 2;
		if (!mc.level.getEntities((Entity) null, new AABB(
				Mth.floor(explosionPos.x - maxDist - 1.0),
				Mth.floor(explosionPos.y - maxDist - 1.0),
				Mth.floor(explosionPos.z - maxDist - 1.0),
				Mth.floor(explosionPos.x + maxDist + 1.0),
				Mth.floor(explosionPos.y + maxDist + 1.0),
				Mth.floor(explosionPos.z + maxDist + 1.0))).contains(target)) {
			return 0f;
		}

		if (!target.isInvulnerable()) {
			double distExposure = Math.sqrt(target.distanceToSqr(explosionPos)) / maxDist;
			if (distExposure <= 1.0) {
				double xDiff = target.getX() - explosionPos.x;
				double yDiff = target.getEyeY() - explosionPos.y;
				double zDiff = target.getZ() - explosionPos.z;
				double diff = Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
				if (diff != 0.0) {
					double exposure = ServerExplosion.getSeenPercent(explosionPos, target);
					double finalExposure = (1.0 - distExposure) * exposure;

					float toDamage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * maxDist + 1.0);

					if (target instanceof Player) {
						if (mc.level.getDifficulty() == Difficulty.EASY) {
							toDamage = Math.min(toDamage / 2f + 1f, toDamage);
						} else if (mc.level.getDifficulty() == Difficulty.HARD) {
							toDamage = toDamage * 3f / 2f;
						}
					}

					DamageSource source = mc.level.damageSources().explosion(null, null);

					// Armor
					toDamage = CombatRules.getDamageAfterAbsorb(target, toDamage, source, target.getArmorValue(),
							(float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));

					// Enchantments
					if (target.hasEffect(MobEffects.RESISTANCE)) {
						int resistance = 25 - (target.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
						float resistance_1 = toDamage * resistance;
						toDamage = Math.max(resistance_1 / 25f, 0f);
					}

					if (toDamage <= 0f) {
						toDamage = 0f;
					}
					// TODO(26.2): protection-enchant reduction excluded - EnchantmentHelper.getProtectionAmount(armor, source) was
					// removed; getDamageProtection(ServerLevel, victim, source) needs a ServerLevel not available on the client.

					return toDamage;
				}
			}
		}

		return 0f;
	}

	public static boolean willKill(LivingEntity target, float damage) {
		if (target.getMainHandItem().getItem() == Items.TOTEM_OF_UNDYING || target.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) {
			return false;
		}

		return damage >= target.getHealth() + target.getAbsorptionAmount();
	}

	public static boolean willPop(LivingEntity target, float damage) {
		if (target.getMainHandItem().getItem() != Items.TOTEM_OF_UNDYING && target.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING) {
			return false;
		}

		return damage >= target.getHealth() + target.getAbsorptionAmount();
	}

	public static boolean willPopOrKill(LivingEntity target, float damage) {
		return damage >= target.getHealth() + target.getAbsorptionAmount();
	}

	public static boolean willGoBelowHealth(LivingEntity target, float damage, float minHealth) {
		return target.getHealth() + target.getAbsorptionAmount() - damage < minHealth;
	}
}
