package com.example.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class ExampleModClient implements ClientModInitializer {
	public static Path skinsDir;
	public static Path capesDir;
	public static String selectedSkin;
	public static String selectedCape;

	@Override
	public void onInitializeClient() {
		Path root = FabricLoader.getInstance().getGameDir().resolve("Yskins");
		skinsDir = root.resolve("skins");
		capesDir = root.resolve("capes");
		try {
			Files.createDirectories(skinsDir);
			Files.createDirectories(capesDir);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommandManager.literal("yskins")
				.then(ClientCommandManager.literal("list").executes(ctx -> {
					ctx.getSource().sendFeedback(Component.literal("Skins: " + names(skinsDir)));
					ctx.getSource().sendFeedback(Component.literal("Capes: " + names(capesDir)));
					return 1;
				}))
				.then(ClientCommandManager.literal("skin")
					.then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
						.executes(ctx -> {
							String n = StringArgumentType.getString(ctx, "name");
							if (!names(skinsDir).contains(n)) {
								ctx.getSource().sendFeedback(Component.literal("Skin tidak ditemukan: " + n));
								return 0;
							}
							selectedSkin = n;
							ctx.getSource().sendFeedback(Component.literal("Skin dipilih: " + n));
							return 1;
						})))
				.then(ClientCommandManager.literal("cape")
					.then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
						.executes(ctx -> {
							String n = StringArgumentType.getString(ctx, "name");
							if (!names(capesDir).contains(n)) {
								ctx.getSource().sendFeedback(Component.literal("Cape tidak ditemukan: " + n));
								return 0;
							}
							selectedCape = n;
							ctx.getSource().sendFeedback(Component.literal("Cape dipilih: " + n));
							return 1;
						})))
				.then(ClientCommandManager.literal("reset").executes(ctx -> {
					selectedSkin = null;
					selectedCape = null;
					ctx.getSource().sendFeedback(Component.literal("Skin dan cape direset"));
					return 1;
				}))));
	}

	private static List<String> names(Path dir) {
		try (Stream<Path> s = Files.list(dir)) {
			return s.map(p -> p.getFileName().toString())
				.filter(n -> n.toLowerCase().endsWith(".png"))
				.sorted()
				.toList();
		} catch (IOException e) {
			return List.of();
		}
	}
}
