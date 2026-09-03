package dev.customfog.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;

import dev.customfog.client.render.VolumetricFogRenderer;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Inject(method = "setupFog", at = @At("RETURN"))
	private void customfog$killVanillaAirFog(
			Camera camera,
			int renderDistanceInChunks,
			DeltaTracker deltaTracker,
			float darkenWorldAmount,
			ClientLevel level,
			CallbackInfoReturnable<FogData> cir
	) {
		FogData data = cir.getReturnValue();
		if (data == null || !VolumetricFogRenderer.shouldReplaceVanillaFog(camera)) {
			return;
		}

		float far = VolumetricFogRenderer.disabledDistance();
		data.environmentalStart = far;
		data.environmentalEnd = far * 2f;
		data.renderDistanceStart = far;
		data.renderDistanceEnd = far * 2f;
		data.skyEnd = far;
		data.cloudEnd = far;
	}
}
