package org.bleachhack.util.shader;

import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import com.mojang.blaze3d.opengl.GlProgram;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.Identifier;

import java.io.IOException;

public class ShaderLoader {

	public static GlProgram load(VertexFormat format, Identifier id) throws IOException {
		ResourceManager resMang = Minecraft.getInstance().getResourceManager();
		
		return new GlProgram(new OpenResourceManager(resMang), id.toString(), format);
	}

	public static PostChain loadEffect(RenderTarget framebuffer, Identifier id) throws JsonSyntaxException, IOException {
		ResourceManager resMang = Minecraft.getInstance().getResourceManager();
		TextureManager texMang = Minecraft.getInstance().getTextureManager();
	
		return new PostChain(texMang, new OpenResourceManager(resMang), framebuffer, id);
	}

}
