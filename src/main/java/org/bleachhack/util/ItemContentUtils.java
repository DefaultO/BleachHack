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
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

public class ItemContentUtils {

	public static List<ItemStack> getItemsInContainer(ItemStack item) {
		NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

		ItemContainerContents contents = item.get(DataComponents.CONTAINER);
		if (contents != null) {
			contents.copyInto(items);
		}

		return items;
	}

	public static List<List<String>> getTextInBook(ItemStack item) {
		List<String> pages = new ArrayList<>();

		WritableBookContent writable = item.get(DataComponents.WRITABLE_BOOK_CONTENT);
		if (writable != null) {
			writable.getPages(false).forEach(pages::add);
		} else {
			WrittenBookContent written = item.get(DataComponents.WRITTEN_BOOK_CONTENT);
			if (written != null) {
				for (Component text : written.getPages(false)) {
					pages.add(text.getString());
				}
			}
		}

		List<List<String>> finalPages = new ArrayList<>();

		for (String s : pages) {
			String buffer = "";
			List<String> pageBuffer = new ArrayList<>();

			for (char c : s.toCharArray()) {
				if (Minecraft.getInstance().font.width(buffer) > 114 || buffer.endsWith("\n")) {
					pageBuffer.add(buffer.replace("\n", ""));
					buffer = "";
				}

				buffer += c;
			}

			pageBuffer.add(buffer);
			finalPages.add(pageBuffer);
		}

		return finalPages;
	}
}
