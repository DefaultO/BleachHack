/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventRenderShader;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.NoRender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

	@Shadow @Final private Minecraft minecraft;
	@Shadow private Identifier postEffectId;
	@Shadow private boolean effectActive;
	@Shadow @Final private RenderTarget mainRenderTarget;
	@Shadow @Final private CrossFrameResourcePool resourcePool;

	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void onBobHurt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
		if (ModuleManager.getModule(NoRender.class).isOverlayToggled(2)) {
			ci.cancel();
		}
	}

	@Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
	private void displayItemActivation(ItemStack floatingItem, CallbackInfo ci) {
		if (ModuleManager.getModule(NoRender.class).isWorldToggled(1) && floatingItem.getItem() == Items.TOTEM_OF_UNDYING) {
			ci.cancel();
		}
	}

	// TODO(26.2): the old nausea wobble lerp was split into a portal intensity lerp and a nausea effect
	// blend factor - zero both to keep the "Wobble" NoRender toggle working.
	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F", ordinal = 0),
			require = 0 /* TODO: meteor compatibility */)
	private float nauseaWobble(float delta, float first, float second) {
		if (ModuleManager.getModule(NoRender.class).isOverlayToggled(5)) {
			return 0;
		}

		return Mth.lerp(delta, first, second);
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"),
			require = 0 /* TODO: meteor compatibility */)
	private float nauseaBlendFactor(LocalPlayer player, Holder<MobEffect> effect, float partialTicks) {
		if (ModuleManager.getModule(NoRender.class).isOverlayToggled(5)) {
			return 0;
		}

		return player.getEffectBlendFactor(effect, partialTicks);
	}

	// TODO(26.2): GameRenderer no longer stores a PostChain (postProcessor); it stores an Identifier and
	// resolves the PostChain through the ShaderManager each frame. This redirects the postEffectId null
	// check in render() - the vanilla chain (if any) is resolved and passed to the event like before, the
	// event's effect is processed manually, and null is returned so vanilla skips its own post pass.
	@Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;postEffectId:Lnet/minecraft/resources/Identifier;", ordinal = 0))
	private Identifier render_Shader(GameRenderer renderer) {
		PostChain vanillaChain = postEffectId != null && effectActive
				? minecraft.getShaderManager().getPostChain(postEffectId, LevelTargetBundle.MAIN_TARGETS)
				: null;

		EventRenderShader event = new EventRenderShader(vanillaChain);
		BleachHack.eventBus.post(event);

		if (event.getEffect() != null) {
			event.getEffect().process(mainRenderTarget, resourcePool);
		}

		return null;
	}
}
