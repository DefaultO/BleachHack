/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventInteract;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.event.events.EventWorldRender;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.util.BleachLogger;
import org.bleachhack.util.InventoryUtils;
import org.bleachhack.util.render.Renderer;
import org.bleachhack.util.render.color.QuadColor;

import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class AutoBedrockBreak extends Module {

	private BlockPos pos;
	private int step;

	public AutoBedrockBreak() {
		super("AutoBedrockBreak", KEY_UNBOUND, ModuleCategory.EXPLOITS, "Automatically does the bedrock break exploit.");
	}

	@Override
	public void onDisable(boolean inWorld) {
		reset();

		super.onDisable(inWorld);
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (pos != null) {
			switch (step) {
				case 0:
					if (!mc.level.noCollision(new AABB(Vec3.atCenterOf(pos.above()), Vec3.atCenterOf(pos.offset(1, 8, 1))))) {
						reset();
						BleachLogger.info("Not enough empty space to break this block!");
					} else if (InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.PISTON) == -1) {
						reset();
						BleachLogger.info("Missing pistons!");
					} else if (InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.REDSTONE_BLOCK) == -1) {
						reset();
						BleachLogger.info("Missing a redstone block!");
					} else if (InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.TNT) == -1) {
						reset();
						BleachLogger.info("Missing TNT!");
					} else if (InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.LEVER) == -1) {
						reset();
						BleachLogger.info("Missing a lever!");
					} else if (dirtyPlace(pos.above(3), InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.REDSTONE_BLOCK), Direction.DOWN)) {
						step++;
					}

					break;
				case 1:
					mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), 90, mc.player.onGround(), mc.player.horizontalCollision));
					// mc.player.setPitch(90) "its jank either way"
					step++;

					break;
				case 2:
					if (dirtyPlace(pos.above(), InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.PISTON), Direction.DOWN))
						step++;

					break;
				case 3:
					if (dirtyPlace(pos.above(7), InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.TNT), Direction.DOWN))
						step++;

					break;
				case 4:
					if (dirtyPlace(pos.above(6), InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.LEVER), Direction.UP))
						step++;

					break;
				case 5:
					if (dirtyPlace(pos.above(5), InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.TNT), Direction.DOWN))
						step++;

					break;
				case 6:
					Vec3 leverCenter = Vec3.atCenterOf(pos.above(6));
					if (mc.player.getEyePosition().distanceTo(leverCenter) <= 4.75) {
						mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(leverCenter, Direction.DOWN, pos.above(6), false));
						step++;
					}

					break;
				default:
					if (mc.level.getBlockState(pos).isAir()
							|| mc.level.getBlockState(pos).getBlock() instanceof PistonBaseBlock
							|| (mc.level.getBlockState(pos.above()).getBlock() instanceof PistonBaseBlock
									&&  mc.level.getBlockState(pos.above()).getValue(PistonBaseBlock.FACING) != Direction.UP)) {
						setEnabled(false);
						return;
					}

					if (step >= 82) {
						mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), -90, mc.player.onGround(), mc.player.horizontalCollision));
						// mc.player.setPitch(-90) "its jank either way"
					}

					if (step > 84) {
						InteractionHand hand = InventoryUtils.selectSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.PISTON);
						if (hand != null) {
							mc.player.connection.send(new ServerboundUseItemOnPacket(hand,
									new BlockHitResult(Vec3.atBottomCenterOf(pos.above()), Direction.DOWN, pos.above(), false), 0));
						}
					}

					step++;
					break;
			}
		}
	}

	@BleachSubscribe
	public void onWorldRender(EventWorldRender.Post event) {
		if (pos == null) {
			if (mc.hitResult instanceof BlockHitResult
					&& !mc.level.isEmptyBlock(((BlockHitResult) mc.hitResult).getBlockPos())) {
				Renderer.drawBoxOutline(((BlockHitResult) mc.hitResult).getBlockPos(), QuadColor.single(0xffc040c0), 2f);
			}
		} else {
			Renderer.drawBoxOutline(pos, QuadColor.single(0xffc080c0), 2f);
		}
	}

	@BleachSubscribe
	public void onInteract(EventInteract.InteractBlock event) {
		if (pos == null && !mc.level.isEmptyBlock(event.getHitResult().getBlockPos())) {
			pos = event.getHitResult().getBlockPos();
			event.setCancelled(true);
		}
	}

	private boolean dirtyPlace(BlockPos pos, int slot, Direction dir) {
		Vec3 hitPos = Vec3.atCenterOf(pos).add(dir.getStepX() * 0.5, dir.getStepY() * 0.5, dir.getStepZ() * 0.5);
		if (mc.player.getEyePosition().distanceTo(hitPos) >= 4.75 || !mc.level.getEntities(null, new AABB(pos)).isEmpty()) {
			return false;
		}

		InteractionHand hand = InventoryUtils.selectSlot(slot);
		if (hand != null) {
			mc.gameMode.useItemOn(mc.player, hand, new BlockHitResult(hitPos, dir, pos, false));
			return true;
		}

		return false;
	}

	private void reset() {
		pos = null;
		step = 0;
	}
}
