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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.*;
import org.bleachhack.util.BleachLogger;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

	@Shadow private void drawBlockOutline(PoseStack matrices, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState) {}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/ProfilerFiller;swap(Ljava/lang/String;)V"))
	private void render_swap(ProfilerFiller profiler, String string) {
		profiler.swap(string);

		if (string.equals("entities")) {
			BleachHack.eventBus.post(new EventEntityRender.PreAll());
		} else if (string.equals("blockentities")) {
			BleachHack.eventBus.post(new EventEntityRender.PostAll());
			BleachHack.eventBus.post(new EventBlockEntityRender.PreAll());
		} else if (string.equals("destroyProgress")) {
			BleachHack.eventBus.post(new EventBlockEntityRender.PostAll());
		}
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void render_head(PoseStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
							 LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo callback) {
		EventWorldRender.Pre event = new EventWorldRender.Pre(tickDelta, matrices);
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			callback.cancel();
		}
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void render_return(PoseStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
			LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo callback) {
		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.IS_SYSTEM_MAC);
		EventWorldRender.Post event = new EventWorldRender.Post(tickDelta, matrices);
		BleachHack.eventBus.post(event);
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawBlockOutline(Lnet/minecraft/client/util/math/PoseStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/entity/Entity;DDDLnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"))
	private void render_drawBlockOutline(WorldRenderer worldRenderer, PoseStack matrices, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState) {
		EventRenderBlockOutline event = new EventRenderBlockOutline(matrices, vertexConsumer, blockPos, blockState);
		BleachHack.eventBus.post(event);

		if (!event.isCancelled()) {
			drawBlockOutline(event.getMatrices(), event.getVertexConsumer(), entity, d, e, f, event.getPos(), event.getState());
		}
	}

	@Unique
	private static boolean SODIUM_INSTALLED = FabricLoader.getInstance().isModLoaded("sodium");

	@Redirect(method = "renderEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/PoseStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
	private <E extends Entity> void renderEntity_render(EntityRenderDispatcher dispatcher, E entity, double x, double y, double z, float yaw, float tickDelta, PoseStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		EventEntityRender.Single.Pre event = new EventEntityRender.Single.Pre(entity, matrices, vertexConsumers);
		BleachHack.eventBus.post(event);

		if (!event.isCancelled()) {
			try {
				dispatcher.render(event.getEntity(), x, y, z, yaw, tickDelta, event.getMatrix(), SODIUM_INSTALLED ? vertexConsumers : event.getVertex(), light);
			} catch (Exception e) {
				BleachLogger.error("Disabling Entity Rendering Mixin, another mod conflicting?");
				e.printStackTrace();
				SODIUM_INSTALLED = true;
			}
		}
	}

	@Redirect(method = "renderEndSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;color(IIII)Lnet/minecraft/client/render/VertexConsumer;"))
	private VertexConsumer renderEndSky_color(VertexConsumer vertexConsumer, int red, int green, int blue, int alpha) {
		EventSkyRender.Color.EndSkyColor event = new EventSkyRender.Color.EndSkyColor(1f);
		BleachHack.eventBus.post(event);

		if (event.getColor() != null) {
			return vertexConsumer.color(
					(int) (event.getColor().x * 255), (int) (event.getColor().y * 255), (int) (event.getColor().z * 255), (int) alpha);
		} else {
			return vertexConsumer.color(red, green, blue, alpha);
		}
	}

}
