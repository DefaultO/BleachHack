/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.renderer.LevelTargetBundle;
import org.bleachhack.event.events.EventRenderShader;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;

import net.minecraft.resources.Identifier;

public class ShaderRender extends Module {

	private List<Identifier> shaders = new ArrayList<>();

	public ShaderRender() {
		super("ShaderRender", KEY_UNBOUND, ModuleCategory.RENDER, "1.7 Super secret settings.",
				new SettingMode("Shader", "Notch", "FXAA", "Art", "Bumpy", "Blobs", "Blobs2", "Pencil", "Vibrant",
						"Deconverge", "Flip", "Invert", "NTSC", "Outline", "Phosphor", "Scanline", "Sobel",
						"Bits", "Desaturate", "Green", "Blur", "Wobble", "Antialias", "Creeper", "Spider").withDesc("Shader to use."));
		
		for (String s: getSetting(0).asMode().modes) {
			if (s.equals("Vibrant")) {
				shaders.add(Identifier.withDefaultNamespace("color_convolve"));
			} else if (s.equals("Scanline")) {
				shaders.add(Identifier.withDefaultNamespace("scan_pincushion"));
			} else {
				shaders.add(Identifier.withDefaultNamespace(s.toLowerCase(Locale.ENGLISH)));
			}
		}
	}

	@BleachSubscribe
	public void onWorldRender(EventRenderShader event) {
		// TODO(26.2): post effects are data-driven (post_effect/<id>.json) and loaded/cached by the
		// ShaderManager; vanilla only ships a few of the old 1.7 shaders (e.g. creeper, spider, invert),
		// missing ones resolve to null which simply disables the effect.
		event.setEffect(mc.getShaderManager().getPostChain(shaders.get(getSetting(0).asMode().getMode()), LevelTargetBundle.MAIN_TARGETS));
	}

}
