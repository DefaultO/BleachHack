/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.mixin;

import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventClientMove;
import org.bleachhack.event.events.EventSendMovementPackets;
import org.bleachhack.event.events.EventSwingHand;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.module.mods.BetterPortal;
import org.bleachhack.module.mods.EntityControl;
import org.bleachhack.module.mods.Freecam;
import org.bleachhack.module.mods.NoSlow;
import org.bleachhack.module.mods.SafeWalk;
import org.bleachhack.module.mods.Scaffold;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

@Mixin(LocalPlayer.class)
public class MixinClientPlayerEntity extends AbstractClientPlayer {

	@Shadow private float jumpRidingScale;

	@Shadow @Final public ClientPacketListener connection;

	private MixinClientPlayerEntity(ClientLevel world, GameProfile profile) {
		super(world, profile);
	}

	@Shadow protected void updateAutoJump(float dx, float dz) {}

	// 26.2: sendMovementPackets -> sendPosition
	// TODO(26.2): vehicle/input packets are now sent inline in LocalPlayer.tick(), so cancelling this
	// event only suppresses on-foot movement packets, not vehicle-move packets.
	@Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
	private void sendPosition(CallbackInfo info) {
		EventSendMovementPackets event = new EventSendMovementPackets();
		BleachHack.eventBus.post(event);

		if (event.isCancelled()) {
			info.cancel();
		}
	}

	// 26.2: item-use slowdown moved from tickMovement/aiStep to modifyInput
	@Redirect(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"),
			require = 0 /* TODO: meteor compatibility */)
	private boolean modifyInput_isUsingItem(LocalPlayer player) {
		NoSlow noSlow = ModuleManager.getModule(NoSlow.class);
		if (noSlow.isEnabled() && noSlow.getSetting(5).asToggle().getState())
			return false;

		return player.isUsingItem();
	}

	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	private void move(MoverType type, Vec3 movement, CallbackInfo info) {
		EventClientMove event = new EventClientMove(type, movement);
		BleachHack.eventBus.post(event);
		if (event.isCancelled()) {
			info.cancel();
		} else if (!type.equals(event.getType()) || !movement.equals(event.getVec())) {
			double double_1 = this.getX();
			double double_2 = this.getZ();
			super.move(event.getType(), event.getVec());
			float dx = (float) (this.getX() - double_1);
			float dz = (float) (this.getZ() - double_2);
			this.updateAutoJump(dx, dz);
			this.addWalkedDistance(Mth.length(dx, dz) * 0.6F);
			info.cancel();
		}
	}

	// 26.2: pushOutOfBlocks -> moveTowardsClosestSpace
	@Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
	private void pushOutOfBlocks(double x, double d, CallbackInfo ci) {
		if (ModuleManager.getModule(Freecam.class).isEnabled()) {
			ci.cancel();
		}
	}

	// 26.2: updateNausea -> handlePortalTransitionEffect, closeHandledScreen -> closeContainer
	@Redirect(method = "handlePortalTransitionEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;closeContainer()V", ordinal = 0),
			require = 0 /* TODO: inertia compatibility */)
	private void handlePortalTransitionEffect_closeContainer(LocalPlayer player) {
		if (!ModuleManager.getModule(BetterPortal.class).isEnabled()
				|| !ModuleManager.getModule(BetterPortal.class).getSetting(0).asToggle().getState()) {
			closeContainer();
		}
	}

	// 26.2: screens are set via Minecraft.gui (Gui.setScreen) now
	@Redirect(method = "handlePortalTransitionEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 0),
			require = 0 /* TODO: inertia compatibility */)
	private void handlePortalTransitionEffect_setScreen(Gui gui, Screen screen) {
		if (!ModuleManager.getModule(BetterPortal.class).isEnabled()
				|| !ModuleManager.getModule(BetterPortal.class).getSetting(0).asToggle().getState()) {
			gui.setScreen(screen);
		}
	}

	// 26.2: swingHand -> swing
	@Overwrite
	public void swing(InteractionHand hand) {
		EventSwingHand event = new EventSwingHand(hand);
		BleachHack.eventBus.post(event);

		if (!event.isCancelled()) {
			super.swing(event.getHand());
		}

		connection.send(new ServerboundSwingPacket(hand));
	}

	// 26.2: clipAtLedge -> isStayingOnGroundSurface
	@Override
	protected boolean isStayingOnGroundSurface() {
		return super.isStayingOnGroundSurface()
				|| ModuleManager.getModule(SafeWalk.class).isEnabled()
				|| (ModuleManager.getModule(Scaffold.class).isEnabled()
						&& ModuleManager.getModule(Scaffold.class).getSetting(8).asToggle().getState());
	}

	// 26.2: getMountJumpStrength -> getJumpRidingScale
	@Overwrite
	public float getJumpRidingScale() {
		return ModuleManager.getModule(EntityControl.class).isEnabled()
				&& ModuleManager.getModule(EntityControl.class).getSetting(2).asToggle().getState() ? 1F : jumpRidingScale;
	}
}
