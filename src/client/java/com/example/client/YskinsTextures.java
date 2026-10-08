package com.example.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class YskinsTextures {
	private static final Map<String, Identifier> CACHE = new HashMap<>();

	public static Identifier load(Path dir, String kind, String fileName) {
		String key = kind + "/" + fileName;
		Identifier cached = CACHE.get(key);
		if (cached != null) return cached;

		Path file = dir.resolve(fileName);
		try (InputStream in = Files.newInputStream(file)) {
			NativeImage image = NativeImage.read(in);
			String safe = fileName.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
			Identifier id = Identifier.fromNamespaceAndPath(
				"yskins", kind + "/" + safe + "_" + Integer.toHexString(fileName.hashCode()));
			Minecraft.getInstance().getTextureManager()
				.register(id, new DynamicTexture(() -> "yskins " + key, image));
			CACHE.put(key, id);
			return id;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
