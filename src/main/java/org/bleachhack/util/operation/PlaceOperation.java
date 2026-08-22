/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util.operation;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.ArrayUtils;
import org.bleachhack.util.InventoryUtils;
import org.bleachhack.util.render.Renderer;
import org.bleachhack.util.render.color.QuadColor;
import org.bleachhack.util.world.WorldUtils;

public class PlaceOperation extends Operation {

	protected Item[] items;

	protected PlaceOperation(BlockPos pos, Item... items) {
		this.pos = pos;
		this.items = items;
	}

	public static OperationBlueprint blueprint(int localX, int localY, int localZ, Item... items) {
		return (origin, dir) -> new PlaceOperation(origin.offset(rotate(localX, localY, localZ, dir)), items);
	}

	@Override
	public boolean canExecute() {
		if (mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)) > 4.5)
			return false;

		return InventoryUtils.getSlot(true, i -> ArrayUtils.contains(items, mc.player.getInventory().getItem(i).getItem())) != -1;
	}

	@Override
	public boolean execute() {
		int slot = InventoryUtils.getSlot(true, i -> ArrayUtils.contains(items, mc.player.getInventory().getItem(i).getItem()));

		return WorldUtils.placeBlock(pos, slot, 0, false, false, true);
	}

	@Override
	public boolean verify() {
		return true;
	}

	public Item[] getItems() {
		return items;
	}

	@Override
	public void render() {
		Item item = getItems()[0];
		if (item instanceof BlockItem) {
			BlockState state = ((BlockItem) item).getBlock().defaultBlockState();

			// TODO(26.2): ghost block-model preview removed. Minecraft.getBlockRenderManager()
			// (BlockRenderDispatcher), Minecraft.getBufferBuilders()/getEntityVertexConsumers()
			// (RenderBuffers/MultiBufferSource), and RenderTypes.getMovingBlockLayer(state) no longer
			// exist. 26.2 renders moving blocks through the FeatureRenderer/GuiRenderState submit
			// pipeline (MovingBlockFeatureRenderer + ModelBlockRenderer.tesselateBlock with a
			// MovingBlockRenderState), which can't be driven inline from here. Needs a rewrite; the
			// translucent shape fill below is kept so the placement preview is still visible.
			// PoseStack matrices = WorldRenderer.matrixFrom(pos.getX(), pos.getY(), pos.getZ());
			// mc.getBlockRenderManager().renderBlock(state, pos, mc.level, matrices,
			//         mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderTypes.getMovingBlockLayer(state)),
			//         false, RandomSource.create(0L));
			// mc.getBufferBuilders().getEntityVertexConsumers().draw(RenderTypes.getMovingBlockLayer(state));

			for (AABB box: state.getShape(mc.level, pos).toAabbs()) {
				Renderer.drawBoxFill(box.move(pos), QuadColor.single(0.45f, 0.7f, 1f, 0.4f));
			}
		} else {
			Renderer.drawBoxBoth(pos, QuadColor.single(1f, 1f, 0f, 0.3f), 2.5f);
		}
	}
}
