package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventRenderBlockOutline;
import org.bleachhack.event.events.EventWorldRender;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingColor;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.util.render.Renderer;
import org.bleachhack.util.render.color.QuadColor;
import org.bleachhack.util.shader.BleachCoreShaders;
import org.bleachhack.util.shader.ColorVertexConsumerProvider;
import org.bleachhack.util.shader.ShaderEffectWrapper;
import org.bleachhack.util.shader.ShaderLoader;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class BlockHighlight extends Module {

	private ShaderEffectWrapper shader;
	private ColorVertexConsumerProvider colorVertexer;

	public BlockHighlight() {
		super("BlockHighlight", KEY_UNBOUND, ModuleCategory.RENDER, "Highlights blocks that you're looking at.",
				new SettingMode("Render", "Shader", "AABB").withDesc("The Render mode."),
				new SettingSlider("ShaderFill", 1, 255, 50, 0).withDesc("How opaque the fill on shader mode should be."),
				new SettingSlider("AABB", 0, 5, 2, 1).withDesc("How thick the box outline should be."),
				new SettingSlider("BoxFill", 0, 255, 50, 0).withDesc("How opaque the fill on box mode should be."),
				new SettingColor("Color", 0, 128, 128).withDesc("The color of the highlight."));
	}

	@Override
	public void onEnable(boolean inWorld) {
		super.onEnable(inWorld);

		try {
			shader = new ShaderEffectWrapper(
					ShaderLoader.loadEffect(mc.gameRenderer.mainRenderTarget(), Identifier.fromNamespaceAndPath("bleachhack", "shaders/post/entity_outline.json")));

			colorVertexer = new ColorVertexConsumerProvider(shader.getFramebuffer("main"), BleachCoreShaders::getColorOverlayShader);
		} catch (Exception e) {
			e.printStackTrace();
			setEnabled(false);
		}
	}

	@BleachSubscribe
	public void onRenderBlockOutline(EventRenderBlockOutline event) {
		event.setCancelled(true);
	}

	@BleachSubscribe
	public void onWorldRender(EventWorldRender.Post event) {
		int mode = getSetting(0).asMode().getMode();

		if (!(mc.hitResult instanceof BlockHitResult))
			return;

		BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
		BlockState state = mc.level.getBlockState(pos);
		if (!state.isRedstoneConductor(mc.level, pos) || !mc.level.getWorldBorder().isWithinBounds(pos)) {
			return;
		}

		int[] color = this.getSetting(4).asColor().getRGBArray();
		if (mode == 0) {
			// TODO(26.2): shader mode is inert — the immediate BlockEntityRenderer.render(...)
			// and BlockModelRenderer.renderFlat(...) calls that drew the tinted block into the
			// shader framebuffer are gone (block/BE rendering is extract/submit based now) and
			// the shader pipeline itself is stubbed. Frame flow kept so the feature can be
			// rebuilt on the submit pipeline.
			shader.prepare();
			shader.clearFramebuffer("main");
			colorVertexer.draw();
			shader.render();
			shader.drawFramebufferToMain("main");
		} else {
			AABB box = state.getShape(mc.level, pos).bounds().move(pos);
			float width = getSetting(2).asSlider().getValueFloat();
			int fill = getSetting(3).asSlider().getValueInt();

			if (width != 0)
				Renderer.drawBoxOutline(box, QuadColor.single(color[0], color[1], color[2], 255), width);

			if (fill != 0)
				Renderer.drawBoxFill(box, QuadColor.single(color[0], color[1], color[2], fill));
		}
	}
}
