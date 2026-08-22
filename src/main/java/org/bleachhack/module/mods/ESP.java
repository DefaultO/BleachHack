/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventEntityRender;
import org.bleachhack.event.events.EventWorldRender;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.esp.EspGroup;
import org.bleachhack.setting.module.ModuleSetting;
import org.bleachhack.util.BleachLogger;
import org.bleachhack.setting.module.SettingColor;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.render.Renderer;
import org.bleachhack.util.render.color.QuadColor;
import org.bleachhack.util.shader.BleachShaders;
import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class ESP extends Module {

	private static final int RENDER = 0;
	private static final int SHADER_FILL = 1;
	private static final int SHADER_OUTLINE = 2;
	private static final int BOX = 3;
	private static final int BOX_FILL = 4;
	private static final int MAX_DISTANCE = 5;
	private static final int INVISIBLES = 6;
	private static final int HEALTH_TINT = 7;
	private static final int THROUGH_WALLS = 8;
	private static final int SHADER_STYLE = 9;
	/** Colour groups start here; everything after is generated from the entity registry. */
	private static final int GROUPS_START = 10;

	/** Players get a second colour before the per-entity rows, for friends. */
	private static final int FRIEND_COLOR_CHILD = 1;

	/** For each group: where its per-entity override rows live inside the group toggle. */
	private static final Map<EntityType<?>, Integer> ENTITY_CHILD_INDEX = new HashMap<>();
	private static final Map<EspGroup, Integer> GROUP_SETTING_INDEX = new EnumMap<>(EspGroup.class);

	public ESP() {
		super("ESP", KEY_UNBOUND, ModuleCategory.RENDER, "Highlights Entities in the world.", buildSettings());

		// Only show the options that apply to the selected render mode.
		getSetting(SHADER_FILL).visibleWhen(this::isShaderMode);
		getSetting(SHADER_OUTLINE).visibleWhen(this::isShaderMode);
		getSetting(SHADER_STYLE).visibleWhen(this::isShaderMode);
		getSetting(BOX).visibleWhen(() -> !isShaderMode());
		getSetting(BOX_FILL).visibleWhen(() -> !isShaderMode());
		getSetting(THROUGH_WALLS).visibleWhen(() -> !isShaderMode());
	}

	/**
	 * Base options, then one toggle per colour group. Each group holds its own colour
	 * plus a row per entity type in it, so a whole family can share a colour or any
	 * single entity can override it. The per-entity rows come from the registry, so
	 * nothing is missing and modded entities show up too.
	 */
	private static ModuleSetting<?>[] buildSettings() {
		List<ModuleSetting<?>> settings = new ArrayList<>(List.of(
				new SettingMode("Render", "Shader", "Box").withDesc("The Render mode."),
				new SettingSlider("ShaderFill", 0, 255, 50, 0).withDesc("How opaque the fill on shader mode should be."),
				new SettingSlider("ShaderOutline", 1, 5, 1, 0).withDesc("How thick the outline on shader mode should be."),
				new SettingSlider("Box", 0, 5, 2, 1).withDesc("How thick the box outline should be."),
				new SettingSlider("BoxFill", 0, 255, 50, 0).withDesc("How opaque the fill on box mode should be."),
				new SettingSlider("MaxDistance", 0, 256, 0, 0).withDesc("Only highlight entities within this many blocks, 0 = no limit."),
				new SettingToggle("Invisibles", true).withDesc("Also highlight invisible entities."),
				new SettingToggle("HealthTint", false).withDesc("Fade living entities from their color to red as they lose health."),
				new SettingToggle("ThroughWalls", true).withDesc("Draw box mode over terrain and fluids instead of hiding behind them."),
				new SettingMode("ShaderStyle", "Solid", "Inline", "Gradient")
						.withDesc("How the shader outline is shaded across its width. Inline puts a dark band against the entity.")));

		// group -> its entity types, sorted so the rows read alphabetically
		Map<EspGroup, List<EntityType<?>>> byGroup = new EnumMap<>(EspGroup.class);

		for (EspGroup group : EspGroup.values()) {
			byGroup.put(group, new ArrayList<>());
		}

		try {
			for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
				byGroup.get(EspGroup.of(type)).add(type);
			}
		} catch (Throwable t) {
			BleachLogger.logger.log(Level.WARN, "ESP could not enumerate entity types, per-entity colors unavailable: %s", t);
		}

		for (EspGroup group : EspGroup.values()) {
			List<EntityType<?>> types = byGroup.get(group);
			types.sort(Comparator.comparing(EspGroup::prettyName));

			List<ModuleSetting<?>> children = new ArrayList<>();
			children.add(new SettingColor("Color", group.red, group.green, group.blue)
					.withDesc("Color for every " + group.displayName.toLowerCase() + " entity without its own override."));

			if (group == EspGroup.PLAYERS) {
				children.add(new SettingColor("Friend Color", 0, 255, 255).withDesc("Color for players on your friends list."));
			}

			for (EntityType<?> type : types) {
				ENTITY_CHILD_INDEX.put(type, children.size());
				children.add(new SettingToggle(EspGroup.prettyName(type), false)
						.withDesc("Use a custom color for this entity instead of the group color.")
						.withChildren(new SettingColor("Color", group.red, group.green, group.blue)));
			}

			GROUP_SETTING_INDEX.put(group, settings.size());
			settings.add(new SettingToggle(group.displayName, group.enabledByDefault)
					.withDesc("Highlights " + group.displayName.toLowerCase() + " entities.")
					.withChildren(children.toArray(new ModuleSetting<?>[0])));
		}

		return settings.toArray(new ModuleSetting<?>[0]);
	}

	private boolean isShaderMode() {
		return getSetting(RENDER).asMode().getMode() == 0;
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
				esp.getSetting(SHADER_OUTLINE).asSlider().getValueInt(),
				esp.getSetting(SHADER_STYLE).asMode().getMode());
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
		boolean throughWalls = getSetting(THROUGH_WALLS).asToggle().getState();

		for (Entity e: mc.level.entitiesForRendering()) {
			int[] color = getColor(e);

			if (color != null) {
				if (width != 0)
					Renderer.drawBoxOutline(e.getBoundingBox(), QuadColor.single(color[0], color[1], color[2], 255), width, throughWalls);

				if (fill != 0)
					Renderer.drawBoxFill(e.getBoundingBox(), QuadColor.single(color[0], color[1], color[2], fill), throughWalls);
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
		EspGroup group = EspGroup.of(e.getType());
		Integer groupIndex = GROUP_SETTING_INDEX.get(group);

		if (groupIndex == null) {
			return null;
		}

		SettingToggle groupToggle = getSetting(groupIndex).asToggle();

		if (!groupToggle.getState()) {
			return null;
		}

		if (group == EspGroup.PLAYERS && BleachHack.friendMang.has(e)) {
			return groupToggle.getChild(FRIEND_COLOR_CHILD).asColor().getRGBArray();
		}

		Integer childIndex = ENTITY_CHILD_INDEX.get(e.getType());

		if (childIndex != null && childIndex < groupToggle.getChildren().size()) {
			SettingToggle override = groupToggle.getChild(childIndex).asToggle();

			if (override.getState()) {
				return override.getChild(0).asColor().getRGBArray();
			}
		}

		return groupToggle.getChild(0).asColor().getRGBArray();
	}
}
