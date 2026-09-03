package dev.customfog.client.config;

public record FogProfile(
		float red,
		float green,
		float blue,
		float density,
		float heightFalloff,
		float fogBaseY,
		float noiseScale,
		float noiseStrength,
		float distanceK
) {
	public static FogProfile overworld() {
		return new FogProfile(0.62f, 0.70f, 0.78f, 0.3f, 0f, 63f, 0.018f, 0.35f, 0.024f);
	}

	public static FogProfile nether() {
		return new FogProfile(0.72f, 0.32f, 0.18f, 0.36f, 0f, 32f, 0.024f, 0.4f, 0.03f);
	}

	public static FogProfile end() {
		return new FogProfile(0.55f, 0.38f, 0.72f, 0.24f, 0f, 40f, 0.016f, 0.32f, 0.02f);
	}

	public static FogProfile water() {
		return new FogProfile(0.04f, 0.16f, 0.24f, 0.42f, 0f, 63f, 0.02f, 0.4f, 0.03f);
	}

	public FogProfile sanitized() {
		return new FogProfile(
				clamp(red, 0f, 1f),
				clamp(green, 0f, 1f),
				clamp(blue, 0f, 1f),
				clamp(density, 0.001f, 0.45f),
				clamp(heightFalloff, 0f, 2f),
				fogBaseY,
				clamp(noiseScale, 0.001f, 1f),
				clamp(noiseStrength, 0f, 1f),
				clamp(distanceK, 0.0001f, 0.2f)
		);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
