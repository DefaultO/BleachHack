package org.bleachhack.gui.window.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import org.bleachhack.gui.window.Window;

public class WindowCheckboxWidget extends WindowWidget {

	public boolean checked;
	public Component text;

	public WindowCheckboxWidget(int x, int y, String text, boolean pressed) {
		this(x, y, Component.literal(text), pressed);
	}

	public WindowCheckboxWidget(int x, int y, Component text, boolean pressed) {
		super(x, y, 10 + Minecraft.getInstance().font.width(text), 10);
		this.checked = pressed;
		this.text = text;
	}

	@Override
	public void render(GuiGraphicsExtractor drawContext, int windowX, int windowY, int mouseX, int mouseY) {
		super.render(drawContext, windowX, windowY, mouseX, mouseY);

		Font textRenderer = Minecraft.getInstance().font;

		int x = windowX + x1;
		int y = windowY + y1;
		int color = mouseX >= x && mouseX <= x + 10 && mouseY >= y && mouseY <= y + 10 ? 0x906060ff : 0x9040409f;

		Window.fill(drawContext, x, y, x + 11, y + 11, color);

		// 26.2 text() skips zero-alpha colors (old TextRenderer forced them opaque), hence the ff alpha
		if (checked) {
			drawContext.text(textRenderer, "✔", x + 2, y + 2, 0xffffeeff);
			//fill(matrix, x + 3, y + 3, x + 7, y + 7, 0xffffffff);
		}
		drawContext.text(textRenderer, text, x + 15, y + 2, 0xffc0c0c0);
	}

	@Override
	public void mouseClicked(int windowX, int windowY, int mouseX, int mouseY, int button) {
		super.mouseClicked(windowX, windowY, mouseX, mouseY, button);

		if (mouseX >= windowX + x1 && mouseX <= windowX + x1 + 10 && mouseY >= windowY + y1 && mouseY <= windowY + y1 + 10) {
			checked = !checked;
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		}
	}
}
