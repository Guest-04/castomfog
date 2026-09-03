package dev.customfog.client.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import dev.customfog.CustomFog;

public final class FogConfigLoader {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static FogConfig current = FogConfig.defaults();

	private FogConfigLoader() {
	}

	public static FogConfig current() {
		return current;
	}

	public static void load() {
		Path path = configPath();
		if (!Files.exists(path)) {
			current = FogConfig.defaults();
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(path)) {
			FogConfig parsed = GSON.fromJson(reader, FogConfig.class);
			current = parsed == null ? FogConfig.defaults() : rebalance(parsed.sanitized());
			save();
		} catch (IOException | RuntimeException e) {
			CustomFog.LOGGER.warn("Failed to read config/customfog.json, using defaults", e);
			current = FogConfig.defaults();
		}
	}

	public static void save() {
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(current.sanitized(), writer);
			}
		} catch (IOException e) {
			CustomFog.LOGGER.warn("Failed to write config/customfog.json", e);
		}
	}

	private static FogConfig rebalance(FogConfig config) {
		FogConfig defaults = FogConfig.defaults();
		FogProfile water = config.water() == null ? defaults.water() : rebalanceProfile(config.water(), defaults.water());
		return new FogConfig(
				config.enabled(),
				defaults.qualitySteps(),
				rebalanceProfile(config.overworld(), defaults.overworld()),
				rebalanceProfile(config.nether(), defaults.nether()),
				rebalanceProfile(config.end(), defaults.end()),
				water
		);
	}

	private static FogProfile rebalanceProfile(FogProfile current, FogProfile defaults) {
		return new FogProfile(
				current.red(),
				current.green(),
				current.blue(),
				defaults.density(),
				0f,
				current.fogBaseY(),
				defaults.noiseScale(),
				defaults.noiseStrength(),
				defaults.distanceK()
		);
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("customfog.json");
	}
}
