package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

public class ExampleModClient implements ClientModInitializer {
	public static Path skinsDir;
	public static Path capesDir;
	public static String selectedSkin;
	public static String selectedCape;
	private static Path configFile;
	private static KeyMapping openGuiKey;

	@Override
	public void onInitializeClient() {
		Path root = FabricLoader.getInstance().getGameDir().resolve("Yskins");
		skinsDir = root.resolve("skins");
		capesDir = root.resolve("capes");
		configFile = root.resolve("config.properties");
		try {
			Files.createDirectories(skinsDir);
			Files.createDirectories(capesDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		loadConfig();

		// Keybind (default N, changeable in Controls)
		KeyMapping.Category category = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath("yskins", "main"));
		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.yskins.open_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, category));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openGuiKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new YskinsScreen());
				}
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommandManager.literal("yskins")
				.then(ClientCommandManager.literal("list").executes(ctx -> {
					ctx.getSource().sendFeedback(Component.literal("Skins: " + names(skinsDir)));
					ctx.getSource().sendFeedback(Component.literal("Capes: " + names(capesDir)));
					return 1;
				}))
				.then(ClientCommandManager.literal("skin")
					.then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
						.suggests((ctx, builder) ->
							SharedSuggestionProvider.suggest(names(skinsDir), builder))
						.executes(ctx -> {
							String n = StringArgumentType.getString(ctx, "name");
							if (!names(skinsDir).contains(n)) {
								ctx.getSource().sendFeedback(Component.literal("Skin not found: " + n));
								return 0;
							}
							selectedSkin = n;
							saveConfig();
							ctx.getSource().sendFeedback(Component.literal("Skin selected: " + n));
							return 1;
						})))
				.then(ClientCommandManager.literal("cape")
					.then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
						.suggests((ctx, builder) ->
							SharedSuggestionProvider.suggest(names(capesDir), builder))
						.executes(ctx -> {
							String n = StringArgumentType.getString(ctx, "name");
							if (!names(capesDir).contains(n)) {
								ctx.getSource().sendFeedback(Component.literal("Cape not found: " + n));
								return 0;
							}
							selectedCape = n;
							saveConfig();
							ctx.getSource().sendFeedback(Component.literal("Cape selected: " + n));
							return 1;
						})))
				.then(ClientCommandManager.literal("reset").executes(ctx -> {
					selectedSkin = null;
					selectedCape = null;
					saveConfig();
					ctx.getSource().sendFeedback(Component.literal("Skin and cape reset"));
					return 1;
				}))));
	}

	public static List<String> names(Path dir) {
		try (Stream<Path> s = Files.list(dir)) {
			return s.map(p -> p.getFileName().toString())
				.filter(n -> n.toLowerCase().endsWith(".png"))
				.sorted()
				.toList();
		} catch (IOException e) {
			return List.of();
		}
	}

	private static void loadConfig() {
		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(configFile)) {
			p.load(in);
		} catch (IOException e) {
			return;
		}
		String skin = p.getProperty("skin");
		String cape = p.getProperty("cape");
		if (skin != null && names(skinsDir).contains(skin)) selectedSkin = skin;
		if (cape != null && names(capesDir).contains(cape)) selectedCape = cape;
	}

	public static void saveConfig() {
		Properties p = new Properties();
		if (selectedSkin != null) p.setProperty("skin", selectedSkin);
		if (selectedCape != null) p.setProperty("cape", selectedCape);
		try (OutputStream out = Files.newOutputStream(configFile)) {
			p.store(out, "Yskins");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
