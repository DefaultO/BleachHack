/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.event.events;

import net.minecraft.world.phys.Vec3;
import org.bleachhack.event.Event;

public class EventSkyRender extends Event {

	// TODO(26.2): DimensionEffects (yarn) / DimensionSpecialEffects no longer exists. Sky rendering was
	// split into SkyRenderer + DimensionType.Skybox + EnvironmentAttributes; there is no drop-in
	// replacement type. This Properties holder (and its producers MixinClientWorld/MixinDimensionEffects
	// and consumer Ambience, which subclasses DimensionEffects) needs a coordinated rewrite.
	// Typed as Object to keep the holder shape intact without gutting the feature.
	public static class Properties extends EventSkyRender {

		private Object sky;

		public Properties(Object sky) {
			this.setSky(sky);
		}

		public Object getSky() {
			return sky;
		}

		public void setSky(Object sky) {
			this.sky = sky;
		}
	}

	public static class Color extends EventSkyRender {

		private float tickDelta;
		private Vec3 color = null;

		public Color(float tickDelta) {
			this.tickDelta = tickDelta;
		}

		public float getTickDelta() {
			return tickDelta;
		}

		public void setColor(Vec3 color) {
			this.color = color;
		}

		public Vec3 getColor() {
			return color;
		}

		public static class SkyColor extends Color {

			public SkyColor(float tickDelta) {
				super(tickDelta);
			}
		}

		public static class CloudColor extends Color {

			public CloudColor(float tickDelta) {
				super(tickDelta);
			}
		}

		public static class FogColor extends Color {

			public FogColor(float tickDelta) {
				super(tickDelta);
			}
		}

		public static class EndSkyColor extends Color {

			public EndSkyColor(float tickDelta) {
				super(tickDelta);
			}
		}
	}
}
