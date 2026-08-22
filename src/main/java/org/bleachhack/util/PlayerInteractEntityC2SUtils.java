/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;

/* Mojang how */
/* HOW */
public class PlayerInteractEntityC2SUtils {

	public static Entity getEntity(ServerboundInteractPacket packet) {
		// 26.2: packet is a record, read the entity id directly (no more buffer round-trip).
		return Minecraft.getInstance().level.getEntity(packet.entityId());
	}

	// TODO(26.2): ServerboundInteractPacket no longer carries an interact-type/action field.
	// Attacks moved to their own packet (ServerboundAttackPacket), and the old INTERACT vs
	// INTERACT_AT distinction collapsed into a single interact-with-location packet. We can
	// only tell INTERACT_AT (has a location) from a plain INTERACT here; this packet can
	// never represent ATTACK anymore. Callers that relied on InteractType.ATTACK (e.g.
	// Criticals) must instead listen for ServerboundAttackPacket.
	public static InteractType getInteractType(ServerboundInteractPacket packet) {
		return packet.location() != null ? InteractType.INTERACT_AT : InteractType.INTERACT;
	}

	public enum InteractType {
		INTERACT,
		ATTACK,
		INTERACT_AT
	}
}
