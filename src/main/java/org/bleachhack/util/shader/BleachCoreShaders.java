package org.bleachhack.util.shader;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;

import java.io.IOException;

public class BleachCoreShaders {
	
	private static final GlProgram COLOR_OVERLAY_SHADER;
	
	public static GlProgram getColorOverlayShader() {
		return COLOR_OVERLAY_SHADER;
	}
	
	static {
		try {
			COLOR_OVERLAY_SHADER = ShaderLoader.load(DefaultVertexFormat.POSITION_COLOR_TEXTURE, new Identifier("bleachhack", "color_overlay"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to initilize BleachHack core shaders", e);
		}
	}

}
