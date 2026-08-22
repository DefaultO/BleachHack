/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.world;

import com.google.common.collect.Sets;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bleachhack.setting.module.SettingRotate;
import org.bleachhack.util.InventoryUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WorldUtils {

	protected static final Minecraft mc = Minecraft.getInstance();

	public static final Set<Block> RIGHTCLICKABLE_BLOCKS = Sets.newHashSet(
			Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST,
			Blocks.SHULKER_BOX, Blocks.ANVIL, Blocks.BELL,
			Blocks.OAK_BUTTON, Blocks.ACACIA_BUTTON, Blocks.BIRCH_BUTTON, Blocks.DARK_OAK_BUTTON,
			Blocks.JUNGLE_BUTTON, Blocks.SPRUCE_BUTTON, Blocks.STONE_BUTTON, Blocks.COMPARATOR,
			Blocks.REPEATER, Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE, Blocks.BIRCH_FENCE_GATE,
			Blocks.JUNGLE_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE, Blocks.ACACIA_FENCE_GATE,
			Blocks.BREWING_STAND, Blocks.DISPENSER, Blocks.DROPPER,
			Blocks.LEVER, Blocks.NOTE_BLOCK, Blocks.JUKEBOX,
			Blocks.BEACON, Blocks.FURNACE, Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR,
			Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR, Blocks.ACACIA_DOOR,
			Blocks.DARK_OAK_DOOR, Blocks.CAKE, Blocks.ENCHANTING_TABLE,
			Blocks.DRAGON_EGG, Blocks.HOPPER, Blocks.REPEATING_COMMAND_BLOCK,
			Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.CRAFTING_TABLE,
			Blocks.ACACIA_TRAPDOOR, Blocks.BIRCH_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR,
			Blocks.JUNGLE_TRAPDOOR, Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR,
			Blocks.CAKE, Blocks.ACACIA_SIGN, Blocks.ACACIA_WALL_SIGN,
			Blocks.BIRCH_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.DARK_OAK_SIGN,
			Blocks.DARK_OAK_WALL_SIGN, Blocks.JUNGLE_SIGN, Blocks.JUNGLE_WALL_SIGN,
			Blocks.OAK_SIGN, Blocks.OAK_WALL_SIGN, Blocks.SPRUCE_SIGN,
			Blocks.SPRUCE_WALL_SIGN, Blocks.CRIMSON_SIGN, Blocks.CRIMSON_WALL_SIGN,
			Blocks.WARPED_SIGN, Blocks.WARPED_WALL_SIGN, Blocks.BLAST_FURNACE, Blocks.SMOKER,
			Blocks.CARTOGRAPHY_TABLE, Blocks.GRINDSTONE, Blocks.LECTERN, Blocks.LOOM,
			Blocks.STONECUTTER, Blocks.SMITHING_TABLE);

	static {
		// 26.2: colored block variants live in ColorCollections now.
		RIGHTCLICKABLE_BLOCKS.addAll(Blocks.DYED_SHULKER_BOX.asList());
		RIGHTCLICKABLE_BLOCKS.addAll(Blocks.BED.asList());
	}

	public static List<LevelChunk> getLoadedChunks() {
		List<LevelChunk> chunks = new ArrayList<>();

		int viewDist = mc.options.renderDistance().get();

		for (int x = -viewDist; x <= viewDist; x++) {
			for (int z = -viewDist; z <= viewDist; z++) {
				LevelChunk chunk = mc.level.getChunkSource().getChunk((int) mc.player.getX() / 16 + x, (int) mc.player.getZ() / 16 + z, false);

				if (chunk != null) {
					chunks.add(chunk);
				}
			}
		}

		return chunks;
	}

	public static List<BlockEntity> getBlockEntities() {
		List<BlockEntity> list = new ArrayList<>();
		for (LevelChunk chunk: getLoadedChunks())
			list.addAll(chunk.getBlockEntities().values());

		return list;
	}

	public static boolean doesBoxTouchBlock(AABB box, Block block) {
		for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
			for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
				for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
					if (mc.level.getBlockState(new BlockPos(x, y, z)).getBlock() == block) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public static boolean doesBoxCollide(AABB box) {
		for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
			for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
				for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
					int fx = x, fy = y, fz = z;
					if (mc.level.getBlockState(new BlockPos(x, y, z)).getCollisionShape(mc.level, new BlockPos(x, y, z)).toAabbs().stream()
							.anyMatch(b -> b.move(fx, fy, fz).intersects(box))) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public static boolean placeBlock(BlockPos pos, int slot, SettingRotate sr, boolean forceLegit, boolean airPlace, boolean swingHand) {
		return placeBlock(pos, slot, !sr.getState() ? 0 : sr.getRotateMode() + 1, forceLegit, airPlace, swingHand);
	}

	public static boolean placeBlock(BlockPos pos, int slot, int rotateMode, boolean forceLegit, boolean airPlace, boolean swingHand) {
		if (!mc.level.isInWorldBounds(pos) || !isBlockEmpty(pos))
			return false;

		for (Direction d : Direction.values()) {
			if (!mc.level.isInWorldBounds(pos.relative(d)))
				continue;

			Block neighborBlock = mc.level.getBlockState(pos.relative(d)).getBlock();

			if (!airPlace && neighborBlock.defaultBlockState().canBeReplaced())
				continue;

			Vec3 vec = getLegitLookPos(pos.relative(d), d.getOpposite(), true, 5);

			if (vec == null) {
				if (forceLegit) {
					continue;
				}

				vec = getLegitLookPos(pos.relative(d), d.getOpposite(), false, 5);

				if (vec == null) {
					continue;
				}
			}

			int prevSlot = mc.player.getInventory().getSelectedSlot();
			InteractionHand hand = InventoryUtils.selectSlot(slot);

			if (hand == null) {
				return false;
			}

			if (rotateMode == 1) {
				facePosPacket(vec.x, vec.y, vec.z);
			} else if (rotateMode == 2) {
				facePos(vec.x, vec.y, vec.z);
			}

			if (RIGHTCLICKABLE_BLOCKS.contains(neighborBlock)) {
				sendSneak(true);
			}

			if (swingHand) {
				mc.player.swing(hand);
			} else {
				mc.player.connection.send(new ServerboundSwingPacket(hand));
			}

			mc.gameMode.useItemOn(mc.player, hand,
					new BlockHitResult(Vec3.atCenterOf(pos), airPlace ? d : d.getOpposite(), airPlace ? pos : pos.relative(d), false));

			if (RIGHTCLICKABLE_BLOCKS.contains(neighborBlock))
				sendSneak(false);

			mc.player.getInventory().setSelectedSlot(prevSlot);

			return true;
		}

		return false;
	}

	// ponytail: 26.2 dropped ServerboundPlayerCommandPacket PRESS/RELEASE_SHIFT_KEY; sneak now rides the input packet.
	private static void sendSneak(boolean sneak) {
		Input in = mc.player.input.keyPresses;
		mc.player.connection.send(new ServerboundPlayerInputPacket(
				sneak ? new Input(in.forward(), in.backward(), in.left(), in.right(), in.jump(), true, in.sprint()) : in));
	}

	public static Vec3 getLegitLookPos(BlockPos pos, Direction dir, boolean raycast, int res) {
		return getLegitLookPos(new AABB(pos), dir, raycast, res, 0.01);
	}

	public static Vec3 getLegitLookPos(AABB box, Direction dir, boolean raycast, int res, double extrude) {
		Vec3 eyePos = mc.player.getEyePosition();
		Vec3 blockPos = new Vec3(box.minX, box.minY, box.minZ).add(
				(dir == Direction.WEST ? -extrude : dir.getStepX() * box.getXsize() + extrude),
				(dir == Direction.DOWN ? -extrude : dir.getStepY() * box.getYsize() + extrude),
				(dir == Direction.NORTH ? -extrude : dir.getStepZ() * box.getZsize() + extrude));

		for (double i = 0; i <= 1; i += 1d / (double) res) {
			for (double j = 0; j <= 1; j += 1d / (double) res) {
				Vec3 lookPos = blockPos.add(
						(dir.getAxis() == Axis.X ? 0 : i * box.getXsize()),
						(dir.getAxis() == Axis.Y ? 0 : dir.getAxis() == Axis.Z ? j * box.getYsize() : i * box.getYsize()),
						(dir.getAxis() == Axis.Z ? 0 : j * box.getZsize()));

				if (eyePos.distanceTo(lookPos) > 4.55)
					continue;

				if (raycast) {
					if (mc.level.clip(new ClipContext(eyePos, lookPos,
							ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.MISS) {
						return lookPos;
					}
				} else {
					return lookPos;
				}
			}
		}

		return null;
	}

	public static boolean isBlockEmpty(BlockPos pos) {
		if (!mc.level.getBlockState(pos).canBeReplaced()) {
			return false;
		}

		AABB box = new AABB(pos);
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof LivingEntity && box.intersects(e.getBoundingBox())) {
				return false;
			}
		}

		return true;
	}

	public static void facePosAuto(double x, double y, double z, SettingRotate sr) {
		if (sr.getRotateMode() == 0) {
			facePosPacket(x, y, z);
		} else {
			facePos(x, y, z);
		}
	}

	public static void facePos(double x, double y, double z) {
		float[] rot = getViewingRotation(mc.player, x, y, z);

		mc.player.setYRot(mc.player.getYRot() + Mth.wrapDegrees(rot[0] - mc.player.getYRot()));
		mc.player.setXRot(mc.player.getXRot() + Mth.wrapDegrees(rot[1] - mc.player.getXRot()));
	}

	public static void facePosPacket(double x, double y, double z) {
		float[] rot = getViewingRotation(mc.player, x, y, z);

		if (!mc.player.isPassenger()) {
			mc.player.yHeadRot = mc.player.getYRot() + Mth.wrapDegrees(rot[0] - mc.player.getYRot());
			mc.player.yBodyRot = mc.player.yHeadRot;
			mc.player.xBob = mc.player.getXRot() + Mth.wrapDegrees(rot[1] - mc.player.getXRot());
		}

		mc.player.connection.send(
				new ServerboundMovePlayerPacket.Rot(
						mc.player.getYRot() + Mth.wrapDegrees(rot[0] - mc.player.getYRot()),
						mc.player.getXRot() + Mth.wrapDegrees(rot[1] - mc.player.getXRot()), mc.player.onGround(), mc.player.horizontalCollision));
	}
	
	public static float[] getViewingRotation(Entity entity, double x, double y, double z) {
		double diffX = x - entity.getX();
		double diffY = y - entity.getEyeY();
		double diffZ = z - entity.getZ();

		double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

		return new float[] {
				(float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90f,
				(float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) };
	}

	public static int getTopBlockIgnoreLeaves(int x, int z) {
		int top = mc.level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;

		while (top > mc.level.getMinY()) {
			BlockState state = mc.level.getBlockState(new BlockPos(x, top, z));

			if (!(state.isAir() || state.getBlock() instanceof LeavesBlock || state.getBlock() instanceof VegetationBlock)) {
				break;
			}

			top--;
		}

		return top;
	}
}