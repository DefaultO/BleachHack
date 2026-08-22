/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import com.mojang.math.Axis;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.joml.Vector3f;

public class WorldRenderer {

	private static final Minecraft mc = Minecraft.getInstance();

	// A Pointer to RenderSystem.shaderLightDirections
	private static final Vector3f[] shaderLight;

	static {
		try {
			shaderLight = (Vector3f[]) FieldUtils.getField(RenderSystem.class, "shaderLightDirections", true).get(null);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/** Draws text in the world. **/
	public static void drawText(Component text, double x, double y, double z, double scale, boolean shadow) {
		drawText(text, x, y, z, 0, 0, scale, shadow);
	}

	/** Draws text in the world. **/
	public static void drawText(Component text, double x, double y, double z, double offX, double offY, double scale, boolean fill) {
		PoseStack matrices = matrixFrom(x, y, z);

		Camera camera = mc.gameRenderer.getCamera();
		matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
		matrices.multiply(Axis.POSITIVE_X.rotationDegrees(camera.getPitch()));

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		matrices.translate(offX, offY, 0);
		matrices.scale(-0.025f * (float) scale, -0.025f * (float) scale, 1);

		int halfWidth = mc.textRenderer.getWidth(text) / 2;

		VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

		if (fill) {
			int opacity = (int) (Minecraft.getInstance().options.getTextBackgroundOpacity(0.25F) * 255.0F) << 24;
			mc.textRenderer.draw(text, -halfWidth, 0f, 553648127, false, matrices.peek().getPositionMatrix(), immediate, Font.TextLayerType.NORMAL, opacity, 0xf000f0);
			immediate.draw();
		} else {
			matrices.push();
			matrices.translate(1, 1, 0);
			mc.textRenderer.draw(text.copy(), -halfWidth, 0f, 0x202020, false, matrices.peek().getPositionMatrix(), immediate, Font.TextLayerType.NORMAL, 0, 0xf000f0);
			immediate.draw();
			matrices.pop();
		}

		mc.textRenderer.draw(text, -halfWidth, 0f, -1, false, matrices.peek().getPositionMatrix(), immediate, Font.TextLayerType.NORMAL, 0, 0xf000f0);
		immediate.draw();

		RenderSystem.disableBlend();
	}

	/** Draws a 2D gui items somewhere in the world. **/
	public static void drawGuiItem(double x, double y, double z, double offX, double offY, double scale, ItemStack item) {
		if (item.isEmpty()) {
			return;
		}

		PoseStack matrices = matrixFrom(x, y, z);

		Camera camera = mc.gameRenderer.getCamera();
		matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
		matrices.multiply(Axis.POSITIVE_X.rotationDegrees(camera.getPitch()));

		matrices.translate(offX, offY, 0);
		matrices.scale((float) scale, (float) scale, 0.001f);

		matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(180f));

		mc.getBufferBuilders().getEntityVertexConsumers().draw();
		
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		Vector3f[] currentLight = shaderLight.clone();
		DiffuseLighting.disableGuiDepthLighting();

		mc.getItemRenderer().renderItem(item, ModelTransformationMode.GUI, 0xF000F0,
				OverlayTexture.DEFAULT_UV, matrices, mc.getBufferBuilders().getEntityVertexConsumers(), mc.world, 0);

		mc.getBufferBuilders().getEntityVertexConsumers().draw();

		RenderSystem.setShaderLights(currentLight[0], currentLight[1]);
		RenderSystem.disableBlend();
	}

	public static PoseStack matrixFrom(double x, double y, double z) {
		PoseStack matrices = new PoseStack();

		Camera camera = mc.gameRenderer.getCamera();
		matrices.multiply(Axis.POSITIVE_X.rotationDegrees(camera.getPitch()));
		matrices.multiply(Axis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

		matrices.translate(x - camera.getPos().x, y - camera.getPos().y, z - camera.getPos().z);

		return matrices;
	}
}
