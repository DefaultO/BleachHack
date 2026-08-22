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
    @ModifyArg(method = "readNbt()Lnet/minecraft/nbt/NbtCompound;", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;readNbt(Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/NbtElement;"))
    private NbtAccounter increaseLimit(NbtAccounter in) {
        return ModuleManager.getModule(AntiChunkBan.class).isEnabled() ? NbtAccounter.ofUnlimitedBytes() : in;
    }
}
