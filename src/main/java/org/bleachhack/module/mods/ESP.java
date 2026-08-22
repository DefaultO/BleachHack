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
import org.bleachhack.util.shader.BleachShaders;
import org.bleachhack.util.world.EntityUtils;
import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.boat.Boat;

public class ESP extends Module {

	private static final int RENDER = 0;
	private static final int SHADER_FILL = 1;
	private static final int SHADER_OUTLINE = 2;
	private static final int BOX = 3;
	private static final int BOX_FILL = 4;
	private static final int MAX_DISTANCE = 5;
	private static final int INVISIBLES = 6;
	private static final int HEALTH_TINT = 7;
	private static final int PLAYERS = 8;
	private static final int MOBS = 9;
	private static final int ANIMALS = 10;
	private static final int ITEMS = 11;
	private static final int CRYSTALS = 12;
	private static final int VEHICLES = 13;
	private static final int ARMORSTANDS = 14;
	private static final int PROJECTILES = 15;
	private static final int OTHER = 16;

	public ESP() {
		super("ESP", KEY_UNBOUND, ModuleCategory.RENDER, "Highlights Entities in the world.",
				new SettingMode("Render", "Shader", "Box").withDesc("The Render mode."),
				new SettingSlider("ShaderFill", 0, 255, 50, 0).withDesc("How opaque the fill on shader mode should be."),
				new SettingSlider("ShaderOutline", 1, 5, 1, 0).withDesc("How thick the outline on shader mode should be."),
				new SettingSlider("Box", 0, 5, 2, 1).withDesc("How thick the box outline should be."),
				new SettingSlider("BoxFill", 0, 255, 50, 0).withDesc("How opaque the fill on box mode should be."),
				new SettingSlider("MaxDistance", 0, 256, 0, 0).withDesc("Only highlight entities within this many blocks, 0 = no limit."),
				new SettingToggle("Invisibles", true).withDesc("Also highlight invisible entities."),
				new SettingToggle("HealthTint", false).withDesc("Fade living entities from their color to red as they lose health."),

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
						new SettingColor("Color", 160, 150, 50).withDesc("Outline color for armor stands.")),

				new SettingToggle("Projectiles", false).withDesc("Highlights arrows, fireballs and thrown items.").withChildren(
						new SettingColor("Color", 255, 255, 255).withDesc("Outline color for projectiles.")),

				new SettingToggle("Other", false).withDesc("Highlights anything the categories above miss (tnt, falling blocks, item frames, xp...).").withChildren(
						new SettingColor("Color", 200, 200, 200).withDesc("Outline color for everything else.")));
	}

	/**
	 * The post effect to run while shader mode is on, or null to leave vanilla's alone.
	 * Fill and thickness pick a pre-baked variant - see {@link BleachShaders}.
	 */
	public static @Nullable Identifier currentShaderChain() {
		ESP esp = ModuleManager.getModule(ESP.class);

		if (esp == null || !esp.isEnabled() || esp.getSetting(RENDER).asMode().getMode() != 0) {
			return null;
		}

		return BleachShaders.variantFor(
				esp.getSetting(SHADER_FILL).asSlider().getValueInt(),
				esp.getSetting(SHADER_OUTLINE).asSlider().getValueInt());
	}

	/**
	 * Shader mode: give the entity a color during render-state extraction. 26.2 draws
	 * every entity with a non-zero outlineColor as a flat silhouette into the
	 * entity_outline framebuffer, which our post effect turns into a filled highlight
	 * with a solid rim.
	 *
	 * The color has to be opaque - the outline render type takes its alpha from the
	 * render type's own modulator and ignores whatever we put here, so fill opacity is
	 * carried by the shader variant instead.
	 */
	@BleachSubscribe
	public void onEntityOutline(EventEntityRender.Single.Outline event) {
		if (getSetting(RENDER).asMode().getMode() != 0) {
			return;
		}

		int[] color = getColor(event.getEntity());

		if (color != null) {
			event.setColor(ARGB.color(255, color[0], color[1], color[2]));
		}
	}

	@BleachSubscribe
	public void onWorldRender(EventWorldRender.Post event) {
		if (getSetting(RENDER).asMode().getMode() == 0) {
			return;
		}

		float width = getSetting(BOX).asSlider().getValueFloat();
		int fill = getSetting(BOX_FILL).asSlider().getValueInt();

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
		if (e == mc.player) {
			return null;
		}

		if (!getSetting(INVISIBLES).asToggle().getState() && e.isInvisible()) {
			return null;
		}

		double maxDistance = getSetting(MAX_DISTANCE).asSlider().getValue();
		if (maxDistance > 0 && mc.player != null && mc.player.distanceToSqr(e) > maxDistance * maxDistance) {
			return null;
		}

		int[] color = getCategoryColor(e);

		if (color != null && getSetting(HEALTH_TINT).asToggle().getState() && e instanceof LivingEntity living && living.getMaxHealth() > 0) {
			float health = Math.min(living.getHealth() / living.getMaxHealth(), 1f);
			color = new int[] {
					Math.round(color[0] + (255 - color[0]) * (1f - health)),
					Math.round(color[1] * health),
					Math.round(color[2] * health) };
		}

		return color;
	}

	private int[] getCategoryColor(Entity e) {
		if (e instanceof Player && getSetting(PLAYERS).asToggle().getState()) {
			return getSetting(PLAYERS).asToggle().getChild(BleachHack.friendMang.has(e) ? 1 : 0).asColor().getRGBArray();
		} else if (e instanceof Enemy && getSetting(MOBS).asToggle().getState()) {
			return getSetting(MOBS).asToggle().getChild(0).asColor().getRGBArray();
		} else if (EntityUtils.isAnimal(e) && getSetting(ANIMALS).asToggle().getState()) {
			return getSetting(ANIMALS).asToggle().getChild(0).asColor().getRGBArray();
		} else if (e instanceof ItemEntity && getSetting(ITEMS).asToggle().getState()) {
			return getSetting(ITEMS).asToggle().getChild(0).asColor().getRGBArray();
		} else if (e instanceof EndCrystal && getSetting(CRYSTALS).asToggle().getState()) {
			return getSetting(CRYSTALS).asToggle().getChild(0).asColor().getRGBArray();
		} else if ((e instanceof Boat || e instanceof AbstractMinecart) && getSetting(VEHICLES).asToggle().getState()) {
			return getSetting(VEHICLES).asToggle().getChild(0).asColor().getRGBArray();
		} else if (e instanceof ArmorStand && getSetting(ARMORSTANDS).asToggle().getState()) {
			return getSetting(ARMORSTANDS).asToggle().getChild(0).asColor().getRGBArray();
		} else if (e instanceof Projectile && getSetting(PROJECTILES).asToggle().getState()) {
			return getSetting(PROJECTILES).asToggle().getChild(0).asColor().getRGBArray();
		} else if (getSetting(OTHER).asToggle().getState()) {
			// catch-all so nothing is silently skipped - tnt, falling blocks, item frames,
			// xp orbs, and any mob that fits none of the categories above
			return getSetting(OTHER).asToggle().getChild(0).asColor().getRGBArray();
		}

		return null;
	}
}
