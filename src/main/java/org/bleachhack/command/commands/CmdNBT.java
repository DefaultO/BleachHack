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

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
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
				Component textNbt = NbtUtils.toPrettyPrintedText(nbt);

				Component copy = Component.literal("§e§l<COPY>")
						.styled(s ->
						s.withClickEvent(
								new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, textNbt.getString()))
						.withHoverEvent(
								new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy the nbt of this item to your clipboard"))));

				BleachLogger.info(Component.literal("§6§lNBT: ").append(copy).append("§6\n" + textNbt));
			}
		} else if (args[0].equalsIgnoreCase("copy")) {
			if (args.length != 2) {
				throw new CmdSyntaxException();
			}

			CompoundTag nbt = getNbt(args[1]);

			if (nbt != null) {
				mc.keyboard.setClipboard(nbt.toString());
				BleachLogger.info("§6Copied\n§f" + NbtUtils.toPrettyPrintedText(nbt).getString() + "\n§6to clipboard.");
			}
		} else if (args[0].equalsIgnoreCase("set")) {
			if (!mc.interactionManager.getCurrentGameMode().isCreative()) {
				BleachLogger.error("You must be in creative mode to set NBT!");
				return;
			}

			if (args.length < 2) {
				throw new CmdSyntaxException();
			}

			ItemStack item = mc.player.getMainHandStack();
			item.setNbt(TagParser.parse(StringUtils.join(ArrayUtils.subarray(args, 1, args.length), ' ')));
			BleachLogger.info("§6Set NBT of " + item.getItem().getName().getString() + " to\n" + BleachJsonHelper.formatJson(item.getNbt().toString()));
		} else if (args[0].equalsIgnoreCase("wipe")) {
			if (!mc.interactionManager.getCurrentGameMode().isCreative()) {
				BleachLogger.error("You must be in creative mode to wipe NBT!");
				return;
			}

			mc.player.getMainHandStack().setNbt(new CompoundTag());
		} else {
			throw new CmdSyntaxException();
		}
	}

	private CompoundTag getNbt(String arg) throws CmdSyntaxException {
		if (arg.equalsIgnoreCase("hand")) {
			return mc.player.getMainHandStack().getOrCreateNbt();
		} else if (arg.equalsIgnoreCase("block")) {
			HitResult target = mc.crosshairTarget;
			if (target.getType() == HitResult.Type.BLOCK) {
				BlockPos pos = ((BlockHitResult) target).getBlockPos();
				BlockEntity be = mc.world.getBlockEntity(pos);

				if (be != null) {
					return be.createNbt();
				} else {
					return new CompoundTag();
				}
			}

			BleachLogger.error("Not looking at a block.");
			return null;
		} else if (arg.equalsIgnoreCase("entity")) {
			HitResult target = mc.crosshairTarget;
			if (target.getType() == HitResult.Type.ENTITY) {
				return ((EntityHitResult) target).getEntity().writeNbt(new CompoundTag());
			}

			BleachLogger.error("Not looking at an entity.");
			return null;
		}

		throw new CmdSyntaxException();
	}
}
