/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;

/**
 * TODO(26.2): the old per-vertex alpha override (Xray opacity slider dimming
 * every rendered block) targeted FixedColorVertexConsumer, which no longer
 * exists - 26.2 BufferBuilder writes packed vertex structs. Xray still hides
 * non-ore blocks via MixinSectionCompiler; only the partial-transparency mode
 * is dormant until this is rebuilt on the new vertex path.
 */
@Mixin(BufferBuilder.class)
public class MixinBufferBuilder {
}
