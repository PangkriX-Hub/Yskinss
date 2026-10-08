package com.yclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yclient.YClientMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("yclient.json");

	private static YConfig config = new YConfig();

	private ConfigManager() {
	}

	public static YConfig get() {
		return config;
	}

	public static void load() {
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				YConfig loaded = GSON.fromJson(reader, YConfig.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (Exception e) {
				YClientMod.LOGGER.warn("Could not read yclient.json, using defaults", e);
			}
		}
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Path tmp = FILE.resolveSibling("yclient.json.tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
			Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			YClientMod.LOGGER.warn("Could not save yclient.json", e);
		}
	}

	public static void reset() {
		config = new YConfig();
		save();
	}
                                                   }
