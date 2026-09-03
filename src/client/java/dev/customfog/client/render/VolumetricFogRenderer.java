package dev.customfog.client.render;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import dev.customfog.CustomFog;
import dev.customfog.client.config.DimensionProfiles;
import dev.customfog.client.config.FogConfigLoader;
import dev.customfog.client.config.FogProfile;
import dev.customfog.client.iris.IrisCompat;

public final class VolumetricFogRenderer {
	public static final int FOG_VOLUME_SIZE = new Std140SizeCalculator()
			.putMat4f()
			.putVec4()
			.putVec4()
			.putVec4()
			.putVec4()
			.get();

	private static final float DISABLED_DISTANCE = 1.0e5f;

	private static FogFrameState frameState = FogFrameState.inactive();
	private static MappableRingBuffer volumeBuffer;
	private static boolean pipelineFailed;
	private static final Vector3f smoothedFogColor = new Vector3f(0.62f, 0.70f, 0.78f);
	private static boolean fogColorReady;
	private static GpuTexture capturedDepth;
	private static GpuTextureView capturedDepthView;
	private static int capturedWidth;
	private static int capturedHeight;
	private static boolean capturedReady;

	private VolumetricFogRenderer() {
	}

	public static boolean shouldExtractFog(Camera camera) {
		if (pipelineFailed || !FogConfigLoader.current().enabled() || IrisCompat.renderingShadowPass()) {
			return false;
		}
		if (camera == null) {
			return false;
		}

		FogType fogType = camera.getFluidInCamera();
		if (fogType == FogType.LAVA || fogType == FogType.POWDER_SNOW) {
			return false;
		}

		Entity entity = camera.entity();
		if (entity instanceof LivingEntity living
				&& (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) {
			return false;
		}

		return true;
	}

	public static boolean shouldReplaceVanillaFog(Camera camera) {
		return shouldExtractFog(camera);
	}

	public static float disabledDistance() {
		return DISABLED_DISTANCE;
	}

	public static void extract(LevelExtractionContext context) {
		Camera camera = context.camera();
		if (camera == null || context.level() == null || !shouldExtractFog(camera)) {
			frameState = FogFrameState.inactive();
			return;
		}

		CameraRenderState cameraState = context.levelState().cameraRenderState;
		boolean inWater = camera.getFluidInCamera() == FogType.WATER;
		FogProfile profile = inWater
				? FogConfigLoader.current().water()
				: DimensionProfiles.profileFor(context.level().dimension());
		int renderDistance = Math.max(2, Minecraft.getInstance().options.getEffectiveRenderDistance());
		float time = (context.levelState().gameTime + context.deltaTracker().getGameTimeDeltaPartialTick(false)) * 0.05f;

		Matrix4f view = new Matrix4f(cameraState.viewRotationMatrix);
		Vec3 pos = cameraState.pos;
		view.translate(-(float) pos.x, -(float) pos.y, -(float) pos.z);
		Matrix4f invProjView = new Matrix4f(cameraState.projectionMatrix).mul(view).invert();

		Vector3f targetColor = inWater
				? new Vector3f(profile.red(), profile.green(), profile.blue())
				: fogColor(profile, context.level(), context.levelState().skyRenderState);
		if (!fogColorReady) {
			smoothedFogColor.set(targetColor);
			fogColorReady = true;
		} else {
			smoothedFogColor.lerp(targetColor, 0.02f);
		}

		frameState = new FogFrameState(
				true,
				profile,
				FogConfigLoader.current().qualitySteps(),
				time,
				renderDistance * 16f,
				invProjView,
				new Vector3f((float) pos.x, (float) pos.y, (float) pos.z),
				new Vector3f(smoothedFogColor),
				inWater
		);
	}

	public static void drawVanillaTarget(LevelRenderContext context) {
		if (IrisCompat.shaderPackInUse()) {
			return;
		}
		drawNow();
	}

	public static void captureIrisDepth(LevelRenderContext context) {
		if (!IrisCompat.shaderPackInUse() || IrisCompat.renderingShadowPass() || !frameState.active()) {
			capturedReady = false;
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		GpuTexture src = minecraft.getMainRenderTarget().getDepthTexture();
		if (src == null || src.isClosed()) {
			capturedReady = false;
			return;
		}

		try {
			ensureCapturedDepth(src);
			RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
					src,
					capturedDepth,
					0,
					0,
					0,
					0,
					0,
					capturedWidth,
					capturedHeight
			);
			capturedReady = true;
		} catch (RuntimeException e) {
			capturedReady = false;
		}
	}

	public static void drawAfterIris() {
		if (!IrisCompat.shaderPackInUse() || IrisCompat.renderingShadowPass()) {
			return;
		}
		drawNow();
	}

	private static void drawNow() {
		if (!frameState.active() || pipelineFailed || IrisCompat.renderingShadowPass()) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}

		try {
			drawPass(minecraft);
		} catch (RuntimeException e) {
			pipelineFailed = true;
			frameState = FogFrameState.inactive();
			CustomFog.LOGGER.error("Volumetric fog pipeline failed; leaving vanilla fog alone", e);
		}
	}

