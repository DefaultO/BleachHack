/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.event.events;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.bleachhack.event.Event;

public class EventInteract extends Event {

	public static class InteractItem extends EventInteract {

		protected InteractionHand hand;

		public InteractionHand getHand() {
			return hand;
		}

		public InteractItem(InteractionHand hand) {
			this.hand = hand;
		}
	}

	public static class InteractBlock extends EventInteract {

		protected InteractionHand hand;
		protected BlockHitResult hitResult;

		public InteractionHand getHand() {
			return hand;
		}

		public BlockHitResult getHitResult() {
			return hitResult;
		}

		public InteractBlock(InteractionHand hand, BlockHitResult hitResult) {
			this.hand = hand;
			this.hitResult = hitResult;
		}
	}

	public static class AttackBlock extends EventInteract {

		protected BlockPos pos;
		protected Direction direction;

		public BlockPos getPos() {
			return pos;
		}

		public Direction getDirection() {
			return direction;
		}

		public AttackBlock(BlockPos pos, Direction direction) {
			this.pos = pos;
			this.direction = direction;
		}
	}

	public static class BreakBlock extends EventInteract {

		protected BlockPos pos;

		public BlockPos getPos() {
			return pos;
		}

		public BreakBlock(BlockPos pos) {
			this.pos = pos;
		}
	}
}
