/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventClientMove;
import org.bleachhack.event.events.EventPacket;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.event.events.EventWorldRender;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.world.WorldUtils;
import org.lwjgl.glfw.GLFW;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.JigsawBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Mode;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class NoSlow extends Module {

	private Vec3 addVelocity = Vec3.ZERO;
	private long lastTime;

	public NoSlow() {
		super("NoSlow", KEY_UNBOUND, ModuleCategory.MOVEMENT, "Disables Stuff From Slowing You Down.",
				new SettingToggle("Slowness", true).withDesc("Removes the slowness effect."),
				new SettingToggle("SoulSand", true).withDesc("Removes soul sand slowness."),
				new SettingToggle("SlimeBlocks", true).withDesc("Removes slimeblock slowness."),
				new SettingToggle("Webs", true).withDesc("Removes cobweb slowness."),
				new SettingToggle("Berry Bush", true).withDesc("Removes berry bush slowness."),
				new SettingToggle("Items", true).withDesc("Removes the slowness while eating items."),
				new SettingToggle("Inventory", true).withDesc("Allows you to move while in inventories.").withChildren(
						new SettingToggle("Sneaking", false).withDesc("Enables the sneak key while in inventories."),
						new SettingToggle("NCPBypass", false).withDesc("Allows you to move items around on serves with NCP."),
						new SettingToggle("Rotate", true).withDesc("Allows you to use the arrow keys to rotate.").withChildren(
								new SettingToggle("PitchLimit", true).withDesc("Prevents you from being able to do a 360 pitch spin."),
								new SettingToggle("Anti-Spinbot", true).withDesc("Adds a random amount of rotation when spinning to prevent spinbot detects."))));
	}

	@BleachSubscribe
	public void onClientMove(EventClientMove event) {
		if (!isEnabled())
			return;

		/* Slowness */
		if (getSetting(0).asToggle().getState() && (mc.player.getStatusEffect(MobEffects.SLOWNESS) != null || mc.player.getStatusEffect(MobEffects.BLINDNESS) != null)) {
			if (mc.options.forwardKey.isPressed()
					&& mc.player.getVelocity().x > -0.15 && mc.player.getVelocity().x < 0.15
					&& mc.player.getVelocity().z > -0.15 && mc.player.getVelocity().z < 0.15) {
				mc.player.setVelocity(mc.player.getVelocity().add(addVelocity));
				addVelocity = addVelocity.add(new Vec3(0, 0, 0.05).rotateY(-(float) Math.toRadians(mc.player.getYaw())));
			} else {
				addVelocity = addVelocity.multiply(0.75, 0.75, 0.75);
			}
		}

		/* Soul Sand */
		if (getSetting(1).asToggle().getState() && mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.SOUL_SAND) {
			mc.player.setVelocity(mc.player.getVelocity().multiply(2.5, 1, 2.5));
		}

		/* Slime Block */
		if (getSetting(2).asToggle().getState()
				&& mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(0, -0.01, 0))).getBlock() == Blocks.SLIME_BLOCK && mc.player.isOnGround()) {
			double d = Math.abs(mc.player.getVelocity().y);
			if (d < 0.1D && !mc.player.bypassesSteppingEffects()) {
				double e = 1 / (0.4D + d * 0.2D);
				mc.player.setVelocity(mc.player.getVelocity().multiply(e, 1.0D, e));
			}
		}

		/* Web */
		if (getSetting(3).asToggle().getState() && WorldUtils.doesBoxTouchBlock(mc.player.getBoundingBox(), Blocks.COBWEB)) {
			// still kinda scuffed until i get an actual mixin
			mc.player.slowMovement(mc.world.getBlockState(mc.player.getBlockPos()), new Vec3(1.75, 1.75, 1.75));
		}

		/* Berry Bush */
		if (getSetting(4).asToggle().getState() && WorldUtils.doesBoxTouchBlock(mc.player.getBoundingBox(), Blocks.SWEET_BERRY_BUSH)) {
			// also scuffed
			mc.player.slowMovement(mc.world.getBlockState(mc.player.getBlockPos()), new Vec3(1.7, 1.7, 1.7));
		}

		// Items handled in MixinPlayerEntity:sendMovementPackets_isUsingItem
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		/* Inventory */
		if (getSetting(6).asToggle().getState() && shouldInvMove(mc.currentScreen)) {

			for (KeyMapping k : new KeyMapping[] { mc.options.forwardKey, mc.options.backKey,
					mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey, mc.options.sprintKey }) {
				k.setPressed(InputConstants.isKeyPressed(mc.getWindow().getHandle(),
						InputConstants.fromTranslationKey(k.getBoundKeyTranslationKey()).getCode()));
			}

			if (getSetting(6).asToggle().asToggle().getChild(0).asToggle().getState()) {
				mc.options.sneakKey.setPressed(InputConstants.isKeyPressed(mc.getWindow().getHandle(),
						InputConstants.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode()));
			}


		}
	}


	@BleachSubscribe
	public void onRender(EventWorldRender.Post event) {
		/* Inventory */
		if (getSetting(6).asToggle().getState()
				&& getSetting(6).asToggle().asToggle().getChild(2).asToggle().getState()
				&& shouldInvMove(mc.currentScreen)) {

			float yaw = 0f;
			float pitch = 0f;

			//mc.keyboard.setRepeatEvents(true);

			float amount = (System.currentTimeMillis() - lastTime) / 10f;
			lastTime = System.currentTimeMillis();

			if (InputConstants.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT))
				yaw -= amount;
			if (InputConstants.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT))
				yaw += amount;
			if (InputConstants.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_UP))
				pitch -= amount;
			if (InputConstants.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_DOWN))
				pitch += amount;

			if (getSetting(6).asToggle().asToggle().getChild(2).asToggle().asToggle().getChild(1).asToggle().getState()) {
				if (yaw == 0f && pitch != 0f) {
					yaw += -0.1 + Math.random() / 5f;
				} else {
					yaw *= 0.75f + Math.random() / 2f;
				}

				if (pitch == 0f && yaw != 0f) {
					pitch += -0.1 + Math.random() / 5f;
				} else {
					pitch *= 0.75f + Math.random() / 2f;
				}
			}


			mc.player.setYaw(mc.player.getYaw() + yaw);

			if (getSetting(6).asToggle().asToggle().getChild(2).asToggle().asToggle().getChild(0).asToggle().getState()) {
				mc.player.setPitch(Mth.clamp(mc.player.getPitch() + pitch, -90f, 90f));
			} else {
				mc.player.setPitch(mc.player.getPitch() + pitch);
			}
		}
	}

	@BleachSubscribe
	public void onSendPacket(EventPacket.Send event) {
		if (event.getPacket() instanceof ServerboundContainerClickPacket && getSetting(6).asToggle().asToggle().getChild(1).asToggle().getState()) {
			mc.player.networkHandler.sendPacket(new ServerboundPlayerCommandPacket(mc.player, Mode.STOP_SPRINTING));
		}
	}

	private boolean shouldInvMove(Screen screen) {
		if (screen == null) {
			return false;
		}

		return !(screen instanceof ChatScreen
				|| screen instanceof BookEditScreen
				|| screen instanceof SignEditScreen
				|| screen instanceof JigsawBlockEditScreen
				|| screen instanceof StructureBlockEditScreen
				|| screen instanceof AnvilScreen
				|| screen instanceof CreativeModeInventoryScreen);
	}
}
