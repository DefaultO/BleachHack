/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import org.apache.commons.lang3.tuple.Pair;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.InventoryUtils;
import org.bleachhack.util.world.WorldUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class AutoFarm extends Module {

	private final Map<BlockPos, Integer> mossMap = new HashMap<>();

	public AutoFarm() {
		super("AutoFarm", KEY_UNBOUND, ModuleCategory.PLAYER, "Automatically does farming activities for you.",
				new SettingSlider("Range", 1, 6, 4.5, 1).withDesc("Farming reach."),
				new SettingToggle("Till", true).withDesc("Tills dirt around you.").withChildren(
						new SettingToggle("WateredOnly", false).withDesc("Only tills watered dirt.")),
				new SettingToggle("Harvest", true).withDesc("Harvests grown crops.").withChildren(
						new SettingToggle("Crops", true).withDesc("Harvests wheat, carrots, potato & beetroot."),
						new SettingToggle("StemCrops", true).withDesc("Harvests melons/pumpkins."),
						new SettingToggle("NetherWart", true).withDesc("Harvests nether wart."),
						new SettingToggle("Cocoa", true).withDesc("Harvests cocoa beans."),
						new SettingToggle("Berries", false).withDesc("Harvests sweet berries."),
						new SettingToggle("SugarCane", false).withDesc("Harvests sugar canes."),
						new SettingToggle("Cactus", false).withDesc("Harvests cactuses.")),
				new SettingToggle("Plant", true).withDesc("Plants crops around you.").withChildren(
						new SettingToggle("Crops", true).withDesc("Plants wheat, carrots, potato & beetroot."),
						new SettingToggle("StemCrops", true).withDesc("Plants melon/pumpkin stems."),
						new SettingToggle("NetherWart", true).withDesc("Plants nether wart.")),
				new SettingToggle("Bonemeal", true).withDesc("Bonemeals ungrown crop.").withChildren(
						new SettingToggle("Crops", true).withDesc("Bonemeals wheat, carrots, potato & beetroot."),
						new SettingToggle("StemCrops", true).withDesc("Bonemeals melon/pumpkin stems."),
						new SettingToggle("Cocoa", true).withDesc("Bonemeals cocoa beans."),
						new SettingToggle("Berries", false).withDesc("Bonemeals sweet berries."),
						new SettingToggle("Mushrooms", false).withDesc("Bonemeals mushrooms."),
						new SettingToggle("Saplings", false).withDesc("Bonemeals saplings."),
						new SettingToggle("Moss", false).withDesc("Bonemeals moss.")));
	}

	@Override
	public void onDisable(boolean inWorld) {
		mossMap.clear();

		super.onDisable(inWorld);
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		mossMap.entrySet().removeIf(e -> e.setValue(e.getValue() - 1) == 0);

		double range = getSetting(0).asSlider().getValue();
		int ceilRange = Mth.ceil(range);
		SettingToggle tillSetting = getSetting(1).asToggle();
		SettingToggle harvestSetting = getSetting(2).asToggle();
		SettingToggle plantSetting = getSetting(3).asToggle();
		SettingToggle bonemealSetting = getSetting(4).asToggle();

		// Special case for moss to maximize efficiency
		if (bonemealSetting.getState() && bonemealSetting.getChild(6).asToggle().getState()) {
			int slot = InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.BONE_MEAL);
			if (slot != -1) {
				BlockPos bestBlock = BlockPos.withinManhattanStream(BlockPos.containing(mc.player.getEyePosition()), ceilRange, ceilRange, ceilRange)
						.filter(b -> mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(b)) <= range && !mossMap.containsKey(b))
						.map(b -> Pair.of(b.immutable(), getMossSpots(b)))
						.filter(p -> p.getRight() > 10)
						.map(Pair::getLeft)
						.min(Comparator.reverseOrder()).orElse(null);

				if (bestBlock != null) {
					if (!mc.level.isEmptyBlock(bestBlock.above())) {
						mc.gameMode.continueDestroyBlock(bestBlock.above(), Direction.UP);
					}

					InteractionHand hand = InventoryUtils.selectSlot(slot);
					mc.gameMode.useItemOn(mc.player, hand,
							new BlockHitResult(Vec3.upFromBottomCenterOf(bestBlock, 1), Direction.UP, bestBlock, false));
					mossMap.put(bestBlock, 100);
					return;
				}
			}
		}

		for (BlockPos pos: BlockPos.withinManhattan(BlockPos.containing(mc.player.getEyePosition()), ceilRange, ceilRange, ceilRange)) {
			if (mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)) > range)
				continue;

			BlockState state = mc.level.getBlockState(pos);
			Block block = state.getBlock();
			if (tillSetting.getState() && canTill(block) && mc.level.isEmptyBlock(pos.above())) {
				if (!tillSetting.getChild(0).asToggle().getState()
						|| BlockPos.betweenClosedStream(pos.getX() - 4, pos.getY(), pos.getZ() - 4, pos.getX() + 4, pos.getY(), pos.getZ() + 4).anyMatch(
								b -> mc.level.getFluidState(b).is(FluidTags.WATER))) {
					InteractionHand hand = InventoryUtils.selectSlot(true, i -> mc.player.getInventory().getItem(i).getItem() instanceof HoeItem);

					if (hand != null) {
						mc.gameMode.useItemOn(mc.player, hand,
								new BlockHitResult(Vec3.upFromBottomCenterOf(pos, 1), Direction.UP, pos, false));
						return;
					}
				}
			}

			if (harvestSetting.getState()) {
				if ((harvestSetting.getChild(0).asToggle().getState() && block instanceof CropBlock && ((CropBlock) block).isMaxAge(state))
						|| (harvestSetting.getChild(1).asToggle().getState() && block == Blocks.MELON)
						|| (harvestSetting.getChild(1).asToggle().getState() && block == Blocks.PUMPKIN)
						|| (harvestSetting.getChild(2).asToggle().getState() && block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3)
						|| (harvestSetting.getChild(3).asToggle().getState() && block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) >= 2)
						|| (harvestSetting.getChild(4).asToggle().getState() && block instanceof SweetBerryBushBlock && state.getValue(SweetBerryBushBlock.AGE) >= 3)
						|| (harvestSetting.getChild(5).asToggle().getState() && shouldHarvestTallCrop(pos, block, SugarCaneBlock.class))
						|| (harvestSetting.getChild(6).asToggle().getState() && shouldHarvestTallCrop(pos, block, CactusBlock.class))) {
					mc.gameMode.continueDestroyBlock(pos, Direction.UP);
					return;
				}
			}

			if (plantSetting.getState() && mc.level.getEntities((Entity) null, new AABB(pos.above()), EntitySelector.LIVING_ENTITY_STILL_ALIVE).isEmpty()) {
				if (block instanceof FarmlandBlock && mc.level.isEmptyBlock(pos.above())) {
					int slot = InventoryUtils.getSlot(true, i -> {
						Item item = mc.player.getInventory().getItem(i).getItem();

						if (plantSetting.getChild(0).asToggle().getState() && (item == Items.WHEAT_SEEDS || item == Items.CARROT || item == Items.POTATO || item == Items.BEETROOT_SEEDS)) {
							return true;
						}

						return plantSetting.getChild(1).asToggle().getState() && (item == Items.PUMPKIN_SEEDS || item == Items.MELON_SEEDS);
					});

					if (slot != -1) {
						WorldUtils.placeBlock(pos.above(), slot, 0, false, false, true);
						return;
					}
				}

				if (block instanceof SoulSandBlock && mc.level.isEmptyBlock(pos.above()) && plantSetting.getChild(2).asToggle().getState()) {
					int slot = InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.NETHER_WART);

					if (slot != -1) {
						WorldUtils.placeBlock(pos.above(), slot, 0, false, false, true);
						return;
					}
				}
			}

			if (bonemealSetting.getState()) {
				int slot = InventoryUtils.getSlot(true, i -> mc.player.getInventory().getItem(i).getItem() == Items.BONE_MEAL);

				if (slot != -1) {
					if ((bonemealSetting.getChild(0).asToggle().getState() && block instanceof CropBlock && !((CropBlock) block).isMaxAge(state))
							|| (bonemealSetting.getChild(1).asToggle().getState() && block instanceof StemBlock && state.getValue(StemBlock.AGE) < StemBlock.MAX_AGE)
							|| (bonemealSetting.getChild(2).asToggle().getState() && block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) < 2)
							|| (bonemealSetting.getChild(3).asToggle().getState() && block instanceof SweetBerryBushBlock && state.getValue(SweetBerryBushBlock.AGE) < 3)
							|| (bonemealSetting.getChild(4).asToggle().getState() && block instanceof MushroomBlock)
							|| (bonemealSetting.getChild(5).asToggle().getState() && (block instanceof SaplingBlock || block instanceof AzaleaBlock) && canPlaceSapling(pos))) {
						InteractionHand hand = InventoryUtils.selectSlot(slot);
						mc.gameMode.useItemOn(mc.player, hand,
								new BlockHitResult(Vec3.upFromBottomCenterOf(pos, 1), Direction.UP, pos, false));
						return;
					}
				}
			}
		}
	}

	private boolean shouldHarvestTallCrop(BlockPos pos, Block posBlock, Class<? extends Block> blockClass) {
		return posBlock.getClass().equals(blockClass)
				&& mc.level.getBlockState(pos.below()).getBlock().getClass().equals(blockClass)
				&& !mc.level.getBlockState(pos.below(2)).getBlock().getClass().equals(blockClass);
	}

	private int getMossSpots(BlockPos pos) {
		if (mc.level.getBlockState(pos).getBlock() != Blocks.MOSS_BLOCK
				|| mc.level.getBlockState(pos.above()).getDestroySpeed(mc.level, pos) != 0f) {
			return 0;
		}

		return (int) BlockPos.withinManhattanStream(pos, 3, 4, 3)
				.filter(b -> isMossGrowableOn(mc.level.getBlockState(b).getBlock()) && mc.level.isEmptyBlock(b.above()))
				.count();
	}

	private boolean isMossGrowableOn(Block block) {
		return block == Blocks.STONE || block == Blocks.GRANITE || block == Blocks.ANDESITE || block == Blocks.DIORITE
				|| block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.MYCELIUM || block == Blocks.GRASS_BLOCK
				|| block == Blocks.PODZOL || block == Blocks.ROOTED_DIRT;
	}

	private boolean canPlaceSapling(BlockPos pos) {
		return BlockPos.betweenClosedStream(pos.getX() - 1, pos.getY() + 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 5, pos.getZ() + 1)
				.allMatch(b -> TreeFeature.validTreePos(mc.level, b));
	}

	private boolean canTill(Block block) {
		return block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.COARSE_DIRT
				|| block == Blocks.ROOTED_DIRT || block == Blocks.DIRT_PATH;
	}
}
