package org.bleachhack.gui.window.widget;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public class WindowPassTextFieldWidget extends WindowTextFieldWidget {

	public WindowPassTextFieldWidget(int x, int y, int width, int height, String text) {
		super(x, y, width, height);
		this.textField = new EditBox(mc.font, x, y, width, height, Component.empty());
		// ponytail: 26.2 EditBox renders through TextFormatters, replacing the old Font-subclass hide() hack
		this.textField.addFormatter((str, offset) -> FormattedCharSequence.forward("\u2022".repeat(str.length()), Style.EMPTY));
		this.textField.setValue(text);
		this.textField.setMaxLength(32767);
	}
}
