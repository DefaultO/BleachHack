/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.command.commands;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bleachhack.command.Command;
import org.bleachhack.command.CommandCategory;
import org.bleachhack.command.exception.CmdSyntaxException;
import org.bleachhack.util.BleachLogger;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

public class CmdEnchant extends Command {

	private static final Map<String[], ResourceKey<Enchantment>> enchantments = new LinkedHashMap<>();

	static {
		enchantments.put(new String[] { "aqua_affinity", "aqua" }, Enchantments.AQUA_AFFINITY);
		enchantments.put(new String[] { "bane_of_arthropods", "arthropods" }, Enchantments.BANE_OF_ARTHROPODS);
		enchantments.put(new String[] { "blast", "blast_prot" }, Enchantments.BLAST_PROTECTION);
		enchantments.put(new String[] { "channeling" }, Enchantments.CHANNELING);
		enchantments.put(new String[] { "curse_binding", "binding" }, Enchantments.BINDING_CURSE);
		enchantments.put(new String[] { "curse_vanish", "vanish" }, Enchantments.VANISHING_CURSE);
		enchantments.put(new String[] { "depth_strider", "strider" }, Enchantments.DEPTH_STRIDER);
		enchantments.put(new String[] { "efficiency", "eff" }, Enchantments.EFFICIENCY);
		enchantments.put(new String[] { "feather_falling", "fall" }, Enchantments.FEATHER_FALLING);
		enchantments.put(new String[] { "fire_aspect" }, Enchantments.FIRE_ASPECT);
		enchantments.put(new String[] { "fire_prot" }, Enchantments.FIRE_PROTECTION);
		enchantments.put(new String[] { "flame" }, Enchantments.FLAME);
		enchantments.put(new String[] { "fortune" }, Enchantments.FORTUNE);
		enchantments.put(new String[] { "frost_walker", "frost" }, Enchantments.FROST_WALKER);
		enchantments.put(new String[] { "impaling" }, Enchantments.IMPALING);
		enchantments.put(new String[] { "infinity" }, Enchantments.INFINITY);
		enchantments.put(new String[] { "knockback", "knock" }, Enchantments.KNOCKBACK);
		enchantments.put(new String[] { "looting", "loot" }, Enchantments.LOOTING);
		enchantments.put(new String[] { "loyalty" }, Enchantments.LOOTING);
		enchantments.put(new String[] { "luck_of_the_sea", "luck" }, Enchantments.LUCK_OF_THE_SEA);
		enchantments.put(new String[] { "lure" }, Enchantments.LURE);
		enchantments.put(new String[] { "mending", "mend" }, Enchantments.MENDING);
		enchantments.put(new String[] { "multishot" }, Enchantments.MULTISHOT);
		enchantments.put(new String[] { "piercing" }, Enchantments.PIERCING);
		enchantments.put(new String[] { "power" }, Enchantments.POWER);
		enchantments.put(new String[] { "projectile_prot", "proj_prot" }, Enchantments.PROJECTILE_PROTECTION);
		enchantments.put(new String[] { "protection", "prot" }, Enchantments.PROTECTION);
		enchantments.put(new String[] { "punch" }, Enchantments.PUNCH);
		enchantments.put(new String[] { "quick_charge", "charge" }, Enchantments.QUICK_CHARGE);
		enchantments.put(new String[] { "respiration", "resp" }, Enchantments.RESPIRATION);
		enchantments.put(new String[] { "riptide" }, Enchantments.RIPTIDE);
		enchantments.put(new String[] { "sharpness", "sharp" }, Enchantments.SHARPNESS);
		enchantments.put(new String[] { "silk_touch", "silk" }, Enchantments.SILK_TOUCH);
		enchantments.put(new String[] { "smite" }, Enchantments.SMITE);
		enchantments.put(new String[] { "sweeping_edge", "sweep" }, Enchantments.SWEEPING_EDGE);
		enchantments.put(new String[] { "thorns" }, Enchantments.THORNS);
		enchantments.put(new String[] { "soul_speed", "soul" }, Enchantments.SOUL_SPEED);
		enchantments.put(new String[] { "unbreaking" }, Enchantments.UNBREAKING);
	}

	public CmdEnchant() {
		super("enchant", "Enchants an item.", "enchant <enchant/id> <level> | enchant all <level> | enchant list", CommandCategory.CREATIVE);
	}

	@Override
	public void onCommand(String alias, String[] args) throws CmdSyntaxException {
		if (!mc.gameMode.getPlayerMode().isCreative()) {
			BleachLogger.error("Not In Creative Mode!");
			return;
		}

		if (args.length == 0) {
			throw new CmdSyntaxException();
		}

		if (args[0].equalsIgnoreCase("list")) {
			MutableComponent text = Component.literal("");
			int i = 0;
			for (String[] s: enchantments.keySet()) {
				int color = i % 2 == 0 ? BleachLogger.INFO_COLOR : TextColor.AQUA.getValue();
				text.append(Component.literal("§7[§r" + String.join("§7/§r", s) + "§7] ").setStyle(Style.EMPTY.withColor(color)));
				i++;
			}

			BleachLogger.info(text);
			return;
		}

		int level = args.length == 1 ? 1 : Integer.parseInt(args[1]);
		ItemStack item = mc.player.getInventory().getSelectedItem();

		// TODO(26.2): enchantments are data-driven now; resolved through the level's registry.
		Registry<Enchantment> registry = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

		if (args[0].equalsIgnoreCase("all")) {
			for (Holder<Enchantment> e : registry.listElements().toList()) {
				enchant(item, e, level);
			}

			return;
		}

		int i = NumberUtils.toInt(args[0], -1);

		if (i != -1) {
			enchant(item, registry.get(i).orElse(null), level);
		} else {
			enchant(item, enchantments.entrySet().stream()
					.filter(e -> ArrayUtils.contains(e.getKey(), args[0]))
					.map(Entry::getValue)
					.findFirst()
					.flatMap(registry::get)
					.orElse(null), level);
		}
	}

	public void enchant(ItemStack item, Holder<Enchantment> e, int level) throws CmdSyntaxException {
		if (e == null) {
			throw new CmdSyntaxException("Invalid enchantment!");
		}

		// TODO(26.2): enchantments are a data component now; levels are clamped to 255 (raw NBT used to allow up to 32767).
		item.enchant(e, level);
	}

}
