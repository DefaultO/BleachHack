/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Util;
import net.minecraft.util.Mth;
import org.bleachhack.BleachHack;
import org.bleachhack.gui.effect.ParticleManager;
import org.bleachhack.gui.window.Window;
import org.bleachhack.gui.window.WindowScreen;
import org.bleachhack.gui.window.widget.WindowButtonWidget;
import org.bleachhack.gui.window.widget.WindowTextWidget;
import org.bleachhack.module.mods.UI;
import org.bleachhack.util.io.BleachFileHelper;
import org.bleachhack.util.io.BleachOnlineMang;

import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Random;

public class BleachTitleScreen extends WindowScreen {

	private ParticleManager particleMang = new ParticleManager();
	public static boolean customTitleScreen = true;

	private static String splash;
	private static int splashTicks;

	static {
		BleachOnlineMang.getResourceAsync("splashes.txt", BodyHandlers.ofLines()).thenAccept(st -> {
			if (st != null) {
				List<String> list = st.toList();
				splash = list.get(new Random().nextInt(list.size()));
			}
		});
	}

	public BleachTitleScreen() {
		super(Component.translatable("narrator.screen.title"));
	}

	@Override
	public void init() {
		super.init();

		addWindow(new Window(width / 8,
				height / 8,
				width - width / 8,
				height - height / 8 + 2, "BleachHack",
				(java.util.function.Supplier<ItemStack>) () -> new ItemStack(Items.MUSIC_DISC_CAT)));

		int w = getWindow(0).x2 - getWindow(0).x1;
		int h = getWindow(0).y2 - getWindow(0).y1;
		int maxY = Mth.clamp(h / 4 + 119, 0, h - 22);

		getWindow(0).addWidget(new WindowButtonWidget(w / 2 - 100, h / 4 + 38, w / 2 + 100, h / 4 + 58, I18n.get("menu.singleplayer"), () ->
			minecraft.gui.setScreen(new SelectWorldScreen(minecraft.gui.screen()))
		));

		getWindow(0).addWidget(new WindowButtonWidget(w / 2 - 100, h / 4 + 62, w / 2 + 100, h / 4 + 82, I18n.get("menu.multiplayer"), () ->
			minecraft.gui.setScreen(new JoinMultiplayerScreen(minecraft.gui.screen()))
		));

		getWindow(0).addWidget(new WindowButtonWidget(w / 2 - 100, h / 4 + 86, w / 2 + 100, h / 4 + 106, I18n.get("menu.online"), () ->
			minecraft.gui.setScreen(new RealmsMainScreen(this))
		));

		getWindow(0).addWidget(new WindowButtonWidget(w / 2 - 124, h / 4 + 86, w / 2 - 104, h / 4 + 106, "MC", () -> {
			customTitleScreen = !customTitleScreen;
			BleachFileHelper.saveMiscSetting("customTitleScreen", new JsonPrimitive(false));
			minecraft.gui.setScreen(new TitleScreen(false));
		}));

		getWindow(0).addWidget(new WindowButtonWidget(w / 2 - 100, maxY, w / 2 - 2, maxY + 20, I18n.get("menu.options"), () ->
			minecraft.gui.setScreen(new OptionsScreen(minecraft.gui.screen(), minecraft.options, false))
		));

		getWindow(0).addWidget(new WindowButtonWidget(w / 2 + 2, maxY, w / 2 + 100, maxY + 20, I18n.get("menu.quit"), () ->
			minecraft.stop()
		));

		// Main Component
		getWindow(0).addWidget(new WindowTextWidget(Component.empty(), true, WindowTextWidget.TextAlign.MIDDLE, 3f, w / 2, h / 4 - 25, 0)
				.withRenderEvent((widget, ms, wx, wy) -> {
					MutableComponent bhText = Component.literal("");

					int i = 0;
					for (char c: "BleachHack".toCharArray()) {
						int fi = i++;
						bhText.append(
								Component.literal(String.valueOf(c)).withStyle(s -> s.withColor(TextColor.fromRgb(UI.getRainbowFromSettings(fi)))));
					}

					((WindowTextWidget) widget).setText(bhText);
				}));

		// Version Component
		getWindow(0).addWidget(new WindowTextWidget(BleachHack.VERSION, true, WindowTextWidget.TextAlign.MIDDLE, 1.5f, w / 2, h / 4 - 6, 0xffc050));

		// Splash
		getWindow(0).addWidget(new WindowTextWidget(Component.empty(), true, WindowTextWidget.TextAlign.MIDDLE, 2f, -20f, w / 2 + 80, h / 4 + 6, 0xffff00)
				.withRenderEvent((widget, ms, wx, wy) -> {
					if (splash != null) {
						WindowTextWidget windgetText = (WindowTextWidget) widget;
						windgetText.setText(Component.literal(splash));
						windgetText.color = (windgetText.color & 0x00ffffff) | ((splashTicks * 17) << 24);

						float scale = 1.8F - Mth.abs(Mth.sin(Util.getMillis() % 1000L / 1000.0F * 6.2831855F) * 0.1F);
						scale = scale * 66.0F / (font.width(splash) + 32);
						windgetText.setScale(scale);
					}
				}));

		// Update Component
		JsonObject updateJson = BleachHack.getUpdateJson();
		if (updateJson != null && updateJson.has("version") && updateJson.get("version").getAsInt() > BleachHack.INTVERSION) {
			getWindow(0).addWidget(new WindowTextWidget("§6§nUpdate§6", true, 4, h - 12, 0xffffff)
					.withClickEvent((widget, mx, my, wx, wy) ->
						minecraft.gui.setScreen(new UpdateScreen(minecraft.gui.screen(), updateJson))
					));
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
		// background is drawn by the vanilla extractBackground pass before this is called

		int copyWidth = this.font.width("Copyright Mojang AB. Do not distribute!") + 2;
		drawContext.text(font, "Copyright Mojang AB. Do not distribute!", width - copyWidth, height - 10, -1);
		drawContext.text(font, "Fabric: " + FabricLoader.getInstance().getModContainer("fabricloader").get().getMetadata().getVersion().getFriendlyString(),
				4, height - 30, -1);
		drawContext.text(font, "Minecraft: " + SharedConstants.getCurrentVersion().name(), 4, height - 20, -1);
		drawContext.text(font, "Logged in as: §a" + minecraft.getUser().getName(), 4, height - 10, -1);

		super.extractRenderState(drawContext, mouseX, mouseY, delta);

		particleMang.addParticle(mouseX, mouseY);
		particleMang.renderParticles(drawContext);

	}

	@Override
	public void tick() {
		if (splash != null && splashTicks < 15)
			splashTicks++;
	}
}
