/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.math.NumberUtils;
import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventPacket;
import org.bleachhack.event.events.EventRenderInGameHud;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.gui.clickgui.UIClickGuiScreen;
import org.bleachhack.gui.clickgui.window.UIContainer;
import org.bleachhack.gui.clickgui.window.UIWindow;
import org.bleachhack.gui.clickgui.window.UIWindow.Position;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.module.ModuleManager;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class UI extends Module {

	private static final EquipmentSlot[] ARMOR_SLOTS = { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD };

	private List<Component> moduleListText = new ArrayList<>();
	private Component fpsText = Component.empty();
	private Component pingText = Component.empty();
	private Component coordsText = Component.empty();
	private Component tpsText = Component.empty();
	private Component durabilityText = Component.empty();
	private Component serverText = Component.empty();
	private Component timestampText = Component.empty();

	private long prevTime = 0;
	private double tps = 20;
	private long lastPacket = 0;

	public UI() {
		super("UI", KEY_UNBOUND, ModuleCategory.RENDER, true, "Shows stuff onscreen.",
				new SettingToggle("Modulelist", true).withDesc("Shows the module list.").withChildren(                                 // 0
						new SettingToggle("InnerLine", true).withDesc("Adds an extra line to the front of the module list."),
						new SettingToggle("OuterLine", false).withDesc("Adds an outer line to the module list."),
						new SettingToggle("Fill", true).withDesc("Adds a black fill behind the module list."),
						new SettingToggle("Watermark", true).withDesc("Adds the BleachHack watermark to the module list.").withChildren(
								new SettingMode("Mode", "New", "Old").withDesc("The watermark type.")),
						new SettingSlider("HueBright", 0, 1, 1, 2).withDesc("The hue of the rainbow."),
						new SettingSlider("HueSat", 0, 1, 0.5, 2).withDesc("The saturation of the rainbow."),
						new SettingSlider("HueSpeed", 0.1, 50, 25, 1).withDesc("The speed of the rainbow.")),
				new SettingToggle("FPS", true).withDesc("Shows your FPS."),                                                            // 1
				new SettingToggle("Ping", true).withDesc("Shows your ping."),                                                          // 2
				new SettingToggle("Coords", true).withDesc("Shows your coords and nether coords."),                                    // 3
				new SettingToggle("TPS", true).withDesc("Shows the estimated server tps."),                                            // 4
				new SettingToggle("Durability", false).withDesc("Shows durability left on the item you're holding."),                  // 5
				new SettingToggle("Server", false).withDesc("Shows the current server you are on."),                                   // 6
				new SettingToggle("Timestamp", false).withDesc("Shows the current time.").withChildren(                                // 7
						new SettingToggle("TimeZone", true).withDesc("Shows your time zone in the time."),
						new SettingToggle("Year", false).withDesc("Shows the current year in the time.")),
				new SettingToggle("Players", false).withDesc("Lists all the players in your render distance."),                        // 8
				new SettingToggle("Armor", true).withDesc("Shows your current armor.").withChildren(                                   // 9
						new SettingToggle("Vertical", false).withDesc("Displays your armor vertically."),
						new SettingMode("Damage", "Number", "Bar", "BarV").withDesc("How to show the armor durability.")),
				new SettingToggle("Lag-Meter", true).withDesc("Shows when the server isn't responding.").withChildren(                 // 10
						new SettingMode("Animation", "Fall", "Fade", "None").withDesc("How to animate the lag meter when appearing.")),
				new SettingToggle("Inventory", false).withDesc("Renders your inventory on screen.").withChildren(                      // 11
						new SettingSlider("Background", 0, 255, 140, 0).withDesc("How opaque the background should be.")));

		UIContainer container = UIClickGuiScreen.INSTANCE.getUIContainer();

		// Modulelist
		container.windows.put("modulelist",
				new UIWindow(new Position("l", 1, "t", 2), container,
						() -> getSetting(0).asToggle().getState(),
						this::getModuleListSize,
						this::drawModuleList)
				);

		// Info
		container.windows.put("coords",
				new UIWindow(new Position("l", 1, "b", 0), container,
						() -> getSetting(3).asToggle().getState(),
						() -> new int[] { mc.font.width(coordsText) + 2, 10 },
						(g, x, y) -> g.text(mc.font, coordsText, x + 1, y + 1, 0xffa0a0a0))
				);

		container.windows.put("fps",
				new UIWindow(new Position("l", 1, "coords", 0), container,
						() -> getSetting(1).asToggle().getState(),
						() -> new int[] { mc.font.width(fpsText) + 2, 10 },
						(g, x, y) -> g.text(mc.font, fpsText, x + 1, y + 1, 0xffa0a0a0))
				);

		container.windows.put("ping",
				new UIWindow(new Position("l", 1, "fps", 0), container,
						() -> getSetting(2).asToggle().getState(),
						() -> new int[] { mc.font.width(pingText) + 2, 10 },
						(g, x, y) -> g.text(mc.font, pingText, x + 1, y + 1, 0xffa0a0a0))
				);

		container.windows.put("tps",
				new UIWindow(new Position("l", 1, "ping", 0), container,
						() -> getSetting(4).asToggle().getState(),
						() -> new int[] { mc.font.width(tpsText) + 2, 10 },
						(g, x, y) -> g.text(mc.font, tpsText, x + 1, y + 1, 0xffa0a0a0))
				);

		container.windows.put("durability",
				new UIWindow(new Position(0.2, 0.9), container,
						() -> getSetting(5).asToggle().getState(),
						() -> new int[] { mc.font.width(durabilityText) + 2, 10 },
						(g, x, y) -> g.text(mc.font, durabilityText, x + 1, y + 1, 0xffa0a0a0))
				);

		container.windows.put("server",
				new UIWindow(new Position(0.2, 0.85, "durability", 0), container,
						() -> getSetting(6).asToggle().getState(),
						() -> new int[] { mc.font.width(serverText) + 2, 10 },
						(g, x, y) -> g.text(mc.font, serverText, x + 1, y + 1, 0xffa0a0a0))
				);

		container.windows.put("timestamp",
				new UIWindow(new Position(0.2, 0.8, "server", 0), container,
						() -> getSetting(7).asToggle().getState(),
						() -> new int[] { mc.font.width(timestampText) + 2, 10 },
						(g, x, y) -> g.text(mc.font, timestampText, x + 1, y + 1, 0xffa0a0a0))
				);

		// Players
		container.windows.put("players",
				new UIWindow(new Position("l", 1, "modulelist", 2), container,
						() -> getSetting(8).asToggle().getState(),
						this::getPlayerSize,
						this::drawPlayerList)
				);

		// Armor
		container.windows.put("armor",
				new UIWindow(new Position(0.5, 0.85), container,
						() -> getSetting(9).asToggle().getState(),
						this::getArmorSize,
						this::drawArmor)
				);

		// Lag-Meter
		container.windows.put("lagmeter",
				new UIWindow(new Position(0, 0.05, "c", 1), container,
						() -> getSetting(10).asToggle().getState(),
						this::getLagMeterSize,
						this::drawLagMeter)
				);

		// Inventory
		container.windows.put("inventory",
				new UIWindow(new Position(0.7, 0.90), container,
						() -> getSetting(11).asToggle().getState(),
						this::getInventorySize,
						this::drawInventory)
				);
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		// ModuleList
		moduleListText.clear();

		for (Module m : ModuleManager.getModules())
			if (m.isEnabled())
				moduleListText.add(Component.literal(m.getName()));

		moduleListText.sort(Comparator.comparingInt(t -> -mc.font.width(t)));

		if (getSetting(0).asToggle().getChild(3).asToggle().getState()) {
			int watermarkMode = getSetting(0).asToggle().getChild(3).asToggle().getChild(0).asMode().getMode();

			if (watermarkMode == 0) {
				moduleListText.add(0, BleachHack.watermark.getText().append(Component.literal(" " + BleachHack.VERSION).withStyle(s -> s.withColor(TextColor.fromRgb(0xf0f0f0)))));
			} else {
				moduleListText.add(0, Component.literal("§a> BleachHack " + BleachHack.VERSION));
			}
		}

		// FPS
		int fps = mc.getFps();
		fpsText = Component.literal("FPS: ")
				.append(colorText(Integer.toString(fps), Math.min(fps, 120) / 360f));

		// Ping
		PlayerInfo playerEntry = mc.player.connection.getPlayerInfo(mc.player.getUUID());
		int ping = playerEntry == null ? 0 : playerEntry.getLatency();
		pingText = Component.literal("Ping: ")
				.append(colorText(Integer.toString(ping), (800 - Mth.clamp(ping, 0, 800)) / 2400f));

		// Coords
		boolean nether = mc.level.dimension().identifier().getPath().contains("nether");
		BlockPos pos = mc.player.blockPosition();
		BlockPos pos2 = nether ? BlockPos.containing(mc.player.position().multiply(8, 1, 8))
				: BlockPos.containing(mc.player.position().multiply(0.125, 1, 0.125));

		coordsText = Component.literal("XYZ: ")
				.append(Component.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ()).withStyle(s -> s.withColor(nether ? 0xb02020 : 0x40f0f0)))
				.append(" [")
				.append(Component.literal(pos2.getX() + " " + pos2.getY() + " " + pos2.getZ()).withStyle(s -> s.withColor(nether ? 0x40f0f0 : 0xb02020)))
				.append("]");

		// TPS
		int time = (int) (System.currentTimeMillis() - lastPacket);
		String suffix = time >= 7500 ? "...." : time >= 5000 ? "..." : time >= 2500 ? ".." : time >= 1200 ? ".." : "";

		tpsText = Component.literal("TPS: ")
				.append(colorText(Double.toString(tps), (float) Mth.clamp(tps - 2, 0, 16) / 48))
				.append(suffix);

		// Durability
		ItemStack mainhand = mc.player.getMainHandItem();
		if (mainhand.isDamageableItem()) {
			CompoundTag customTag = mainhand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
			int durability = customTag.contains("dmg")
					? NumberUtils.toInt(customTag.getStringOr("dmg", Integer.toString(customTag.getIntOr("dmg", 0)))) : mainhand.getMaxDamage() - mainhand.getDamageValue();

			durabilityText = Component.literal("Durability: ")
					.append(colorText(Integer.toString(durability), (float) durability / mainhand.getMaxDamage() / 3f % 1f));
		} else {
			durabilityText = Component.literal("Durability: --");
		}

		// Server
		String server = mc.getCurrentServer() == null ? "Singleplayer" : mc.getCurrentServer().ip;
		serverText = Component.literal("Server: ")
				.append(Component.literal(server).withStyle(s -> s.withColor(ChatFormatting.LIGHT_PURPLE)));

		// Timestamp
		String timeString = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd HH:mm:ss"
				+ (getSetting(7).asToggle().getChild(0).asToggle().getState() ? " zzz" : "")
				+ (getSetting(7).asToggle().getChild(1).asToggle().getState() ? " yyyy" : "")));

		timestampText = Component.literal("Time: ")
				.append(Component.literal(timeString).withStyle(s -> s.withColor(ChatFormatting.YELLOW)));
	}

	@BleachSubscribe
	public void onDrawOverlay(EventRenderInGameHud event) {
		if (mc.gui.screen() instanceof UIClickGuiScreen) {
			return;
		}

		UIContainer container = UIClickGuiScreen.INSTANCE.getUIContainer();
		container.updatePositions(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
		container.render(event.getContext());
	}

	// --- Module List

	public int[] getModuleListSize() {
		if (moduleListText.isEmpty()) {
			return new int[] { 0, 0 };
		}

		int inner = getSetting(0).asToggle().getChild(0).asToggle().getState() ? 1 : 0;
		int outer = getSetting(0).asToggle().getChild(1).asToggle().getState() ? 4 : 3;
		return new int[] { mc.font.width(moduleListText.get(0)) + inner + outer, moduleListText.size() * 10 };
	}

	public void drawModuleList(GuiGraphicsExtractor drawContext, int x, int y) {
		if (moduleListText.isEmpty()) return;

		int arrayCount = 0;
		boolean inner = getSetting(0).asToggle().getChild(0).asToggle().getState();
		boolean outer = getSetting(0).asToggle().getChild(1).asToggle().getState();
		boolean fill = getSetting(0).asToggle().getChild(2).asToggle().getState();
		boolean rightAlign = x + mc.font.width(moduleListText.get(0)) / 2 > mc.getWindow().getGuiScaledWidth() / 2;

		int startX = rightAlign ? x + mc.font.width(moduleListText.get(0)) + 3 + (inner ? 1 : 0) + (outer ? 1 : 0) : x;
		for (Component t : moduleListText) {
			int color = getRainbowFromSettings(arrayCount * 40);
			int textStart = (rightAlign ? startX - mc.font.width(t) - 1 : startX + 2) + (inner ? 1 : 0) * (rightAlign ? -1 : 1);
			int outerX = rightAlign ? textStart - 3 : textStart + mc.font.width(t) + 1;

			if (fill) {
				drawContext.fill(rightAlign ? textStart - 2 : startX, y + arrayCount * 10, rightAlign ? startX : outerX, y + 10 + arrayCount * 10, 0x70003030);
			}

			if (inner) {
				drawContext.fill(rightAlign ? startX - 1 : startX, y + arrayCount * 10, rightAlign ? startX : startX + 1, y + 10 + arrayCount * 10, color);
			}

			if (outer) {
				drawContext.fill(outerX, y + arrayCount * 10, outerX + 1, y + 10 + arrayCount * 10, color);
			}

			drawContext.text(mc.font, t, textStart, y + 1 + arrayCount * 10, color);
			arrayCount++;
		}
	}

	// --- Players

	public int[] getPlayerSize() {
		List<Integer> nameLengths = mc.level.players().stream()
				.filter(e -> e != mc.player)
				.map(e -> mc.font.width(
						e.getDisplayName().getString()
						+ " | "
						+ e.blockPosition().getX() + " " + e.blockPosition().getY() + " " + e.blockPosition().getZ()
						+ " (" + Math.round(mc.player.distanceTo(e)) + "m)"))
				.collect(Collectors.toList());

		nameLengths.add(mc.font.width("Players:"));
		nameLengths.sort(Comparator.reverseOrder());

		return new int[] { nameLengths.get(0) + 2, nameLengths.size() * 10 + 1 };
	}

	public void drawPlayerList(GuiGraphicsExtractor drawContext, int x, int y) {
		drawContext.text(mc.font, "Players:", x + 1, y + 1, 0xffff0000);

		int count = 1;
		for (Entity e : mc.level.players().stream()
				.filter(e -> e != mc.player)
				.sorted(Comparator.comparing(mc.player::distanceTo))
				.toList()) {
			int dist = Math.round(mc.player.distanceTo(e));

			String text =
					e.getDisplayName().getString()
					+ " §7|§r " +
					e.blockPosition().getX() + " " + e.blockPosition().getY() + " " + e.blockPosition().getZ()
					+ " (" + dist + "m)";

			int playerColor =
					0xff000000 |
					((255 - (int) Math.min(dist * 2.1, 255) & 0xFF) << 16) |
					(((int) Math.min(dist * 4.28, 255) & 0xFF) << 8);

			drawContext.text(mc.font, text, x + 1, y + 1 + count * 10, playerColor);
			count++;
		}
	}

	// --- Lag Meter

	public int[] getLagMeterSize() {
		return new int[] { 144, 10 };
	}

	public void drawLagMeter(GuiGraphicsExtractor drawContext, int x, int y) {
		long time = System.currentTimeMillis();
		if (time - lastPacket > 500) {
			String text = "Server Lagging For: " + String.format(Locale.ENGLISH, "%.2f", (time - lastPacket) / 1000d) + "s";

			int xd = x + 72 - mc.font.width(text) / 2;
			switch (getSetting(10).asToggle().getChild(0).asMode().getMode()) {

				case 0 -> drawContext.text(mc.font, text, xd, (int) (y + 1 + Math.min((time - lastPacket - 1200) / 20, 0)), 0xffd0d0d0);
				case 1 -> drawContext.text(mc.font, text, xd, y + 1,
						(Mth.clamp((int) (time - lastPacket - 500) / 3, 5, 255) << 24) | 0xd0d0d0);
				case 2 -> drawContext.text(mc.font, text, xd, y + 1, 0xffd0d0d0);
			}
		}
	}

	// --- Armor

	public int[] getArmorSize() {
		boolean vertical = getSetting(9).asToggle().getChild(0).asToggle().getState();
		return new int[] { vertical ? 18 : 74, vertical ? 62 : 16 };
	}

	public void drawArmor(GuiGraphicsExtractor drawContext, int x, int y) {
		boolean vertical = getSetting(9).asToggle().getChild(0).asToggle().getState();

		for (int count = 0; count < ARMOR_SLOTS.length; count++) {
			ItemStack is = mc.player.getItemBySlot(ARMOR_SLOTS[count]);

			if (is.isEmpty())
				continue;

			int curX = vertical ? x : x + count * 19;
			int curY = vertical ? y + 47 - count * 16 : y;
			drawContext.item(is, curX, curY);

			int durcolor = is.isDamageableItem() ? 0xff000000 | Mth.hsvToRgb((float) (is.getMaxDamage() - is.getDamageValue()) / is.getMaxDamage() / 3.0F, 1.0F, 1.0F) : 0;

			if (is.getCount() > 1) {
				drawContext.pose().pushMatrix();
				String s = Integer.toString(is.getCount());

				drawContext.pose().translate(curX + 19 - mc.font.width(s), curY + 9);
				drawContext.pose().scale(0.85f, 0.85f);

				drawContext.text(mc.font, s, 0, 0, 0xffffffff);
				drawContext.pose().popMatrix();
			}

			if (is.isDamageableItem()) {
				int mode = getSetting(9).asToggle().getChild(1).asMode().getMode();
				if (mode == 0) {
					drawContext.pose().pushMatrix();
					drawContext.pose().scale(0.75f, 0.75f);

					String dur = Integer.toString(is.getMaxDamage() - is.getDamageValue());
					drawContext.text(mc.font, dur, (int) ((curX + 7 - mc.font.width(dur) * 1.333f / 4) * 1.333f), (int) ((curY - (vertical ? 2 : 3)) * 1.333f), durcolor);

					drawContext.pose().popMatrix();
				} else if (mode == 1) {
					int barLength = Math.round(13.0F - is.getDamageValue() * 13.0F / is.getMaxDamage());
					drawContext.fill(curX + 2, curY + 13, curX + 15, curY + 15, 0xff000000);
					drawContext.fill(curX + 2, curY + 13, curX + 2 + barLength, curY + 14, durcolor);
				} else {
					int barLength = Math.round(12.0F - is.getDamageValue() * 12.0F / is.getMaxDamage());
					drawContext.fill(curX + 15, curY + 2, curX + 17, curY + 14, 0xff000000);
					drawContext.fill(curX + 15, curY + 2, curX + 16, curY + 2 + barLength, durcolor);
				}
			}
		}
	}

	// --- Inventory

	public int[] getInventorySize() {
		return new int[] { 155, 53 };
	}

	public void drawInventory(GuiGraphicsExtractor drawContext, int x, int y) {
		if (getSetting(11).asToggle().getState()) {
			drawContext.fill(x + 155, y, x, y + 53,
					(getSetting(11).asToggle().getChild(0).asSlider().getValueInt() << 24) | 0x212120);

			for (int i = 0; i < 27; i++) {
				ItemStack itemStack = mc.player.getInventory().getItem(i + 9);
				int offsetX = x + 1 + (i % 9) * 17;
				int offsetY = y + 1 + (i / 9) * 17;
				drawContext.item(itemStack, offsetX, offsetY);
				drawContext.itemDecorations(mc.font, itemStack, offsetX, offsetY);
			}
		}
	}

	@BleachSubscribe
	public void readPacket(EventPacket.Read event) {
		lastPacket = System.currentTimeMillis();

		if (event.getPacket() instanceof ClientboundSetTimePacket) {
			long time = System.currentTimeMillis();
			long timeOffset = Math.abs(1000 - (time - prevTime)) + 1000;
			tps = Math.round(Mth.clamp(20 / (timeOffset / 1000d), 0, 20) * 100d) / 100d;
			prevTime = time;
		}
	}

	private static Component colorText(String text, float hue) {
		return Component.literal(text).withStyle(s -> s.withColor(Mth.hsvToRgb(hue, 1f, 1f)));
	}

	public static int getRainbow(float sat, float bri, double speed, int offset) {
		double rainbowState = Math.ceil((System.currentTimeMillis() + offset) / speed) % 360;
		return 0xff000000 | Mth.hsvToRgb((float) (rainbowState / 360.0), sat, bri);
	}

	public static int getRainbowFromSettings(int offset) {
		Module ui = ModuleManager.getModule("UI");

		if (ui == null)
			return getRainbow(0.5f, 0.5f, 10, 0);

		return getRainbow(
				ui.getSetting(0).asToggle().getChild(5).asSlider().getValueFloat(),
				ui.getSetting(0).asToggle().getChild(4).asSlider().getValueFloat(),
				ui.getSetting(0).asToggle().getChild(6).asSlider().getValue(),
				offset);
	}
}
