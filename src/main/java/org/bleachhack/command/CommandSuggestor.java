package org.bleachhack.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bleachhack.BleachHack;
import org.bleachhack.event.events.EventKeyPress;
import org.bleachhack.event.events.EventOpenScreen;
import org.bleachhack.event.events.EventRenderInGameHud;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.mixin.AccessorChatScreen;
import org.bleachhack.setting.option.Option;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;

public class CommandSuggestor {

	private static CommandSuggestor INSTANCE;

	private String curText = "";
	private List<String> suggestions = new ArrayList<>();
	private int selected = -1;
	private int scroll;

	public static CommandSuggestor getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new CommandSuggestor();
		}

		return INSTANCE;
	}

	public static void start() {
		BleachHack.eventBus.subscribe(getInstance());
	}

	public static void stop() {
		getInstance().reset();
		BleachHack.eventBus.unsubscribe(getInstance());
	}

	@BleachSubscribe
	public void onDrawOverlay(EventRenderInGameHud event) {
		if (!Option.CHAT_SHOW_SUGGESTIONS.getValue())
			return;

		Screen screen = Minecraft.getInstance().gui.screen();

		if (screen instanceof ChatScreen) {
			EditBox field = ((AccessorChatScreen) screen).getChatField();
			String text = field.getValue();

			if (!text.equals(curText)) {
				suggestions.clear();
				curText = text;

				if (text.startsWith(Command.getPrefix())) {
					suggestions.addAll(CommandManager.getSuggestionProvider().getSuggestions(text.substring(Command.getPrefix().length()).split(" ", -1)));
				}

				selected = 0;
				scroll = 0;
			}

			if (selected >= 0 && selected < suggestions.size()) {
				String[] split = field.getValue().split(" ", -1);
				int offset = split[split.length - 1].length() - (split.length == 1 ? Command.getPrefix().length() : 0);

				if (offset > suggestions.get(selected).length()) {
					field.setSuggestion("");
				} else {
					field.setSuggestion(suggestions.get(selected).substring(offset));
				}
			}

			if (!suggestions.isEmpty()) {
				// TODO(26.2): z-translate(0, 0, 200) removed - the 2D pose stack has no z; layering is handled by gui strata now
				event.getContext().pose().pushMatrix();

				int length = suggestions.stream()
						.map(s -> Minecraft.getInstance().font.width(s))
						.min(Comparator.reverseOrder()).orElse(0);

				int startX = Minecraft.getInstance().font.width(
						field.getValue().replaceFirst("[^ ]*$", "") + (!field.getValue().contains(" ") ? Command.getPrefix() : "")) + 3;
				int startY = screen.height - Math.min(suggestions.size(), 10) * 12 - 15;
				for (int i = scroll; i < suggestions.size() && i < scroll + 10; i++) {
					String suggestion = suggestions.get(i);

					event.getContext().fill(startX, startY, startX + length + 2, startY + 12, 0xd0000000);
					event.getContext().text(Minecraft.getInstance().font,
							suggestion, startX + 1, startY + 2, i == selected ? 0xffffff00 : 0xffb0b0b0);

					startY += 12;
				}

				event.getContext().pose().popMatrix();
			}
		}
	}

	@BleachSubscribe
	public void onKeyPressGlobal(EventKeyPress.Global event) {
		if (event.getAction() != 0 && !suggestions.isEmpty() && !curText.isEmpty()) {
			if (event.getKey() == GLFW.GLFW_KEY_DOWN) {
				selected = selected >= suggestions.size() - 1 ? 0 : selected + 1;
				updateScroll();
			} else if (event.getKey() == GLFW.GLFW_KEY_UP) {
				selected = selected <= 0 ? suggestions.size() - 1 : selected - 1;
				updateScroll();
			} else if (event.getKey() == GLFW.GLFW_KEY_SPACE || event.getKey() == GLFW.GLFW_KEY_TAB) {
				if (selected >= 0 && selected < suggestions.size()) {
					EditBox field = ((AccessorChatScreen) Minecraft.getInstance().gui.screen()).getChatField();
					String[] split = field.getValue().split(" ", -1);
					int offset = split[split.length - 1].length() - (split.length == 1 ? Command.getPrefix().length() : 0);

					if (offset < suggestions.get(selected).length() && !suggestions.get(selected).matches("^<.*>$")) {
						field.setValue(field.getValue() + suggestions.get(selected).substring(offset));
					}
				}
			}
		}
	}

	@BleachSubscribe
	public void onKeyPressChat(EventKeyPress.InChat event) {
		EditBox field = ((AccessorChatScreen) Minecraft.getInstance().gui.screen()).getChatField();
		if (field.getValue().startsWith(Command.getPrefix())
				&& (event.getKey() == GLFW.GLFW_KEY_TAB || event.getKey() == GLFW.GLFW_KEY_UP || event.getKey() == GLFW.GLFW_KEY_DOWN)) {
			event.setCancelled(true);
		}
	}

	private void updateScroll() {
		if (scroll > selected) {
			scroll = Math.max(selected, 0);
		} else if (scroll + 10 <= selected) {
			scroll = Math.min(suggestions.size(), selected - 9);
		}
	}

	@BleachSubscribe
	public void onOpenScreen(EventOpenScreen event) {
		if (Minecraft.getInstance().gui.screen() instanceof ChatScreen) {
			reset();
		}
	}

	public void reset() {
		curText = "";
		suggestions.clear();
		selected = 0;
		scroll = 0;
	}
}