	private static void drawPass(Minecraft minecraft) {
		var target = minecraft.getMainRenderTarget();
		if (target.getColorTextureView() == null) {
			return;
		}

		ensureBuffer();
		uploadVolume();

		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		GpuSampler nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
		GpuTextureView depthView = capturedReady ? capturedDepthView : target.getDepthTextureView();
		if (depthView == null) {
			return;
		}

		try (RenderPass pass = encoder.createRenderPass(
				() -> "customfog volumetric fog",
				target.getColorTextureView(),
				OptionalInt.empty()
		)) {
			pass.setPipeline(FogPipelines.VOLUMETRIC_FOG);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("FogVolume", volumeBuffer.currentBuffer().slice(0, FOG_VOLUME_SIZE));
			pass.bindTexture("DepthSampler", depthView, nearest);
			pass.draw(0, 3);
		}

		volumeBuffer.rotate();
	}

	private static void ensureCapturedDepth(GpuTexture src) {
		int width = src.getWidth(0);
		int height = src.getHeight(0);
		if (capturedDepth != null && capturedWidth == width && capturedHeight == height && !capturedDepth.isClosed()) {
			return;
		}

		closeCapturedDepth();
		capturedDepth = RenderSystem.getDevice().createTexture(
				"customfog captured depth",
				GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
				src.getFormat(),
				width,
				height,
				1,
				1
		);
		capturedDepthView = RenderSystem.getDevice().createTextureView(capturedDepth);
		capturedWidth = width;
		capturedHeight = height;
	}

	private static void ensureBuffer() {
		if (volumeBuffer == null || volumeBuffer.size() < FOG_VOLUME_SIZE) {
			closeBuffer();
			volumeBuffer = new MappableRingBuffer(
					() -> "customfog fog volume",
					GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
					FOG_VOLUME_SIZE
			);
		}
	}

	private static void uploadVolume() {
		FogProfile profile = frameState.profile();
		Vector3f pos = frameState.cameraPos();
		Vector3f fog = frameState.fogColor();
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		GpuBufferSlice slice = volumeBuffer.currentBuffer().slice(0, FOG_VOLUME_SIZE);

		try (GpuBuffer.MappedView mapped = encoder.mapBuffer(slice, false, true)) {
			ByteBuffer data = mapped.data();
			data.clear();
			Std140Builder.intoBuffer(data)
					.putMat4f(frameState.invProjView())
					.putVec4(fog.x, fog.y, fog.z, 1f)
					.putVec4(pos.x, pos.y, pos.z, frameState.time())
					.putVec4(
							profile.density(),
							frameState.inWater() ? 1f : 0f,
							profile.fogBaseY(),
							profile.noiseScale()
					)
					.putVec4(profile.noiseStrength(), profile.distanceK(), frameState.farPlane(), frameState.steps());
		}
	}

	private static Vector3f fogColor(FogProfile profile, ClientLevel level, SkyRenderState sky) {
		float r = profile.red();
		float g = profile.green();
		float b = profile.blue();
		if (level.dimension() != Level.OVERWORLD) {
			return new Vector3f(r, g, b);
		}

		float rawDark = Mth.clamp(level.getSkyDarken() / 15f, 0f, 1f);
		float dark = level.isDarkOutside()
				? Math.max(rawDark, 1f - rawDark)
				: Math.min(rawDark, 1f - rawDark);
		int packed = sky.skyColor;
		float skyR = ((packed >> 16) & 255) / 255f;
		float skyG = ((packed >> 8) & 255) / 255f;
		float skyB = (packed & 255) / 255f;
		float rain = Mth.clamp(sky.rainBrightness, 0f, 1f);
		dark = Mth.clamp(dark * 0.62f + rain * 0.18f, 0f, 0.72f);

		float nightR = r * 0.38f + skyR * 0.18f + 0.10f;
		float nightG = g * 0.36f + skyG * 0.18f + 0.11f;
		float nightB = b * 0.40f + skyB * 0.22f + 0.16f;
		return new Vector3f(
				Mth.lerp(dark, r, nightR),
				Mth.lerp(dark, g, nightG),
				Mth.lerp(dark, b, nightB)
		);
	}

	public static void close() {
		closeBuffer();
		closeCapturedDepth();
	}

	private static void closeBuffer() {
		if (volumeBuffer != null) {
			volumeBuffer.close();
			volumeBuffer = null;
		}
	}

	private static void closeCapturedDepth() {
		if (capturedDepthView != null) {
			capturedDepthView.close();
			capturedDepthView = null;
		}
		if (capturedDepth != null) {
			capturedDepth.close();
			capturedDepth = null;
		}
		capturedReady = false;
	}
}
