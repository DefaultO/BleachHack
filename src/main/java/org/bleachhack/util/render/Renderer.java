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
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bleachhack.util.render.color.LineColor;
import org.bleachhack.util.render.color.QuadColor;

/**
 * World-space overlay drawing, backed by the 26.2 Gizmos API.
 *
 * Calls only work while a gizmo collector is active (i.e. from the client
 * tick or the world-render events) - same places these were called before.
 *
 * ponytail: gizmos take one color per shape, so per-vertex QuadColor/LineColor
 * gradients collapse to the first vertex color. Rebuild on a custom vertex
 * pipeline if gradient ESP matters again.
 */
public class Renderer {

	// -------------------- Fill + Outline Boxes --------------------

	public static void drawBoxBoth(BlockPos blockPos, QuadColor color, float lineWidth, Direction... excludeDirs) {
		drawBoxBoth(new AABB(blockPos), color, lineWidth, excludeDirs);
	}

	public static void drawBoxBoth(AABB box, QuadColor color, float lineWidth, Direction... excludeDirs) {
		QuadColor outlineColor = color.clone();
		outlineColor.overwriteAlpha(255);

		drawBoxBoth(box, color, outlineColor, lineWidth, excludeDirs);
	}

	public static void drawBoxBoth(BlockPos blockPos, QuadColor fillColor, QuadColor outlineColor, float lineWidth, Direction... excludeDirs) {
		drawBoxBoth(new AABB(blockPos), fillColor, outlineColor, lineWidth, excludeDirs);
	}

	public static void drawBoxBoth(AABB box, QuadColor fillColor, QuadColor outlineColor, float lineWidth, Direction... excludeDirs) {
		drawBoxFill(box, fillColor, excludeDirs);
		drawBoxOutline(box, outlineColor, lineWidth, excludeDirs);
	}

	// -------------------- Fill Boxes --------------------

	public static void drawBoxFill(BlockPos blockPos, QuadColor color, Direction... excludeDirs) {
		drawBoxFill(new AABB(blockPos), color, excludeDirs);
	}

	public static void drawBoxFill(AABB box, QuadColor color, Direction... excludeDirs) {
		drawBoxFill(box, color, false, excludeDirs);
	}

	/** @param throughWalls draw over terrain and fluids instead of being depth-tested. */
	public static void drawBoxFill(AABB box, QuadColor color, boolean throughWalls, Direction... excludeDirs) {
		if (!FrustumUtils.isBoxVisible(box)) {
			return;
		}

		GizmoStyle style = GizmoStyle.fill(argb(color));

		if (excludeDirs.length == 0) {
			onTop(Gizmos.cuboid(box, style), throughWalls);
			return;
		}

		Vec3 min = new Vec3(box.minX, box.minY, box.minZ);
		Vec3 max = new Vec3(box.maxX, box.maxY, box.maxZ);
		for (Direction dir : Direction.values()) {
			if (!contains(excludeDirs, dir)) {
				onTop(Gizmos.rect(min, max, dir, style), throughWalls);
			}
		}
	}

	// -------------------- Outline Boxes --------------------

	public static void drawBoxOutline(BlockPos blockPos, QuadColor color, float lineWidth, Direction... excludeDirs) {
		drawBoxOutline(new AABB(blockPos), color, lineWidth, excludeDirs);
	}

	public static void drawBoxOutline(AABB box, QuadColor color, float lineWidth, Direction... excludeDirs) {
		drawBoxOutline(box, color, lineWidth, false, excludeDirs);
	}

	/** @param throughWalls draw over terrain and fluids instead of being depth-tested. */
	public static void drawBoxOutline(AABB box, QuadColor color, float lineWidth, boolean throughWalls, Direction... excludeDirs) {
		if (!FrustumUtils.isBoxVisible(box)) {
			return;
		}

		if (excludeDirs.length == 0) {
			onTop(Gizmos.cuboid(box, GizmoStyle.stroke(argb(color), lineWidth)), throughWalls);
			return;
		}

		// ponytail: per-face strokes; edges shared by two included faces draw twice (harmless).
		GizmoStyle style = GizmoStyle.stroke(argb(color), lineWidth);
		Vec3 min = new Vec3(box.minX, box.minY, box.minZ);
		Vec3 max = new Vec3(box.maxX, box.maxY, box.maxZ);
		for (Direction dir : Direction.values()) {
			if (!contains(excludeDirs, dir)) {
				onTop(Gizmos.rect(min, max, dir, style), throughWalls);
			}
		}
	}

	// -------------------- Quads --------------------

	public static void drawQuadFill(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, int cullMode, QuadColor color) {
		if (!FrustumUtils.isPointVisible(x1, y1, z1) && !FrustumUtils.isPointVisible(x2, y2, z2)
				&& !FrustumUtils.isPointVisible(x3, y3, z3) && !FrustumUtils.isPointVisible(x4, y4, z4)) {
			return;
		}

		Gizmos.rect(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), new Vec3(x3, y3, z3), new Vec3(x4, y4, z4), GizmoStyle.fill(argb(color)));
	}

	public static void drawQuadOutline(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, float lineWidth, QuadColor color) {
		if (!FrustumUtils.isPointVisible(x1, y1, z1) && !FrustumUtils.isPointVisible(x2, y2, z2)
				&& !FrustumUtils.isPointVisible(x3, y3, z3) && !FrustumUtils.isPointVisible(x4, y4, z4)) {
			return;
		}

		Gizmos.rect(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), new Vec3(x3, y3, z3), new Vec3(x4, y4, z4), GizmoStyle.stroke(argb(color), lineWidth));
	}

	// -------------------- Lines --------------------

	public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, LineColor color, float width) {
		if (!FrustumUtils.isPointVisible(x1, y1, z1) && !FrustumUtils.isPointVisible(x2, y2, z2)) {
			return;
		}

		// Old pipeline drew lines with depth test disabled - keep them on top.
		Gizmos.line(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), argb(color), width).setAlwaysOnTop();
	}

	// -------------------- Utils --------------------

	public static PoseStack matrixFrom(double x, double y, double z) {
		PoseStack matrices = new PoseStack();

		Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
		matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
		matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));

		matrices.translate(x - camera.position().x, y - camera.position().y, z - camera.position().z);

		return matrices;
	}

	public static Vec3 getInterpolationOffset(Entity e) {
		if (Minecraft.getInstance().isPaused()) {
			return Vec3.ZERO;
		}

		double tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		return new Vec3(
				e.getX() - Mth.lerp(tickDelta, e.xOld, e.getX()),
				e.getY() - Mth.lerp(tickDelta, e.yOld, e.getY()),
				e.getZ() - Mth.lerp(tickDelta, e.zOld, e.getZ()));
	}

	private static int argb(QuadColor color) {
		int[] c = color.getColor(0);
		return ARGB.color(c[3], c[0], c[1], c[2]);
	}

	private static int argb(LineColor color) {
		int[] c = color.getColor(0f, 0f, 0f, 0);
		return ARGB.color(c[3], c[0], c[1], c[2]);
	}

	private static void onTop(GizmoProperties gizmo, boolean throughWalls) {
		if (throughWalls) {
			gizmo.setAlwaysOnTop();
		}
	}

	private static boolean contains(Direction[] dirs, Direction dir) {
		for (Direction d : dirs) {
			if (d == dir) {
				return true;
			}
		}
		return false;
	}
}
