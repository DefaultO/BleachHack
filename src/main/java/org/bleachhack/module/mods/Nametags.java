/*
 * This file is part of the BleachHack distribution (https://github.com/BleachDev/BleachHack/).
 * Copyright (c) 2021 Bleach and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package org.bleachhack.module.mods;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.ChatFormatting;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import org.apache.commons.lang3.StringUtils;
import org.bleachhack.BleachHack;
import org.bleachhack.command.commands.CmdEntityStats;
import org.bleachhack.event.events.EventEntityRender;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.event.events.EventWorldRender;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.setting.module.SettingToggle;
import org.bleachhack.util.BleachLogger;
import org.bleachhack.util.render.Renderer;
import org.bleachhack.util.render.WorldRenderer;
import org.bleachhack.util.world.EntityUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

public class Nametags extends Module {

	private ExecutorService uuidExecutor;
	// list of future client licenses
	private Map<UUID, Future<String>> uuidFutures = new HashMap<>();

	private Queue<UUID> uuidQueue = new ArrayDeque<>();
	private Map<UUID, String> uuidCache = new HashMap<>();
	private Set<UUID> failedUUIDs = new HashSet<>();
	private long lastLookup = 0;

	public Nametags() {
		super("Nametags", KEY_UNBOUND, ModuleCategory.RENDER, "Shows bigger/cooler nametags above entities.",
				new SettingMode("Health", "Number", "NumberOf", "Bar", "Percent").withDesc("How to show health."),
				new SettingToggle("Players", true).withDesc("Shows nametags over player.").withChildren(
						new SettingSlider("Size", 0.5, 5, 2, 1).withDesc("The size of the nametags."),
						new SettingToggle("Inventory", true).withDesc("Shows the equipment of the player."),
						new SettingToggle("Name", true).withDesc("Shows the name of the player."),
						new SettingToggle("Health", true).withDesc("Shows the health of the player."),
						new SettingToggle("Ping", true).withDesc("Shows the ping of the player."),
						new SettingToggle("Gamemode", false).withDesc("Shows the gamemode of the player.")),
				new SettingToggle("Animals", true).withDesc("Shows nametags over animals.").withChildren(
						new SettingSlider("Size", 0.5, 5, 1, 1).withDesc("The size of the nametags."),
						new SettingToggle("Inventory", true).withDesc("Shows the equipment of the animal."),
						new SettingToggle("Name", true).withDesc("Shows the name of the animal."),
						new SettingToggle("Health", true).withDesc("Shows the health of the animal."),
						new SettingToggle("Tamed", false).withDesc("Shows if the animal is tamed.").withChildren(
								new SettingMode("If Not", "Show", "Hide").withDesc("What to show if the animal isn't tamed.")),
						new SettingToggle("Owner", true).withDesc("Shows the owner of the pet if its tamed."),
						new SettingToggle("HorseStats", false).withDesc("Shows the entities stats if its a horse.")),
				new SettingToggle("Mobs", false).withDesc("Shows nametags over mobs.").withChildren(
						new SettingSlider("Size", 0.5, 5, 1, 1).withDesc("The size of the nametags."),
						new SettingToggle("Inventory", true).withDesc("Shows the equipment of the mob."),
						new SettingToggle("Name", true).withDesc("Shows the name of the mob."),
						new SettingToggle("Health", true).withDesc("Shows the health of the mob.")),
				new SettingToggle("Items", true).withDesc("Shows nametags for items.").withChildren(
						new SettingSlider("Size", 0.5, 5, 1, 1).withDesc("Size of the nametags."),
						new SettingToggle("CustomName", true).withDesc("Shows the items custom name if it has it."),
						new SettingToggle("ItemCount", true).withDesc("Shows how many items are in the stack.")),
				new SettingToggle("ArmorStands", false).withDesc("Shows nametags over armor stands of their eqipment.").withChildren(
						new SettingSlider("Size", 0.5, 5, 1, 1).withDesc("The size of the nametags.")));
	}

	@Override
	public void onDisable(boolean inWorld) {
		uuidQueue.clear();
		failedUUIDs.clear();
		uuidExecutor.shutdownNow();
		uuidFutures.clear();

		Map<UUID, String> cacheCopy = new HashMap<>(uuidCache);
		uuidCache.clear();

		cacheCopy.forEach((u, s) -> {
			if (!s.startsWith("§c")) uuidCache.put(u, s);
		});

		super.onDisable(inWorld);
	}

	@Override
	public void onEnable(boolean inWorld) {
		super.onEnable(inWorld);

		uuidExecutor = Executors.newFixedThreadPool(4);
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		// collect revenue from all the future copies
		for (Entry<UUID, Future<String>> f: new HashMap<>(uuidFutures).entrySet()) {
			if (f.getValue().isDone()) {
				try {
					String s = f.getValue().get();
					uuidCache.put(f.getKey(), s);

					uuidFutures.remove(f.getKey());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}

		if (!uuidQueue.isEmpty() && System.currentTimeMillis() - lastLookup > 1000) {
			lastLookup = System.currentTimeMillis();
			addUUIDFuture(uuidQueue.poll());
		}
	}

	@BleachSubscribe
	public void onLivingLabelRender(EventEntityRender.Single.Label event) {
		if ((event.getEntity() instanceof Player && getSetting(1).asToggle().getState())
				|| (EntityUtils.isAnimal(event.getEntity()) && getSetting(2).asToggle().getState())
				|| (event.getEntity() instanceof Enemy && getSetting(3).asToggle().getState())
				|| (event.getEntity() instanceof ItemEntity && getSetting(4).asToggle().getState())
				|| (event.getEntity() instanceof ArmorStand && getSetting(5).asToggle().getState()))
			event.setCancelled(true);
	}

	@BleachSubscribe
	public void onWorldRender(EventWorldRender.Post event) {
		for (Entity entity: mc.level.entitiesForRendering()) {
			if (entity == mc.player || entity.hasPassenger(mc.player) || mc.player.hasPassenger(entity)) {
				continue;
			}

			Vec3 rPos = entity.position().subtract(Renderer.getInterpolationOffset(entity)).add(0, entity.getBbHeight() + 0.25, 0);

			if (entity instanceof Player && getSetting(1).asToggle().getState()) {
				double scale = Math.max(getSetting(1).asToggle().getChild(0).asSlider().getValue() * (mc.getCameraEntity().distanceTo(entity) / 20), 1);

				List<Component> lines = getPlayerLines((Player) entity);
				drawLines(rPos.x, rPos.y, rPos.z, scale, lines);

				if (getSetting(1).asToggle().getChild(1).asToggle().getState()) {
					drawItems(rPos.x, rPos.y + (lines.size() + 1) * 0.25 * scale, rPos.z, scale, getMainEquipment(entity));
				}
			} else if (EntityUtils.isAnimal(entity) && getSetting(2).asToggle().getState()) {
				double scale = Math.max(getSetting(2).asToggle().getChild(0).asSlider().getValue() * (mc.getCameraEntity().distanceTo(entity) / 20), 1);

				List<Component> lines = getAnimalLines((LivingEntity) entity);
				drawLines(rPos.x, rPos.y, rPos.z, scale, lines);

				if (getSetting(2).asToggle().getChild(1).asToggle().getState() && entity instanceof Fox) {
					drawItems(rPos.x, rPos.y + (lines.size() + 1) * 0.25 * scale, rPos.z, scale, List.of(((Fox) entity).getMainHandItem()));
				}
			} else if (entity instanceof Enemy && getSetting(3).asToggle().getState()) {
				double scale = Math.max(getSetting(3).asToggle().getChild(0).asSlider().getValue() * (mc.getCameraEntity().distanceTo(entity) / 20), 1);

				List<Component> lines = getMobLines((LivingEntity) entity);
				drawLines(rPos.x, rPos.y, rPos.z, scale, lines);

				if (getSetting(3).asToggle().getChild(1).asToggle().getState()) {
					drawItems(rPos.x, rPos.y + (lines.size() + 1) * 0.25 * scale, rPos.z, scale, getMainEquipment(entity));
				}
			} else if (entity instanceof ItemEntity && getSetting(4).asToggle().getState()) {
				double scale = Math.max(getSetting(4).asToggle().getChild(0).asSlider().getValue() * (mc.getCameraEntity().distanceTo(entity) / 20), 1);

				List<Component> lines = getItemLines((ItemEntity) entity);
				drawLines(rPos.x, rPos.y, rPos.z, scale, lines);
			} else if (entity instanceof ArmorStand && getSetting(5).asToggle().getState()) {
				double scale = Math.max(getSetting(5).asToggle().getChild(0).asSlider().getValue() * (mc.getCameraEntity().distanceTo(entity) / 20), 1);

				drawItems(rPos.x, rPos.y + 0.25 * scale, rPos.z, scale, getMainEquipment(entity));
			}
		}
	}

	private void drawLines(double x, double y, double z, double scale, List<Component> lines) {
		double offset = lines.size() * 0.25 * scale;

		for (Component t: lines) {
			WorldRenderer.drawText(t, x, y + offset, z, scale, true);
			offset -= 0.25 * scale;
		}
	}

	private void drawItems(double x, double y, double z, double scale, List<ItemStack> items) {
		double lscale = scale * 0.4;

		for (int i = 0; i < items.size(); i++) {
			drawItem(x, y, z, i + 0.5 - items.size() / 2d, 0, lscale, items.get(i));
		}
	}

	private void drawItem(double x, double y, double z, double offX, double offY, double scale, ItemStack item) {
		if (item.isEmpty())
			return;

		WorldRenderer.drawGuiItem(x, y, z, offX * scale, offY * scale, scale, item);

		double w = mc.font.width("x" + item.getCount()) / 52d;
		WorldRenderer.drawText(Component.literal("x" + item.getCount()),
				x, y, z, (offX - w) * scale, (offY - 0.07) * scale, scale * 1.75, false);

		int c = 0;
		for (Object2IntMap.Entry<Holder<Enchantment>> m : EnchantmentHelper.getEnchantmentsForCrafting(item).entrySet()) {
			String text = Enchantment.getFullname(m.getKey(), m.getIntValue()).getString();

			if (text.isEmpty())
				continue;

			text = text.replaceFirst("Curse of (.)", "C$1");

			String subText = text.substring(0, Math.min(text.length(), 2)) + m.getIntValue();

			WorldRenderer.drawText(Component.literal(subText).withStyle(s -> s.withColor(TextColor.fromRgb(m.getKey().is(EnchantmentTags.CURSE) ? 0xff5050 : 0xffb0e0))),
					x, y, z, (offX + 0.02) * scale, (offY + 0.75 - c * 0.34) * scale, scale * 1.4, false);
			c--;
		}
	}

	private List<ItemStack> getMainEquipment(Entity e) {
		// Old getItemsEquipped() order with offhand moved last: [main, feet, legs, chest, head, off]
		LivingEntity le = (LivingEntity) e;
		return Lists.newArrayList(
				le.getItemBySlot(EquipmentSlot.MAINHAND),
				le.getItemBySlot(EquipmentSlot.FEET),
				le.getItemBySlot(EquipmentSlot.LEGS),
				le.getItemBySlot(EquipmentSlot.CHEST),
				le.getItemBySlot(EquipmentSlot.HEAD),
				le.getItemBySlot(EquipmentSlot.OFFHAND));
	}

	public List<Component> getPlayerLines(Player player) {
		List<Component> lines = new ArrayList<>();
		List<Component> mainText = new ArrayList<>();

		PlayerInfo playerEntry = mc.player.connection.getPlayerInfo(player.getGameProfile().id());

		if (getSetting(1).asToggle().getChild(4).asToggle().getState() && playerEntry != null) { // Ping
			mainText.add(Component.literal(playerEntry.getLatency() + "ms").withStyle(ChatFormatting.GRAY));
		}

		if (getSetting(1).asToggle().getChild(2).asToggle().getState()) { // Name
			mainText.add(player.getName().copy().withStyle(BleachHack.friendMang.has(player) ? ChatFormatting.AQUA : ChatFormatting.RED));
		}

		if (getSetting(1).asToggle().getChild(3).asToggle().getState()) { // Health
			if (getSetting(0).asMode().getMode() == 2) {
				lines.add(getHealthText(player));
			} else {
				mainText.add(getHealthText(player));
			}
		}

		if (getSetting(1).asToggle().getChild(5).asToggle().getState() && playerEntry != null) { // GM
			mainText.add(Component.literal("[" + playerEntry.getGameMode().toString().substring(0, playerEntry.getGameMode() == GameType.SPECTATOR ? 2 : 1) + "]").withStyle(ChatFormatting.GOLD));
		}

		if (!mainText.isEmpty())
			lines.add(ComponentUtils.formatList(mainText, Component.literal(" ")));

		return lines;
	}

	public List<Component> getAnimalLines(LivingEntity animal) {
		List<Component> lines = new ArrayList<>();

		if (animal instanceof AbstractHorse || animal instanceof TamableAnimal) {
			boolean tame = animal instanceof AbstractHorse
					? ((AbstractHorse) animal).isTamed() : ((TamableAnimal) animal).isTame();

			EntityReference<LivingEntity> ownerRef = animal instanceof AbstractHorse
					? ((AbstractHorse) animal).getOwnerReference() : ((TamableAnimal) animal).getOwnerReference();
			UUID ownerUUID = ownerRef == null ? null : ownerRef.getUUID();

			if (getSetting(2).asToggle().getChild(4).asToggle().getState() && !animal.isBaby()
					&& (getSetting(2).asToggle().getChild(4).asToggle().getChild(0).asMode().getMode() != 1 || tame)) {
				lines.add(0, Component.literal(tame ? "Tamed: Yes" : "Tamed: No").withStyle(tame ? ChatFormatting.GREEN : ChatFormatting.RED));
			}

			if (getSetting(2).asToggle().getChild(5).asToggle().getState() && ownerUUID != null) {
				if (uuidCache.containsKey(ownerUUID)) {
					lines.add(0, Component.literal("Owner: " + uuidCache.get(ownerUUID)).withStyle(ChatFormatting.GREEN));
				} else if (failedUUIDs.contains(ownerUUID)) {
					lines.add(0, Component.literal("Owner: " + ChatFormatting.GRAY + "Invalid UUID!").withStyle(ChatFormatting.GREEN));
				} else {
					// Try to see if the owner is online on the server before calling the mojang api
					Optional<GameProfile> owner = mc.player.connection.getOnlinePlayers().stream()
							.map(PlayerInfo::getProfile)
							.filter(profile -> profile != null && ownerUUID.equals(profile.id()) && profile.name() != null)
							.findFirst();

					if (owner.isPresent()) {
						uuidCache.put(ownerUUID, owner.get().name());
					} else if (!uuidQueue.contains(ownerUUID) && !uuidFutures.containsKey(ownerUUID)) {
						uuidQueue.add(ownerUUID);
					}

					lines.add(0, Component.literal("Owner: " + ChatFormatting.GRAY + "Loading...").withStyle(ChatFormatting.GREEN));
				}
			}

			if (getSetting(2).asToggle().getChild(6).asToggle().getState() && animal instanceof AbstractHorse) {
				AbstractHorse he = (AbstractHorse) animal;

				lines.add(0, Component.literal(
						CmdEntityStats.getSpeed(he) + " m/s" + ChatFormatting.GRAY + " | " + ChatFormatting.RESET + CmdEntityStats.getJumpHeight(he) + " Jump")
						.withStyle(ChatFormatting.GREEN));
			}
		}
		
		List<Component> mainText = new ArrayList<>();

		if (getSetting(2).asToggle().getChild(2).asToggle().getState()) { // Name
			mainText.add(animal.getName().copy().withStyle(ChatFormatting.GREEN));
		}

		if (getSetting(2).asToggle().getChild(3).asToggle().getState()) { // Health
			if (getSetting(0).asMode().getMode() == 2) {
				lines.add(0, getHealthText(animal));
			} else {
				mainText.add(getHealthText(animal));
			}
		}
		
		if (!mainText.isEmpty())
			lines.add(ComponentUtils.formatList(mainText, Component.literal(" ")));

		return lines;
	}

	public List<Component> getMobLines(LivingEntity mob) {
		List<Component> lines = new ArrayList<>();
		List<Component> mainText = new ArrayList<>();

		if (getSetting(3).asToggle().getChild(2).asToggle().getState()) { // Name
			mainText.add(mob.getName().copy().withStyle(ChatFormatting.DARK_PURPLE));
		}

		if (getSetting(3).asToggle().getChild(3).asToggle().getState()) { // Health
			if (getSetting(0).asMode().getMode() == 2) {
				lines.add(getHealthText(mob));
			} else {
				mainText.add(getHealthText(mob));
			}
		}

		if (!mainText.isEmpty())
			lines.add(ComponentUtils.formatList(mainText, Component.literal(" ")));

		return lines;
	}

	public List<Component> getItemLines(ItemEntity item) {
		List<Component> lines = new ArrayList<>();

		if (!item.getName().getString().equals(item.getItem().getHoverName().getString()) && getSetting(4).asToggle().getChild(1).asToggle().getState()) {
			lines.add(
					Component.literal("\"").withStyle(ChatFormatting.GOLD)
					.append(item.getItem().getHoverName().copy().withStyle(ChatFormatting.YELLOW))
					.append(Component.literal("\"").withStyle(ChatFormatting.GOLD)));
		}

		lines.add(item.getName().copy().withStyle(ChatFormatting.GOLD).append(getSetting(4).asToggle().getChild(2).asToggle().getState() ? ChatFormatting.YELLOW + " [x" + item.getItem().getCount() + "]" : ""));

		return lines;
	}

	private Component getHealthText(LivingEntity e) {
		int totalHealth = (int) (e.getHealth() + e.getAbsorptionAmount());

		if (getSetting(0).asMode().getMode() == 0) {
			return Component.literal(Integer.toString(totalHealth)).withStyle(s -> s.withColor(getHealthColor(e)));
		} else if (getSetting(0).asMode().getMode() == 1) {
			return Component.literal(Integer.toString(totalHealth) + ChatFormatting.GREEN + "/" + (int) e.getMaxHealth()).withStyle(s -> s.withColor(getHealthColor(e)));
		} else if (getSetting(0).asMode().getMode() == 2) {
			// Health bar
			String health = "";

			// - Add Green Normal Health
			health += ChatFormatting.GREEN + StringUtils.repeat('|', (int) e.getHealth());

			// - Add Yellow Absorption Health
			health += ChatFormatting.YELLOW + StringUtils.repeat('|', (int) Math.min(e.getAbsorptionAmount(), e.getMaxHealth() - e.getHealth()));

			// - Add Red Empty Health (Remove Based on absorption amount)
			health += ChatFormatting.RED + StringUtils.repeat('|', (int) e.getMaxHealth() - totalHealth);

			// - Add "+??" to the end if the entity has extra hearts
			if (totalHealth > (int) e.getMaxHealth()) {
				health += ChatFormatting.YELLOW + " +" + (totalHealth - (int) e.getMaxHealth());
			}

			return Component.literal(health);
		} else {
			return Component.literal((int) (totalHealth / e.getMaxHealth() * 100) + "%").withStyle(s -> s.withColor(getHealthColor(e)));
		}
	}

	private int getHealthColor(LivingEntity entity) {
		if (entity.getHealth() + entity.getAbsorptionAmount() > entity.getMaxHealth()) {
			return TextColor.fromLegacyFormat(ChatFormatting.YELLOW).getValue();
		} else {
			return Mth.hsvToRgb((entity.getHealth() + entity.getAbsorptionAmount()) / (entity.getMaxHealth() * 3), 1f, 1f);
		}
	}

	// how to download future client 2020 :flushed:
	private void addUUIDFuture(UUID uuid) {
		uuidFutures.put(uuid, uuidExecutor.submit(() -> {
			try {
				String url = "https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names";
				String response = Resources.toString(new URL(url), StandardCharsets.UTF_8);
				BleachLogger.logger.info("bruh uuid time: " + url);

				JsonElement json = JsonParser.parseString(response);

				if (!json.isJsonArray()) {
					BleachLogger.logger.error("[Nametags] Invalid Owner UUID: " + uuid);
					return "§c[Invalid]";
				}

				JsonArray ja = json.getAsJsonArray();

				return ja.get(ja.size() - 1).getAsJsonObject().get("name").getAsString();
			} catch (IOException e) {
				BleachLogger.logger.error("[Nametags] Error Getting Owner UUID: " + uuid);
				return "§c[Error]";
			}
		}));
	}
}
