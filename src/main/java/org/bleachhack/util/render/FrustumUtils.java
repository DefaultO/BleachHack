package org.bleachhack.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bleachhack.mixin.AccessorFrustum;
import org.bleachhack.mixin.AccessorWorldRenderer;

public class FrustumUtils {

	public static Frustum getFrustum() {
		return ((AccessorWorldRenderer) Minecraft.getInstance().worldRenderer).getFrustum();
	}

	public static boolean isBoxVisible(AABB box) {
		return getFrustum().isVisible(box);
	}

	public static boolean isPointVisible(Vec3 vec) {
		return isPointVisible(vec.x, vec.y, vec.z);
	}

	public static boolean isPointVisible(double x, double y, double z) {
		AccessorFrustum frustum = (AccessorFrustum) getFrustum();
		return frustum.getFrustumIntersection().testPoint((float) (x - frustum.getX()), (float) (y - frustum.getY()), (float) (z - frustum.getZ()));
	}
}
