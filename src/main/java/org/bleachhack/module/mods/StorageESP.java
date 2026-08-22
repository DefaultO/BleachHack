/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import com.google.gson.JsonSyntaxException;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

import org.bleachhack.event.events.EventEntityRender;
import org.bleachhack.event.events.EventWorldRender;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.Boxes;
import org.bleachhack.util.render.Renderer;
import org.bleachhack.util.render.color.QuadColor;
import org.bleachhack.util.shader.BleachCoreShaders;
import org.bleachhack.util.shader.ColorVertexConsumerProvider;
import org.bleachhack.util.shader.ShaderEffectWrapper;
import org.bleachhack.util.shader.ShaderLoader;
import org.bleachhack.util.world.WorldUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class StorageESP extends Module {

	private ShaderEffectWrapper shader;
	private ColorVertexConsumerProvider colorVertexer;

	public StorageESP() {
		super("StorageESP", KEY_UNBOUND, ModuleCategory.RENDER, "Highlights storage containers in the world.",
				new SettingMode("Render", "Shader", "AABB").withDesc("The Render mode."),
				new SettingSlider("ShaderFill", 1, 255, 50, 0).withDesc("How opaque the fill on shader mode should be."),
				new SettingSlider("AABB", 0, 5, 2, 1).withDesc("How thick the box outline should be."),
				new SettingSlider("BoxFill", 0, 255, 50, 0).withDesc("How opaque the fill on box mode should be."),

				new SettingToggle("Chests", true).withDesc("Highlights chests/barrels."),
				new SettingToggle("Enderchests", true).withDesc("Highlights enderchests."),
				new SettingToggle("Furnaces", true).withDesc("Highlights furnaces."),
				new SettingToggle("Dispensers", true).withDesc("Highlights dispensers/droppers."),
				new SettingToggle("Hoppers", true).withDesc("Highlights hoppers."),
				new SettingToggle("Shulkers", true).withDesc("Highlights shulkers."),
				new SettingToggle("Brewingstands", true).withDesc("Highlights brewing stands."),
				new SettingToggle("ChestCarts", true).withDesc("Highlights chests in minecarts."),
				new SettingToggle("FurnaceCarts", true).withDesc("Highlights furnaces in minecarts."),
				new SettingToggle("HopperCarts", true).withDesc("Highlights hoppers in minecarts."),
				new SettingToggle("Itemframes", true).withDesc("Highlights item frames."));
		
		try {
			shader = new ShaderEffectWrapper(
					ShaderLoader.loadEffect(mc.gameRenderer.mainRenderTarget(), Identifier.fromNamespaceAndPath("bleachhack", "shaders/post/entity_outline.json")));

			colorVertexer = new ColorVertexConsumerProvider(shader.getFramebuffer("main"), BleachCoreShaders::getColorOverlayShader);
		} catch (JsonSyntaxException | IOException e) {
			throw new RuntimeException("Failed to initialize StorageESP Shader! loaded too early?", e);
		}
	}

	@BleachSubscribe
	public void onWorldRender(EventWorldRender.Pre event) {
		shader.prepare();
		shader.clearFramebuffer("main");
	}

	@BleachSubscribe
	public void onEntityRender(EventEntityRender.Single.Pre event) {
		if (getSetting(0).asMode().getMode() != 0)
			return;

		int[] color = getColorForEntity(event.getEntity());

		if (color != null) {
			event.setVertex(colorVertexer.createDualProvider(event.getVertex(), color[0], color[1], color[2], getSetting(1).asSlider().getValueInt()));
		}
	}

	@BleachSubscribe
	public void onWorldRender(EventWorldRender.Post event) {
		if (getSetting(0).asMode().getMode() == 0) {
			// Manually render blockentities because of culling
			for (BlockEntity be: WorldUtils.getBlockEntities()) {
				int[] color = getColorForBlock(be);

				if (color != null) {
					// TODO(26.2): manual block-entity rendering is gone — BlockEntityRenderer is now a
					// createRenderState/extractRenderState/submit pipeline and the custom shader pipeline
					// (ShaderEffectWrapper/ColorVertexConsumerProvider) is stubbed inert, so shader mode
					// currently draws nothing for block entities.
				}
			}

			colorVertexer.draw();
			shader.render();
			shader.drawFramebufferToMain("main");
		} else {
			float width = getSetting(2).asSlider().getValueFloat();
			int fill = getSetting(3).asSlider().getValueInt();

			for (Entity e: mc.level.entitiesForRendering()) {
				int[] color = getColorForEntity(e);
				AABB box = e.getBoundingBox();

				if (e instanceof ItemFrame && ((ItemFrame) e).getItem().is(Items.FILLED_MAP)) {
					Axis axis = e.getDirection().getAxis();
					box = box.inflate(axis == Axis.X ? 0 : 0.125, axis == Axis.Y ? 0 : 0.125, axis == Axis.Z ? 0 : 0.125);
				}

				if (color != null) {
					if (width != 0)
						Renderer.drawBoxOutline(box, QuadColor.single(color[0], color[1], color[2], 255), width);

					if (fill != 0)
						Renderer.drawBoxFill(box, QuadColor.single(color[0], color[1], color[2], fill));
				}
			}

			Set<BlockPos> skip = new HashSet<>();
			for (BlockEntity be: WorldUtils.getBlockEntities()) {
				if (skip.contains(be.getBlockPos()))
					continue;

				int[] color = getColorForBlock(be);
				AABB box = be.getBlockState().getShape(mc.level, be.getBlockPos()).bounds().move(be.getBlockPos());

				Direction dir = getChestDirection(be);
				if (dir != null) {
					box = Boxes.stretch(box, dir, 0.94);
					skip.add(be.getBlockPos().relative(dir));
				}

				if (color != null) {
					if (width != 0)
						Renderer.drawBoxOutline(box, QuadColor.single(color[0], color[1], color[2], 255), width);

					if (fill != 0)
						Renderer.drawBoxFill(box, QuadColor.single(color[0], color[1], color[2], fill));
				}
			}
		}
	}

	private int[] getColorForBlock(BlockEntity be) {
		if ((be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) && getSetting(4).asToggle().getState()) {
			return new int[] { 255, 155, 75 };
		} else if (be instanceof EnderChestBlockEntity && getSetting(5).asToggle().getState()) {
			return new int[] { 255, 13, 255 };
		} else if (be instanceof AbstractFurnaceBlockEntity && getSetting(6).asToggle().getState()) {
			return new int[] { 128, 128, 128 };
		} else if (be instanceof DispenserBlockEntity && getSetting(7).asToggle().getState()) {
			return new int[] { 140, 140, 178 };
		} else if (be instanceof HopperBlockEntity && getSetting(8).asToggle().getState()) {
			return new int[] { 115, 115, 155 };
		} else if (be instanceof ShulkerBoxBlockEntity && getSetting(9).asToggle().getState()) {
			return new int[] { 128, 50, 255 };
		} else if (be instanceof BrewingStandBlockEntity && getSetting(10).asToggle().getState()) {
			return new int[] { 128, 100, 50 };
		}

		return null;
	}

	private int[] getColorForEntity(Entity e) {
		if (e instanceof MinecartChest && getSetting(11).asToggle().getState()) {
			return new int[] { 255, 165, 75 };
		} else if (e instanceof MinecartFurnace && getSetting(12).asToggle().getState()) {
			return new int[] { 128, 128, 128 };
		} else if (e instanceof MinecartHopper && getSetting(13).asToggle().getState()) {
			return new int[] { 115, 115, 155 };
		} else if (e instanceof ItemFrame && getSetting(14).asToggle().getState()) {
			if (((ItemFrame) e).getItem().isEmpty()) {
				return new int[] { 115, 25, 25 };
			} else if (((ItemFrame) e).getItem().is(Items.FILLED_MAP)) {
				return new int[] { 25, 25, 128 };
			} else {
				return new int[] { 25, 115, 25 };
			}
		}

		return null;
	}

	/** returns the direction of the other chest if its linked, otherwise null **/
	private Direction getChestDirection(BlockEntity entity) {
		if (entity instanceof ChestBlockEntity && entity.getBlockState().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			return ChestBlock.getConnectedDirection(entity.getBlockState());
		}

		return null;
	}
}