package dev.customfog.client.config;

public record FogConfig(
		boolean enabled,
		int qualitySteps,
		FogProfile overworld,
		FogProfile nether,
		FogProfile end,
		FogProfile water
) {
	public static FogConfig defaults() {
		return new FogConfig(true, 8, FogProfile.overworld(), FogProfile.nether(), FogProfile.end(), FogProfile.water());
	}

	public FogConfig sanitized() {
		int steps = Math.max(6, Math.min(12, qualitySteps));
		FogProfile waterProfile = water == null ? FogProfile.water() : water.sanitized();
		return new FogConfig(
				enabled,
				steps,
				overworld.sanitized(),
				nether.sanitized(),
				end.sanitized(),
				waterProfile
		);
	}
}
