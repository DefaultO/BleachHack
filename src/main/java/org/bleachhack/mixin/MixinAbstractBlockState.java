package org.bleachhack.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventRenderBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class MixinAbstractBlockState {

	// 26.2: AbstractBlockState -> BlockStateBase, isOpaque -> canOcclude
	@Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
	private void canOcclude(CallbackInfoReturnable<Boolean> callback) {
		EventRenderBlock.Opaque event = new EventRenderBlock.Opaque((BlockState) (Object) this);
		BleachHack.eventBus.post(event);

		if (event.isOpaque() != null)
			callback.setReturnValue(event.isOpaque());
	}
}
