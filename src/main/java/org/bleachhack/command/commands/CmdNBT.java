/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.command.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bleachhack.command.Command;
import org.bleachhack.command.CommandCategory;
import org.bleachhack.command.exception.CmdSyntaxException;
import org.bleachhack.util.BleachLogger;
import org.bleachhack.util.io.BleachJsonHelper;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;

public class CmdNBT extends Command {

	public CmdNBT() {
		super("nbt", "NBT stuff.", "nbt get [hand/block/entity] | nbt copy [hand/block/entity] | nbt set <nbt> | nbt wipe", CommandCategory.MISC);
	}

	@Override
	public void onCommand(String alias, String[] args) throws CmdSyntaxException, CommandSyntaxException {
		if (args.length == 0) {
			throw new CmdSyntaxException();
		}

		if (args[0].equalsIgnoreCase("get")) {
			if (args.length != 2) {
				throw new CmdSyntaxException();
			}

			CompoundTag nbt = getNbt(args[1]);

			if (nbt != null) {
				Component textNbt = NbtUtils.toPrettyComponent(nbt);

				Component copy = Component.literal("§e§l<COPY>")
						.withStyle(s ->
						s.withClickEvent(
								new ClickEvent.CopyToClipboard(textNbt.getString()))
						.withHoverEvent(
								new HoverEvent.ShowText(Component.literal("Copy the nbt of this item to your clipboard"))));

				BleachLogger.info(Component.literal("§6§lNBT: ").append(copy).append("§6\n").append(textNbt));
			}
		} else if (args[0].equalsIgnoreCase("copy")) {
			if (args.length != 2) {
				throw new CmdSyntaxException();
			}

			CompoundTag nbt = getNbt(args[1]);

			if (nbt != null) {
				mc.keyboardHandler.setClipboard(nbt.toString());
				BleachLogger.info("§6Copied\n§f" + NbtUtils.toPrettyComponent(nbt).getString() + "\n§6to clipboard.");
			}
		} else if (args[0].equalsIgnoreCase("set")) {
			if (!mc.gameMode.getPlayerMode().isCreative()) {
				BleachLogger.error("You must be in creative mode to set NBT!");
				return;
			}

			if (args.length < 2) {
				throw new CmdSyntaxException();
			}

			// TODO(26.2): item NBT was replaced by data components - this sets the minecraft:custom_data component only
			ItemStack item = mc.player.getMainHandItem();
			CompoundTag tag = TagParser.parseCompoundFully(StringUtils.join(ArrayUtils.subarray(args, 1, args.length), ' '));
			item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			BleachLogger.info("§6Set NBT of " + item.getItemName().getString() + " to\n" + BleachJsonHelper.formatJson(tag.toString()));
		} else if (args[0].equalsIgnoreCase("wipe")) {
			if (!mc.gameMode.getPlayerMode().isCreative()) {
				BleachLogger.error("You must be in creative mode to wipe NBT!");
				return;
			}

			// 26.2: no stack NBT to clear anymore, replace the held stack with a fresh one to drop all component patches
			ItemStack held = mc.player.getMainHandItem();
			mc.player.getInventory().setSelectedItem(new ItemStack(held.getItem(), held.getCount()));
		} else {
			throw new CmdSyntaxException();
		}
	}

	private CompoundTag getNbt(String arg) throws CmdSyntaxException {
		if (arg.equalsIgnoreCase("hand")) {
			ItemStack stack = mc.player.getMainHandItem();
			if (stack.isEmpty())
				return new CompoundTag();

			return (CompoundTag) ItemStack.CODEC.encodeStart(
					mc.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
		} else if (arg.equalsIgnoreCase("block")) {
			HitResult target = mc.hitResult;
			if (target.getType() == HitResult.Type.BLOCK) {
				BlockPos pos = ((BlockHitResult) target).getBlockPos();
				BlockEntity be = mc.level.getBlockEntity(pos);

				if (be != null) {
					return be.saveWithFullMetadata(mc.level.registryAccess());
				} else {
					return new CompoundTag();
				}
			}

			BleachLogger.error("Not looking at a block.");
			return null;
		} else if (arg.equalsIgnoreCase("entity")) {
			HitResult target = mc.hitResult;
			if (target.getType() == HitResult.Type.ENTITY) {
				TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, mc.level.registryAccess());
				((EntityHitResult) target).getEntity().saveWithoutId(output);
				return output.buildResult();
			}

			BleachLogger.error("Not looking at an entity.");
			return null;
		}

		throw new CmdSyntaxException();
	}
}
