/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.command.commands;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.bleachhack.command.Command;
import org.bleachhack.command.CommandCategory;
import org.bleachhack.command.exception.CmdSyntaxException;
import org.bleachhack.util.BleachLogger;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.io.Resources;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

public class CmdSkull extends Command {

	public CmdSkull() {
		super("skull", "Gives you a player skull.", "skull <player> | skull img <image url>", CommandCategory.CREATIVE,
				"playerhead", "head");
	}

	@Override
	public void onCommand(String alias, String[] args) throws CmdSyntaxException, CommandSyntaxException {
		if (!mc.gameMode.getPlayerMode().isCreative()) {
			BleachLogger.error("Not In Creative Mode!");
			return;
		}

		if (args.length == 0) {
			throw new CmdSyntaxException();
		}

		ItemStack item = new ItemStack(Items.PLAYER_HEAD, 64);

		if (args.length < 2) {
			try {
				JsonObject json = JsonParser.parseString(
						Resources.toString(new URL("https://api.mojang.com/users/profiles/minecraft/" + args[0]), StandardCharsets.UTF_8))
						.getAsJsonObject();

				JsonObject json2 = JsonParser.parseString(
						Resources.toString(new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + json.get("id").getAsString()), StandardCharsets.UTF_8))
						.getAsJsonObject();

				item.set(DataComponents.PROFILE, texturedProfile(json.get("name").getAsString(),
						json2.get("properties").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString()));
			} catch (Exception e) {
				e.printStackTrace();
				BleachLogger.error("Error getting head! (" + e.getClass().getSimpleName() + ")");
			}
		} else if (args[0].equalsIgnoreCase("img")) {
			// 26.2: SkullOwner NBT was replaced by the minecraft:profile data component
			ResolvableProfile profile = texturedProfile("img", encodeUrl(args[1]));
			item.set(DataComponents.PROFILE, profile);
			BleachLogger.logger.info(profile);
		}

		mc.player.getInventory().addAndPickItem(item);
	}

	private ResolvableProfile texturedProfile(String name, String textureValue) {
		PropertyMap properties = new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", textureValue)));
		return ResolvableProfile.createResolved(new GameProfile(UUID.randomUUID(), name, properties));
	}

	private String encodeUrl(String url) {
		return Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}").getBytes());
	}

}
