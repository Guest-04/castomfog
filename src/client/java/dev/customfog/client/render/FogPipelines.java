package dev.customfog.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import dev.customfog.CustomFog;

public final class FogPipelines {
	public static final Identifier PIPELINE_ID = Identifier.fromNamespaceAndPath(CustomFog.MOD_ID, "pipeline/volumetric_fog");
	public static final Identifier SHADER_ID = Identifier.fromNamespaceAndPath(CustomFog.MOD_ID, "volumetric_fog");

	public static final RenderPipeline VOLUMETRIC_FOG = RenderPipelines.register(
			RenderPipeline.builder()
					.withLocation(PIPELINE_ID)
					.withVertexShader(SHADER_ID)
					.withFragmentShader(SHADER_ID)
					.withSampler("DepthSampler")
					.withUniform("FogVolume", UniformType.UNIFORM_BUFFER)
					.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
					.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
					.withCull(false)
					.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
					.build()
	);

	private FogPipelines() {
	}
}
