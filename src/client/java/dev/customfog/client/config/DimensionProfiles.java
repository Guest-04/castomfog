package dev.customfog.client.config;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class DimensionProfiles {
	private DimensionProfiles() {
	}

	public static FogProfile profileFor(ResourceKey<Level> dimension) {
		FogConfig config = FogConfigLoader.current();
		if (dimension == Level.NETHER) {
			return config.nether();
		}
		if (dimension == Level.END) {
			return config.end();
		}
		return config.overworld();
	}
}
