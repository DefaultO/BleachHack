/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import java.util.Set;

import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingBlockList;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingRotate;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.BleachLogger;
import org.bleachhack.util.InventoryUtils;
import org.bleachhack.util.world.WorldUtils;

import com.google.common.collect.Sets;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Surround extends Module {

	private static final Set<Block> SURROUND_BLOCKS = Sets.newHashSet(
			Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.ENCHANTING_TABLE,
			Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL,
			Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK, Blocks.ANCIENT_DEBRIS,
			Blocks.RESPAWN_ANCHOR);

	public Surround() {
		super("Surround", KEY_UNBOUND, ModuleCategory.COMBAT, "Surrounds yourself with unexplodable blocks.",
				new SettingMode("Mode", "1x1", "Fit").withDesc("Mode, 1x1 places 4 blocks around you, fit fits the blocks around you so it doesn't place inside of you."),
				new SettingMode("Support", "Place", "AirPlace", "Skip").withDesc("What to do when theres no blocks to place surround blocks on."),
				new SettingSlider("BPT", 1, 8, 2, 0).withDesc("Blocks per tick, how many blocks to place per tick."),
				new SettingToggle("Autocenter", false).withDesc("Autocenters you to the nearest block."),
				new SettingToggle("KeepOn", true).withDesc("Keeps the module on after placing the obsidian."),
				new SettingToggle("JumpDisable", true).withDesc("Disables the module if you jump."),
				new SettingRotate(false).withDesc("Rotates when placing."),
				new SettingBlockList("Blocks", "Surround Blocks", SURROUND_BLOCKS::contains, Blocks.OBSIDIAN).withDesc("What blocks to surround with."));
	}

	@Override
	public void onEnable(boolean inWorld) {
		super.onEnable(inWorld);

		if (inWorld) {
			if (getSetting(3).asToggle().getState()) {
				Vec3 centerPos = Vec3.atBottomCenterOf(mc.player.blockPosition());
				mc.player.snapTo(centerPos.x, centerPos.y, centerPos.z);
				mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(centerPos.x, centerPos.y, centerPos.z, mc.player.onGround(), mc.player.horizontalCollision));
			}

			place();
		}
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (getSetting(5).asToggle().getState() && mc.options.keyJump.isDown()) {
			setEnabled(false);
			return;
		}

		place();
	}

	private void place() {
		int slot = InventoryUtils.getSlot(true,
				i -> getSetting(7).asList(Block.class).contains(Block.byItem(mc.player.getInventory().getItem(i).getItem())));

		if (slot == -1) {
			BleachLogger.error("No blocks to surround with!");
			setEnabled(false);
			return;
		}

		int cap = 0;

		AABB box = mc.player.getBoundingBox();
		Set<BlockPos> placePoses = getSetting(0).asMode().getMode() == 0
				? Sets.newHashSet(
						mc.player.blockPosition().north(), mc.player.blockPosition().east(),
						mc.player.blockPosition().south(), mc.player.blockPosition().west())
						: Sets.newHashSet(
								BlockPos.containing(box.minX - 1, box.minY, box.minZ), BlockPos.containing(box.minX, box.minY, box.minZ - 1),
								BlockPos.containing(box.maxX + 1, box.minY, box.minZ), BlockPos.containing(box.maxX, box.minY, box.minZ - 1),
								BlockPos.containing(box.minX - 1, box.minY, box.maxZ), BlockPos.containing(box.minX, box.minY, box.maxZ + 1),
								BlockPos.containing(box.maxX + 1, box.minY, box.maxZ), BlockPos.containing(box.maxX, box.minY, box.maxZ + 1));
		placePoses.removeIf(pos -> !mc.level.getBlockState(pos).canBeReplaced());

		if (placePoses.isEmpty()) {
			return;
		}

		int supportMode = getSetting(1).asMode().getMode();
		for (BlockPos pos : placePoses) {
			if (cap >= getSetting(2).asSlider().getValueInt()) {
				return;
			}

			if (WorldUtils.placeBlock(pos, slot, getSetting(6).asRotate(), false, supportMode == 1, true)) {
				cap++;
			} else {
				if (supportMode == 2) {
					continue;
				}

				if (WorldUtils.placeBlock(pos.below(), slot, getSetting(6).asRotate(), false, supportMode == 1, true)) {
					cap++;

					if (cap >= getSetting(2).asSlider().getValueInt()) {
						return;
					}

					if (WorldUtils.placeBlock(pos, slot, getSetting(6).asRotate(), false, supportMode == 1, true)) {
						cap++;
					}
				}
			}
		}

		if (!getSetting(4).asToggle().getState()) {
			setEnabled(false);
		}
	}

}
