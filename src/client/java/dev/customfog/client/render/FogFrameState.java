package dev.customfog.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import dev.customfog.client.config.FogProfile;

public record FogFrameState(
		boolean active,
		FogProfile profile,
		int steps,
		float time,
		float farPlane,
		Matrix4f invProjView,
		Vector3f cameraPos,
		Vector3f fogColor,
		boolean inWater
) {
	public static FogFrameState inactive() {
		return new FogFrameState(false, FogProfile.overworld(), 16, 0f, 256f, new Matrix4f(), new Vector3f(), new Vector3f(0.62f, 0.70f, 0.78f), false);
	}
}
