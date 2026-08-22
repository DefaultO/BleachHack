/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.Mth;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventRenderShader;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

	@Shadow private PostChain postProcessor;

	@Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
	private void onTiltViewWhenHurt(PoseStack matrixStack, float f, CallbackInfo ci) {
		if (ModuleManager.getModule(NoRender.class).isOverlayToggled(2)) {
			ci.cancel();
		}
	}

	@Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
	private void showFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
		if (ModuleManager.getModule(NoRender.class).isWorldToggled(1) && floatingItem.getItem() == Items.TOTEM_OF_UNDYING) {
			ci.cancel();
		}
	}

	@Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Mth;lerp(FFF)F", ordinal = 0),
			require = 0 /* TODO: meteor compatibility */)
	private float nauseaWobble(float delta, float first, float second) {
		if (ModuleManager.getModule(NoRender.class).isOverlayToggled(5)) {
			return 0;
		}

		return Mth.lerp(delta, first, second);
	}

	@Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;postProcessor:Lnet/minecraft/client/gl/PostChain;", ordinal = 0))
	private PostChain render_Shader(GameRenderer renderer, float tickDelta) {
		EventRenderShader event = new EventRenderShader(postProcessor);
		BleachHack.eventBus.post(event);

		if (event.getEffect() != null) {
			RenderSystem.disableBlend();
			RenderSystem.disableDepthTest();
			RenderSystem.resetTextureMatrix();
			event.getEffect().render(tickDelta);
		}

		return null;
	}
}
