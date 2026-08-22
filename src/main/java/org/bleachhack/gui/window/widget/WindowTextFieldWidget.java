package org.bleachhack.gui.window.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;


public class WindowTextFieldWidget extends WindowWidget {

	public EditBox textField;

	public WindowTextFieldWidget(int x, int y, int width, int height, String text) {
		super(x, y, x + width, y + height);
		this.textField = new EditBox(mc.font, x, y, width, height, Component.empty());
		this.textField.setValue(text);
		this.textField.setMaxLength(32767);
	}

	protected WindowTextFieldWidget(int x, int y, int width, int height) {
		super(x, y, x + width, y + height);
	}

	@Override
	public void render(GuiGraphicsExtractor drawContext, int windowX, int windowY, int mouseX, int mouseY) {
		textField.setX(windowX + x1);
		textField.setY(windowY + y1);
		textField.extractRenderState(drawContext, mouseX, mouseY, 0f);

		super.render(drawContext, windowX, windowY, mouseX, mouseY);
	}

	@Override
	public void mouseClicked(int windowX, int windowY, int mouseX, int mouseY, int button) {
		super.mouseClicked(windowX, windowY, mouseX, mouseY, button);

		MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));
		textField.setFocused(textField.mouseClicked(event, false));
	}

	@Override
	public void tick() {
		super.tick();
		//textField.tick();
	}

	@Override
	public void charTyped(char chr, int modifiers) {
		super.charTyped(chr, modifiers);

		textField.charTyped(new CharacterEvent(chr));
	}

	@Override
	public void keyPressed(int keyCode, int scanCode, int modifiers) {
		super.keyPressed(keyCode, scanCode, modifiers);

		textField.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
	}
}
