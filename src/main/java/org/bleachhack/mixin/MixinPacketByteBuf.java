package org.bleachhack.mixin;

import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.AntiChunkBan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FriendlyByteBuf.class)
public class MixinPacketByteBuf {
    // 26.2: the accounter is created in the static readNbt(ByteBuf) overload now; ofUnlimitedBytes -> unlimitedHeap
    @ModifyArg(method = "readNbt(Lio/netty/buffer/ByteBuf;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;readNbt(Lio/netty/buffer/ByteBuf;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/Tag;"))
    private static NbtAccounter increaseLimit(NbtAccounter in) {
        return ModuleManager.getModule(AntiChunkBan.class).isEnabled() ? NbtAccounter.unlimitedHeap() : in;
    }
}
