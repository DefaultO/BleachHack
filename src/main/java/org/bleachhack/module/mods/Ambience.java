/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventBiomeColor;
import org.bleachhack.event.events.EventPacket;
import org.bleachhack.event.events.EventSkyRender;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingColor;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;

import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

public class Ambience extends Module {

	private final WeatherManager weatherManager = new WeatherManager();

	public Ambience() {
		super("Ambience", KEY_UNBOUND, ModuleCategory.WORLD, "Changes the world ambience.",
				new SettingToggle("Weather", true).withDesc("Changes the world weather.").withChildren(
						new SettingMode("Weather", "Clear", "Rain").withDesc("What weather to use."),
						new SettingSlider("Rain", 0, 2, 0, 2).withDesc("How much it should rain in rain mode.")),
				new SettingToggle("Time", false).withDesc("Changes the world time.").withChildren(
						new SettingSlider("Time", 0, 24000, 12500, 0).withDesc("What time to set the world to.")),
				new SettingToggle("Overworld", true).withDesc("Changes the overworld ambience-").withChildren(
						new SettingToggle("Sky Color", true).withDesc("Changes the overworld sky color.").withChildren(
								new SettingToggle("End Skybox", false).withDesc("2B2T QUeue SKY=!?!?!?"),
								new SettingColor("Sky Color", 128, 255, 128).withDesc("Main color of the sky.")),
						new SettingToggle("Foilage Color", false).withDesc("Changes the foilage color.").withChildren(
								new SettingColor("Color", 128, 255, 128).withDesc("The color of the foilage.")),
						new SettingToggle("Water Color", false).withDesc("Changes the water color.").withChildren(
								new SettingColor("Color", 128, 255, 128).withDesc("Color of the water."))),
				new SettingToggle("Nether", true).withDesc("Changes the nether ambience.").withChildren(
						new SettingToggle("Sky Color", true).withDesc("Changes the nether sky color.").withChildren(
								new SettingToggle("End Skybox", false).withDesc("2B2T QUeue SKY=!?!?!?"),
								new SettingColor("Sky Color", 128, 255, 128).withDesc("Main color of the sky.")),
						new SettingToggle("Foilage Color", false).withDesc("Changes the foilage color.").withChildren(
								new SettingColor("Color", 128, 255, 128).withDesc("The color of the foilage.")),
						new SettingToggle("Water Color", false).withDesc("Changes the water color").withChildren(
								new SettingColor("Color", 128, 255, 128).withDesc("The color of the water."))),
				new SettingToggle("End", true).withDesc("Changes the end ambience.").withChildren(
						new SettingToggle("Sky Color", true).withDesc("Changes the end sky color.").withChildren(
								new SettingToggle("End Skybox", false).withDesc("2B2T QUeue SKY=!?!?!?"),
								new SettingColor("Sky Color", 128, 255, 128).withDesc("Main color of the sky.")),
						new SettingToggle("Foilage Color", false).withDesc("Changes the foilage color.").withChildren(
								new SettingColor("Color", 128, 255, 128).withDesc("The color of the foilage.")),
						new SettingToggle("Water Color", false).withDesc("Changes the water color.").withChildren(
								new SettingColor("Color", 128, 255, 128).withDesc("The color of the water."))));
	}

