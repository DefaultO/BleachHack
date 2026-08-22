/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.event.events;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.bleachhack.event.Event;

public class EventClientMove extends Event {

	private MoverType type;
	private Vec3 vec;

	public EventClientMove(MoverType type, Vec3 vec) {
		this.type = type;
		this.vec = vec;
	}

	public MoverType getType() {
		return type;
	}

	public void setType(MoverType type) {
		this.type = type;
	}

	public Vec3 getVec() {
		return vec;
	}

	public void setVec(Vec3 vec) {
		this.vec = vec;
	}


}
