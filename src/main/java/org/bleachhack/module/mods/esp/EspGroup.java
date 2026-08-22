/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods.esp;

import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

/**
 * Buckets every entity type into a colour group for ESP.
 *
 * Groups give one colour for a whole family, and each group can be expanded to
 * override individual entity types. Membership is derived from the entity's own
 * class and spawn category, so modded and future vanilla entities land somewhere
 * sensible without needing a hardcoded list.
 */
public enum EspGroup {

	PLAYERS("Players", 255, 75, 75, true),
	HOSTILE("Hostile", 255, 60, 60, false),
	PASSIVE("Passive", 75, 255, 75, false),
	AQUATIC("Aquatic", 80, 180, 255, false),
	AMBIENT("Ambient", 190, 190, 120, false),
	ITEMS("Items", 255, 200, 50, true),
	CRYSTALS("Crystals", 255, 50, 255, true),
	VEHICLES("Vehicles", 150, 150, 150, false),
	PROJECTILES("Projectiles", 255, 255, 255, false),
	OTHER("Other", 200, 200, 200, false);

	public final String displayName;
	public final int red;
	public final int green;
	public final int blue;
	public final boolean enabledByDefault;

	EspGroup(String displayName, int red, int green, int blue, boolean enabledByDefault) {
		this.displayName = displayName;
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.enabledByDefault = enabledByDefault;
	}

	/**
	 * EntityType.getBaseClass() always answers Entity.class (it's part of EntityTypeTest,
	 * not a real accessor), so the non-mob families are matched on their registry id.
	 * Both the settings list and the runtime lookup call this, so they can never disagree.
	 */
	private static final Set<String> PROJECTILE_IDS = Set.of(
			"arrow", "spectral_arrow", "trident", "snowball", "egg", "ender_pearl", "experience_bottle",
			"potion", "splash_potion", "lingering_potion", "fireball", "small_fireball", "dragon_fireball",
			"wither_skull", "shulker_bullet", "llama_spit", "fishing_bobber", "firework_rocket",
			"wind_charge", "breeze_wind_charge");

	public static EspGroup of(EntityType<?> type) {
		String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();

		if (id.equals("player")) {
			return PLAYERS;
		} else if (id.equals("item")) {
			return ITEMS;
		} else if (id.equals("end_crystal")) {
			return CRYSTALS;
		} else if (id.contains("boat") || id.contains("raft") || id.contains("minecart")) {
			return VEHICLES;
		} else if (PROJECTILE_IDS.contains(id)) {
			return PROJECTILES;
		}

		return switch (type.getCategory()) {
			case MONSTER -> HOSTILE;
			case CREATURE -> PASSIVE;
			case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> AQUATIC;
			case AMBIENT -> EspGroup.AMBIENT;
			default -> OTHER;
		};
	}

	/** "zombie_villager" -> "Zombie Villager", for the per-entity rows. */
	public static String prettyName(EntityType<?> type) {
		String[] words = type.toShortString().split("_");
		StringBuilder sb = new StringBuilder();

		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}

			if (sb.length() > 0) {
				sb.append(' ');
			}

			sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}

		return sb.toString();
	}
}
