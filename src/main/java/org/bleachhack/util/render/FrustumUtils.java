/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bleachhack.mixin.AccessorFrustum;

public class FrustumUtils {

	public static Frustum getFrustum() {
		return Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum();
	}

	public static boolean isBoxVisible(AABB box) {
		return getFrustum().isVisible(box);
	}

	public static boolean isPointVisible(Vec3 vec) {
		return isPointVisible(vec.x, vec.y, vec.z);
	}

	public static boolean isPointVisible(double x, double y, double z) {
		AccessorFrustum frustum = (AccessorFrustum) getFrustum();
		return frustum.getIntersection().testPoint((float) (x - frustum.getCamX()), (float) (y - frustum.getCamY()), (float) (z - frustum.getCamZ()));
	}
}
