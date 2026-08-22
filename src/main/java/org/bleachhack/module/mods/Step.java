/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import java.util.ArrayDeque;
import java.util.Deque;

import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

public class Step extends Module {

	private boolean flag;
	private int lastStep = 0;
	private Deque<Double> queue = new ArrayDeque<>();

	public Step() {
		super("Step", KEY_UNBOUND, ModuleCategory.MOVEMENT, "Allows you to Run up blocks like stairs.",
				new SettingMode("Mode", "Packet", "Vanilla", "Spider", "Jump").withDesc("Step mode."),
				new SettingSlider("Height", 0.1, 20, 2, 1).withDesc("How high to be able to step (Vanilla only)."),
				new SettingToggle("Cooldown", false).withDesc("Adds a cooldown between stepping to prevent rubberbanding.").withChildren(
						new SettingSlider("Amount", 0.01, 1, 0.1, 2).withDesc("How long the cooldown is (in seconds).")));
	}

	@Override
	public void onDisable(boolean inWorld) {
		if (inWorld)
			setStepHeight(0.5F);

		super.onDisable(inWorld);
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		setStepHeight(getSetting(0).asMode().getMode() == 1 ? getSetting(1).asSlider().getValueFloat() : 0.5f);

		if (!mc.player.horizontalCollision) {
			queue.clear();
		}

		if (getSetting(2).asToggle().getState()) {
			if (!(mc.player.tickCount < lastStep || mc.player.tickCount >= lastStep + getSetting(2).asToggle().getChild(0).asSlider().getValue() * 20)) {
				return;
			}
		}

		if (!mc.level.getBlockState(mc.player.blockPosition().offset(0, (int) (mc.player.getBbHeight() + 1), 0)).canBeReplaced()
				|| mc.player.input.keyPresses.jump()
				|| !(mc.player.input.keyPresses.forward() || mc.player.input.keyPresses.backward() || mc.player.input.keyPresses.left() || mc.player.input.keyPresses.right())) {
			return;
		}

		if (!queue.isEmpty()) {
			mc.player.absSnapTo(mc.player.getX(), queue.poll(), mc.player.getZ());
			return;
		}

		if (getSetting(0).asMode().getMode() == 0 && mc.player.horizontalCollision && mc.player.onGround()) {
			if (!isTouchingWall(mc.player.getBoundingBox().move(0, 1, 0)) || !isTouchingWall(mc.player.getBoundingBox().move(0, 1.5, 0))) {
				mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 0.42, mc.player.getZ(), false, false));
				mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), false, false));

				if (isTouchingWall(mc.player.getBoundingBox().move(0, 1, 0)) && !isTouchingWall(mc.player.getBoundingBox().move(0, 1.5, 0))) {
					mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ(), false, false));
					mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ(), false, false));
					mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 1.24, mc.player.getZ(), false, false));
					mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ(), true, false));
					mc.player.absSnapTo(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ());
				} else {
					mc.player.absSnapTo(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ());
				}

				mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
				lastStep = mc.player.tickCount;
			}
		} else if (getSetting(0).asMode().getMode() == 2) {
			if (!mc.player.horizontalCollision && flag) {
				mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -0.1, mc.player.getDeltaMovement().z);
				lastStep = mc.player.tickCount;
				flag = false;
			} else if (mc.player.horizontalCollision) {
				mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, Math.min((mc.player.getY() + 1) - Math.floor(mc.player.getY()), 0.42), mc.player.getDeltaMovement().z);
				flag = true;
			}
		} else if (getSetting(0).asMode().getMode() == 3) {
			if (mc.player.horizontalCollision && mc.player.onGround()) {
				mc.player.jumpFromGround();
				flag = true;
			}

			if (flag && !mc.player.horizontalCollision /*pos + 1.065 < mc.player.getY()*/) {
				mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -0.1, mc.player.getDeltaMovement().z);
				lastStep = mc.player.tickCount;
				flag = false;
			}
		}
	}

	// 26.2: step height is an attribute now (Entity.maxUpStep/setStepHeight removed)
	private void setStepHeight(float height) {
		mc.player.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(height);
	}

	private boolean isTouchingWall(AABB box) {
		// Check in 2 calls instead of just box.inflate(0.01, 0, 0.01) to prevent it getting stuck in corners
		return !mc.level.noCollision(box.inflate(0.01, 0, 0)) || !mc.level.noCollision(box.inflate(0, 0, 0.01));
	}
}
