/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.dev;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/** Screenshots on demand for {@link DevBridge}, so tests can look at what rendered. */
public class DevScreenshot {

	public static String grab(String name) {
		Minecraft mc = Minecraft.getInstance();
		String file = name.endsWith(".png") ? name : name + ".png";

		// Writes asynchronously - the caller polls for the file.
		Screenshot.grab(mc.gameDirectory, file, mc.gameRenderer.mainRenderTarget(), 1, component -> {});

		return "OK " + new File(new File(mc.gameDirectory, "screenshots"), file).getAbsolutePath();
	}
}
