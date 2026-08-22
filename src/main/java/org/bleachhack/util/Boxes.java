/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;

public class Boxes {

	/** Returns the vector of the min pos of this box. **/
	public static Vec3 getMinVec(AABB box) {
		return new Vec3(box.minX, box.minY, box.minZ);
	}

	/** Returns the vector of the max pos of this box. **/
	public static Vec3 getMaxVec(AABB box) {
		return new Vec3(box.maxX, box.maxY, box.maxZ);
	}

	/** Offsets this box so that minX, minY and minZ are all zero. **/
	public static AABB moveToZero(AABB box) {
		return box.move(getMinVec(box).reverse());
	}

	/** Returns the distance between to oppisite corners of the box. **/
	public static double getCornerLength(AABB box) {
		return getMinVec(box).distanceTo(getMaxVec(box));
	}

	/** Returns the length of an axis in the box. **/
	public static double getAxisLength(AABB box, Axis axis) {
		return box.max(axis) - box.min(axis);
	}

	/** Returns a box with each axis multiplied by the amount specified. **/
	public static AABB multiply(AABB box, double amount) {
		return multiply(box, amount, amount, amount);
	}

	/** Returns a box with each axis multiplied by the amount specified. **/
	public static AABB multiply(AABB box, double x, double y, double z) {
		return box.inflate(
				getAxisLength(box, Axis.X) * (x - 1) / 2d,
				getAxisLength(box, Axis.Y) * (y - 1) / 2d,
				getAxisLength(box, Axis.Z) * (z - 1) / 2d);
	}

	/** Returns a box with one of its sides stretched. **/
	public static AABB stretch(AABB box, Direction dir, double length) {
		return switch (dir) {
			case DOWN -> new AABB(box.minX, box.minY - length, box.minZ, box.maxX, box.maxY, box.maxZ);
			case UP -> new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY + length, box.maxZ);
			case NORTH -> new AABB(box.minX, box.minY, box.minZ - length, box.maxX, box.maxY, box.maxZ);
			case SOUTH -> new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ + length);
			case WEST -> new AABB(box.minX - length, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
			case EAST -> new AABB(box.minX, box.minY, box.minZ, box.maxX + length, box.maxY, box.maxZ);
		};
	}
}