	@Override
	public void onDisable(boolean inWorld) {
		if (inWorld)
			weatherManager.applyWeather(mc.level);

		weatherManager.reset();

		super.onDisable(inWorld);
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (getSetting(0).asToggle().getState()) {
			float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

			if (!weatherManager.isActive()) {
				weatherManager.setRain(mc.level.getRainLevel(tickDelta));
				weatherManager.setThunder(mc.level.getThunderLevel(tickDelta));
			}

			// isRaining() is derived from the rain level in 26.2, no separate raining flag anymore
			if (getSetting(0).asToggle().getChild(0).asMode().getMode() == 0) {
				mc.level.setRainLevel(0f);
			} else {
				mc.level.setRainLevel(getSetting(0).asToggle().getChild(1).asSlider().getValueFloat());
			}
		} else if (weatherManager.isActive()) {
			weatherManager.applyWeather(mc.level);
			weatherManager.reset();
		}

		if (getSetting(1).asToggle().getState()) {
			// TODO(26.2): day time is data-driven now (WorldClock system); freeze the dimension's clock at the chosen time
			long time = getSetting(1).asToggle().getChild(0).asSlider().getValueLong();
			mc.level.dimensionType().defaultClock().ifPresent(clock -> mc.level.clockManager()
					.handleUpdates(mc.level.getGameTime(), Map.<Holder<WorldClock>, ClockNetworkState>of(clock, new ClockNetworkState(time, 0f, 0f))));
			mc.level.environmentAttributes().invalidateTickCache();
		}
	}

	@BleachSubscribe
	public void readPacket(EventPacket.Read event) {
		if (event.getPacket() instanceof ClientboundGameEventPacket && getSetting(0).asToggle().getState()) {
			ClientboundGameEventPacket packet = (ClientboundGameEventPacket) event.getPacket();
			if (packet.getEvent() == ClientboundGameEventPacket.START_RAINING) {
				weatherManager.setRain(1f);
			} else if (packet.getEvent() == ClientboundGameEventPacket.STOP_RAINING) {
				weatherManager.setRain(0f);
			} else if (packet.getEvent() == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
				weatherManager.setRain(packet.getParam());
			} else if (packet.getEvent() == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
				weatherManager.setThunder(packet.getParam());
			} else {
				return;
			}

			event.setCancelled(true);
		} else if (event.getPacket() instanceof ClientboundDisconnectPacket && getSetting(0).asToggle().getState()) {
			weatherManager.reset();
		} else if (event.getPacket() instanceof ClientboundSetTimePacket && getSetting(1).asToggle().getState()) {
			event.setCancelled(true);
		}
	}

	@BleachSubscribe
	public void onBiomeColor(EventBiomeColor event) {
		int type = event instanceof EventBiomeColor.Water ? 2 : 1;

		if (getCurrentDimSetting().getState() && getCurrentDimSetting().getChild(type).asToggle().getState()) {
			event.setColor(getCurrentDimSetting().getChild(type).asToggle().getChild(0).asColor().getRGB());
		}
	}

	@BleachSubscribe
	public void onSkyColor(EventSkyRender.Color event) {
		if (getCurrentDimSetting().getState() && getCurrentDimSetting().getChild(0).asToggle().getState()) {
			int[] color = getCurrentDimSetting().getChild(0).asToggle().getChild(1).asColor().getRGBArray();
			event.setColor(new Vec3(color[0] / 255d, color[1] / 255d, color[2] / 255d));
		}
	}

	@BleachSubscribe
	public void onSkyProperties(EventSkyRender.Properties event) {
		// TODO(26.2): DimensionEffects/DimensionSpecialEffects was removed; the sky is now data-driven
		// (SkyRenderer + DimensionType.Skybox + EnvironmentAttributes) and EventSkyRender.Properties is
		// an inert Object holder. The "End Skybox" override can't be reimplemented here until the sky
		// event pipeline is rebuilt. Sky *color* still works via onSkyColor above.
	}

	private SettingToggle getCurrentDimSetting() {
		return getSetting(mc.level.dimension() == Level.END ? 4 : mc.level.dimension() == Level.NETHER ? 3 : 2).asToggle();
	}

	private static class WeatherManager {

		private float rain = -1f;
		private float thunder = -1f;

		public void setRain(float rain) {
			this.rain = rain;
		}

		public void setThunder(float thunder) {
			this.thunder = thunder;
		}

		public void reset() {
			rain = -1f;
			thunder = -1f;
		}

		public void applyWeather(Level world) {
			if (rain >= 0f) {
				world.setRainLevel(rain);
			}

			if (thunder >= 0f) {
				world.setThunderLevel(thunder);
			}
		}

		public boolean isActive() {
			return rain >= 0f || thunder >= 1f;
		}
	}
}
