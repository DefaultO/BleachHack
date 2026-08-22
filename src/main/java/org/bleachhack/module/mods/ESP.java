/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventEntityRender;
import org.bleachhack.event.events.EventWorldRender;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.setting.module.SettingColor;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.render.Renderer;
import org.bleachhack.util.render.color.QuadColor;
import org.bleachhack.util.world.EntityUtils;

import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.boat.Boat;

public class ESP extends Module {

	public ESP() {
		super("ESP", KEY_UNBOUND, ModuleCategory.RENDER, "Highlights Entities in the world.",
				new SettingMode("Render", "Shader", "Box").withDesc("The Render mode."),
				new SettingSlider("ShaderFill", 1, 255, 50, 0).withDesc("How opaque the fill on shader mode should be."),
				new SettingSlider("Box", 0, 5, 2, 1).withDesc("How thick the box outline should be."),
				new SettingSlider("BoxFill", 0, 255, 50, 0).withDesc("How opaque the fill on box mode should be."),

				new SettingToggle("Players", true).withDesc("Highlights Players.").withChildren(
						new SettingColor("Player Color", 255, 75, 75).withDesc("Outline color for players."),
						new SettingColor("Friend Color", 0, 255, 255).withDesc("Outline color for friends.")),

				new SettingToggle("Mobs", false).withDesc("Highlights Mobs.").withChildren(
						new SettingColor("Color", 128, 25, 128).withDesc("Outline color for mobs.")),

				new SettingToggle("Animals", false).withDesc("Highlights Animals").withChildren(
						new SettingColor("Color", 75, 255, 75).withDesc("Outline color for animals.")),

				new SettingToggle("Items", true).withDesc("Highlights Items.").withChildren(
						new SettingColor("Color", 255, 200, 50).withDesc("Outline color for items.")),

				new SettingToggle("Crystals", true).withDesc("Highlights End Crystals.").withChildren(
						new SettingColor("Color", 255, 50, 255).withDesc("Outline color for crystals.")),

				new SettingToggle("Vehicles", false).withDesc("Highlights Vehicles.").withChildren(
						new SettingColor("Color", 150, 150, 150).withDesc("Outline color for vehicles (minecarts/boats).")),

				new SettingToggle("Armorstands", false).withDesc("Highlights armor stands.").withChildren(
						new SettingColor("Color", 160, 150, 50).withDesc("Outline color for armor stands.")));
	}

	/** True while ESP is drawing through the shader path (used to swap in our post chain). */
	public static boolean isShaderModeActive() {
		ESP esp = ModuleManager.getModule(ESP.class);
		return esp != null && esp.isEnabled() && esp.getSetting(0).asMode().getMode() == 0;
	}

	/**
	 * Shader mode: hand the entity a color during render-state extraction. 26.2 draws
	 * every entity with a non-zero outlineColor as a flat silhouette into the
	 * entity_outline framebuffer, which our post chain (bleachhack:entity_outline)
	 * turns into a solid rim plus a translucent fill.
	 *
	 * The alpha channel carries the fill opacity: a PostPass bakes its uniforms when
	 * it's built and can't be updated per frame, so the per-entity color is the only
	 * channel that can carry a live setting into the shader.
	 */
	@BleachSubscribe
	public void onEntityOutline(EventEntityRender.Single.Outline event) {
		if (getSetting(0).asMode().getMode() != 0)
			return;

		int[] color = getColor(event.getEntity());

		if (color != null) {
			event.setColor(ARGB.color(getSetting(1).asSlider().getValueInt(), color[0], color[1], color[2]));
		}
	}

	@BleachSubscribe
	public void onWorldRender(EventWorldRender.Post event) {
		if (getSetting(0).asMode().getMode() == 0)
			return;

		float width = getSetting(2).asSlider().getValueFloat();
		int fill = getSetting(3).asSlider().getValueInt();

		for (Entity e: mc.level.entitiesForRendering()) {
			int[] color = getColor(e);

			if (color != null) {
				if (width != 0)
					Renderer.drawBoxOutline(e.getBoundingBox(), QuadColor.single(color[0], color[1], color[2], 255), width);

				if (fill != 0)
					Renderer.drawBoxFill(e.getBoundingBox(), QuadColor.single(color[0], color[1], color[2], fill));
			}
		}
	}

	private int[] getColor(Entity e) {
		if (e == mc.player)
			return null;

		if (e instanceof Player && getSetting(4).asToggle().getState()) {
			return getSetting(4).asToggle().getChild(BleachHack.friendMang.has(e) ? 1 : 0).asColor().getRGBArray();
		} else if (e instanceof Enemy && getSetting(5).asToggle().getState()) {
			return getSetting(5).asToggle().getChild(0).asColor().getRGBArray();
		} else if (EntityUtils.isAnimal(e) && getSetting(6).asToggle().getState()) {
			return getSetting(6).asToggle().getChild(0).asColor().getRGBArray();
		} else if (e instanceof ItemEntity && getSetting(7).asToggle().getState()) {
			return getSetting(7).asToggle().getChild(0).asColor().getRGBArray();
		} else if (e instanceof EndCrystal && getSetting(8).asToggle().getState()) {
			return getSetting(8).asToggle().getChild(0).asColor().getRGBArray();
		} else if ((e instanceof Boat || e instanceof AbstractMinecart) && getSetting(9).asToggle().getState()) {
			return getSetting(9).asToggle().getChild(0).asColor().getRGBArray();
		} else if (e instanceof ArmorStand && getSetting(10).asToggle().getState()) {
			return getSetting(10).asToggle().getChild(0).asColor().getRGBArray();
		}

		return null;
	}
}