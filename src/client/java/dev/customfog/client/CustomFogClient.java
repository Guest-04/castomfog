package dev.customfog.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import dev.customfog.CustomFog;
import dev.customfog.client.config.FogConfigLoader;
import dev.customfog.client.render.FogPipelines;
import dev.customfog.client.render.VolumetricFogRenderer;

public class CustomFogClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FogConfigLoader.load();
		if (FogPipelines.VOLUMETRIC_FOG == null) {
			CustomFog.LOGGER.error("Volumetric fog pipeline failed to register");
			return;
		}

		LevelRenderEvents.END_EXTRACTION.register(VolumetricFogRenderer::extract);
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(VolumetricFogRenderer::drawVanillaTarget);
		LevelRenderEvents.END_MAIN.register(VolumetricFogRenderer::captureIrisDepth);
	}
}
