package org.bleachhack.gui.window.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class WindowTextWidget extends WindowWidget {

	private Component text;
	private float scale;
	public boolean shadow;
	public int color;
	public TextAlign align;
	public float rotation;

	public WindowTextWidget(String text, boolean shadow, int x, int y, int color) {
		this(text, shadow, TextAlign.LEFT, x, y, color);
	}

	public WindowTextWidget(Component text, boolean shadow, int x, int y, int color) {
		this(text, shadow, TextAlign.LEFT, x, y, color);
	}

	public WindowTextWidget(String text, boolean shadow, TextAlign align, int x, int y, int color) {
		this(text, shadow, align, 1f, x, y, color);
	}

	public WindowTextWidget(Component text, boolean shadow, TextAlign align, int x, int y, int color) {
		this(text, shadow, align, 1f, x, y, color);
	}

	public WindowTextWidget(String text, boolean shadow, TextAlign align, float scale, int x, int y, int color) {
		this(Component.literal(text), shadow, align, scale, x, y, color);
	}

	public WindowTextWidget(Component text, boolean shadow, TextAlign align, float scale, int x, int y, int color) {
		this(text, shadow, align, scale, 0f, x, y, color);
	}

	public WindowTextWidget(Component text, boolean shadow, TextAlign align, float scale, float rotation, int x, int y, int color) {
		super(x, y, x + mc.font.width(text), (int) (y + 10 * scale));
		this.text = text;
		this.shadow = shadow;
		this.color = color;
		this.align = align;
		this.scale = scale;
		this.rotation = rotation;
	}

	@Override
	public void render(GuiGraphicsExtractor drawContext, int windowX, int windowY, int mouseX, int mouseY) {
		super.render(drawContext, windowX, windowY, mouseX, mouseY);

		float offset = mc.font.width(text) * align.offset * scale;

		drawContext.pose().pushMatrix();
		drawContext.pose().scale(scale, scale);
		drawContext.pose().translate((windowX + x1 - offset) / scale, (windowY + y1) / scale);
		drawContext.pose().rotate((float) Math.toRadians(rotation));

		// old TextRenderer treated zero-alpha colors as opaque; 26.2 text() skips them instead
		int col = (color & 0xfc000000) == 0 ? color | 0xff000000 : color;
		drawContext.text(mc.font, text, 0, 0, col, shadow);

		// Text hover effect was already disabled before the 26.2 migration:
		//((AccessorScreen) mc.currentScreen).callRenderTextHoverEffect(drawContext, text.getStyle(), mouseX - (windowX + x1 - (int) offset), mouseY - (windowY + y1));

		drawContext.pose().popMatrix();
	}

	public Component getText() {
		return text;
	}

	public void setText(Component text) {
		this.text = text;
		this.x2 = x1 + mc.font.width(text);
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
		this.x2 = (int) (x1 + mc.font.width(text) * scale);
		this.y2 = (int) (y1 + 10 * scale);
	}

	public enum TextAlign {
		LEFT(0f),
		MIDDLE(0.5f),
		RIGHT(1f);

		public final float offset;

		TextAlign(float offset) {
			this.offset = offset;
		}
	}

}
