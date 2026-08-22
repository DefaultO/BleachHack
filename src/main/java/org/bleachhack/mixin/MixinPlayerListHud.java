package org.bleachhack.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.bleachhack.BleachHack;
import org.bleachhack.setting.option.Option;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerListHud {

	@Shadow @Final private Minecraft minecraft;

	// 26.2: getPlayerName -> getNameForDisplay, GameProfile.getName() -> name(), styled -> withStyle
	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void getPlayerName(PlayerInfo entry, CallbackInfoReturnable<Component> callback) {
		if (Option.PLAYERLIST_SHOW_FRIENDS.getValue() && BleachHack.friendMang.has(entry.getProfile().name())) {
			callback.setReturnValue(((MutableComponent) callback.getReturnValue()).withStyle(s -> s.withColor(ChatFormatting.AQUA)));
		}
	}
}
