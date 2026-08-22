package org.bleachhack.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventRenderBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTypes.class)
public class MixinRenderLayers {

	// TODO(26.2): RenderTypes.getBlockLayer(BlockState) is gone. Block chunk render layer is no
	// longer selected by a static BlockState->RenderType method; it's baked per-quad at model-bake
	// time (ChunkSectionLayer.byTransparency in BakedQuad.MaterialInfo). No single injectable target
	// exists, so EventRenderBlock.Layer can't be posted here anymore. Disabled to let the mixin apply.
	/*
	@Inject(method = "getBlockLayer", at = @At("HEAD"), cancellable = true)
	private static void getBlockLayer(BlockState state, CallbackInfoReturnable<RenderType> callback) {
		EventRenderBlock.Layer event = new EventRenderBlock.Layer(state);
		BleachHack.eventBus.post(event);

		if (event.getLayer() != null)
			callback.setReturnValue(event.getLayer());
	}
	*/
}
