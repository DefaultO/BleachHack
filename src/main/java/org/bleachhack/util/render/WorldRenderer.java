/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * World-space text/item billboards, backed by the 26.2 Gizmos API.
 *
 * ponytail: Gizmos.billboardText renders a plain single-color string, so
 * Component styling collapses to getString(); rebuild on the submit pipeline
 * if colored nametag parts matter again.
 */
public class WorldRenderer {

	private static final Minecraft mc = Minecraft.getInstance();

	// Rough parity between the old 0.025-per-pixel billboard scale and the gizmo text scale.
	private static final float SCALE_BASE = TextGizmo.Style.DEFAULT_SCALE;

	/** Draws text in the world. **/
	public static void drawText(Component text, double x, double y, double z, double scale, boolean shadow) {
		drawText(text, x, y, z, 0, 0, scale, shadow);
	}

	/** Draws text in the world. **/
	public static void drawText(Component text, double x, double y, double z, double offX, double offY, double scale, boolean fill) {
		// offX/offY were applied in billboard (text-pixel) space; approximate with
		// camera-relative right/up offsets so labels keep their relative placement.
		Vec3 pos = new Vec3(x, y, z).add(billboardOffset(offX, offY, scale));
		Gizmos.billboardText(text.getString(), pos, TextGizmo.Style.forColorAndCentered(-1).withScale(SCALE_BASE * (float) scale)).setAlwaysOnTop();
	}

	/** Draws a 2D gui item somewhere in the world. **/
	public static void drawGuiItem(double x, double y, double z, double offX, double offY, double scale, ItemStack item) {
		if (item.isEmpty()) {
			return;
		}

		// TODO(26.2): floating GUI-item billboards need the new item submit pipeline
		// (the old ItemRenderer/BufferBuilders immediate path is gone). Until then,
		// show the item name + count so the feature stays informative.
		Component label = item.getCount() > 1
				? Component.literal(item.getCount() + "x ").append(item.getHoverName())
				: item.getHoverName();
		drawText(label, x, y, z, offX, offY, scale * 0.5, true);
	}

	public static PoseStack matrixFrom(double x, double y, double z) {
		return Renderer.matrixFrom(x, y, z);
	}

	private static Vec3 billboardOffset(double offX, double offY, double scale) {
		if (offX == 0 && offY == 0) {
			return Vec3.ZERO;
		}

		Camera camera = mc.gameRenderer.mainCamera();
		float yawRad = camera.yRot() * Mth.DEG_TO_RAD;
		Vec3 right = new Vec3(-Mth.cos(yawRad), 0, -Mth.sin(yawRad));
		double unit = 0.025 * scale;
		return right.scale(-offX * unit).add(0, -offY * unit, 0);
	}
}
