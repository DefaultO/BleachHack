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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bleachhack.BleachHack;

public class BleachLogger {

	public static final Logger logger = LogManager.getFormatterLogger("BleachHack");

	public static int INFO_COLOR = 0x64b9fa;
	public static int WARN_COLOR = TextColor.YELLOW.getValue();
	public static int ERROR_COLOR = TextColor.RED.getValue();
	
	// Info
	
	public static void info(String s) {
		info(Component.literal(s));
	}

	public static void info(Component t) {
		try {
			Minecraft.getInstance().gui.hud.getChat()
			.addClientSystemMessage(getBHText(INFO_COLOR)
					//.append("§3§lINFO: §3")
					.append(((MutableComponent) t).withStyle(s -> s.withColor(INFO_COLOR))));
		} catch (Exception e) {
			logger.log(Level.INFO, t.getString());
		}
	}
	
	// Warn
	
	public static void warn(String s) {
		warn(Component.literal(s));
	}

	public static void warn(Component t) {
		try {
			Minecraft.getInstance().gui.hud.getChat()
			.addClientSystemMessage(getBHText(WARN_COLOR)
					//.append("§e§lWARN: §e")
					.append(((MutableComponent) t).withStyle(s -> s.withColor(WARN_COLOR))));
		} catch (Exception e) {
			logger.log(Level.WARN, t.getString());
		}
	}
	
	// Error
	
	public static void error(String s) {
		error(Component.literal(s));
	}

	public static void error(Component t) {
		try {
			Minecraft.getInstance().gui.hud.getChat()
			.addClientSystemMessage(getBHText(ERROR_COLOR)
					//.append("§c§lERROR: §c")
					.append(((MutableComponent) t).withStyle(s -> s.withColor(ERROR_COLOR))));
		} catch (Exception e) {
			logger.log(Level.ERROR, t.getString());
		}
	}

	public static void noPrefix(String s) {
		noPrefix(Component.literal(s));
	}

	public static void noPrefix(Component text) {
		try {
			Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(text);
		} catch (Exception e) {
			logger.log(Level.INFO, text.getString());
		}
	}

	private static MutableComponent getBHText(int color) {
		return Component.literal("[").withStyle(s -> s.withColor(color))
				.append(BleachHack.watermark.getText())
				.append(Component.literal("] ").withStyle(s -> s.withColor(color)));
	}
}
