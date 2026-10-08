package com.example.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class YskinsTextures {
	private static final Map<String, Identifier> CACHE = new HashMap<>();
	private static final Map<String, int[]> SIZES = new HashMap<>();
	private static final Set<String> FAILED = new HashSet<>();
	private static final List<Anim> ANIMS = new ArrayList<>();

	private static final class Anim {
		final DynamicTexture texture;
		final NativeImage[] frames;
		final int[] delays;
		final long total;
		final long start = System.currentTimeMillis();
		int current = 0;

		Anim(DynamicTexture texture, NativeImage[] frames, int[] delays) {
			this.texture = texture;
			this.frames = frames;
			this.delays = delays;
			long sum = 0;
			for (int d : delays) sum += d;
			this.total = Math.max(1, sum);
		}

		void update(long now) {
			long t = (now - start) % total;
			int idx = 0;
			long acc = 0;
			for (int i = 0; i < delays.length; i++) {
				acc += delays[i];
				if (t < acc) {
					idx = i;
					break;
				}
			}
			if (idx == current) return;
			NativeImage px = texture.getPixels();
			if (px == null) return;
			px.copyFrom(frames[idx]);
			texture.upload();
			current = idx;
		}
	}

	public static int[] size(String kind, String fileName) {
		return SIZES.get(kind + "/" + fileName);
	}

	/** Call every client tick to advance animated textures. */
	public static void tickAnimations() {
		if (ANIMS.isEmpty()) return;
		long now = System.currentTimeMillis();
		for (Anim a : ANIMS) {
			a.update(now);
		}
	}

	public static Identifier load(Path dir, String kind, String fileName) {
		String key = kind + "/" + fileName;
		Identifier cached = CACHE.get(key);
		if (cached != null) return cached;
		if (FAILED.contains(key)) return null;

		try {
			byte[] bytes = Files.readAllBytes(dir.resolve(fileName));

			ApngLoader.Result apng = null;
			try {
				apng = ApngLoader.load(bytes);
			} catch (Exception e) {
				e.printStackTrace();
			}

			DynamicTexture tex;
			if (apng != null) {
				NativeImage first = new NativeImage(NativeImage.Format.RGBA, apng.width, apng.height, false);
				first.copyFrom(apng.frames[0]);
				tex = new DynamicTexture(() -> "yskins " + key, first);
				SIZES.put(key, new int[]{apng.width, apng.height});
				ANIMS.add(new Anim(tex, apng.frames, apng.delays));
			} else {
				NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
				SIZES.put(key, new int[]{image.getWidth(), image.getHeight()});
				tex = new DynamicTexture(() -> "yskins " + key, image);
			}

			String safe = fileName.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
			Identifier id = Identifier.fromNamespaceAndPath(
				"yskins", kind + "/" + safe + "_" + Integer.toHexString(fileName.hashCode()));
			Minecraft.getInstance().getTextureManager().register(id, tex);
			CACHE.put(key, id);
			return id;
		} catch (IOException e) {
			e.printStackTrace();
			FAILED.add(key);
			return null;
		}
	}
}
