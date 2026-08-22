/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * TODO(26.2): the old WorldRenderer hooks need re-homing in the extract/submit
 * render architecture. Previously hooked here (see git history on the 1.20.4
 * branch for the implementations):
 * - EventWorldRender.Pre/Post: moved to MixinMinecraftClient#renderFrame (done).
 * - EventEntityRender.Single.Pre (per-entity redirect, Chams/entity mods) and
 *   PreAll/PostAll + EventBlockEntityRender phases (profiler-swap redirect).
 * - EventRenderBlockOutline (block outline redirect).
 * - EventSkyRender.Color.EndSkyColor (end sky color redirect).
 * The events still exist and compile; nothing fires them until re-hooked.
 */
@Mixin(LevelRenderer.class)
public class MixinWorldRenderer {
}
