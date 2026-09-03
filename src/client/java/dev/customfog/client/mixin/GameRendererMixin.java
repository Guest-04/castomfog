package dev.customfog.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;

import dev.customfog.client.render.VolumetricFogRenderer;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void customfog$afterIris(DeltaTracker deltaTracker, CallbackInfo ci) {
		VolumetricFogRenderer.drawAfterIris();
	}

	@Inject(method = "close", at = @At("RETURN"))
	private void customfog$closeGpu(CallbackInfo ci) {
		VolumetricFogRenderer.close();
	}
}
