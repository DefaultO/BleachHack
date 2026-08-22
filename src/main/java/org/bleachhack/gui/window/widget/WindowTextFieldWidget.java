package org.bleachhack.gui.window.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;


public class WindowTextFieldWidget extends WindowWidget {

	public EditBox textField;

	public WindowTextFieldWidget(int x, int y, int width, int height, String text) {
		super(x, y, x + width, y + height);
		this.textField = new EditBox(mc.textRenderer, x, y, width, height, Component.empty());
		this.textField.setText(text);
		this.textField.setMaxLength(32767);
	}

	protected WindowTextFieldWidget(int x, int y, int width, int height) {
		super(x, y, x + width, y + height);
	}

	@Override
	public void render(GuiGraphics drawContext, int windowX, int windowY, int mouseX, int mouseY) {
		textField.setX(windowX + x1);
		textField.setY(windowY + y1);
		textField.render(drawContext, mouseX, mouseY, Minecraft.getInstance().getTickDelta());

		super.render(drawContext, windowX, windowY, mouseX, mouseY);
	}

	@Override
	public void mouseClicked(int windowX, int windowY, int mouseX, int mouseY, int button) {
		super.mouseClicked(windowX, windowY, mouseX, mouseY, button);

		textField.setFocused(textField.mouseClicked(mouseX, mouseY, button));
	}

	@Override
	public void tick() {
		super.tick();
		//textField.tick();
	}

	@Override
	public void charTyped(char chr, int modifiers) {
		super.charTyped(chr, modifiers);

		textField.charTyped(chr, modifiers);
	}

	@Override
	public void keyPressed(int keyCode, int scanCode, int modifiers) {
		super.keyPressed(keyCode, scanCode, modifiers);

		textField.keyPressed(keyCode, scanCode, modifiers);
	}
}
