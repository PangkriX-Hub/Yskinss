package com.example.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class YskinsScreen extends Screen {
	private static final int W = 300;
	private static final int ROW_H = 38;
	private static final int HEADER = 34;
	private static final int FOOTER = 60;
	private static final int PAGER = 24;

	private final String originalCape;
	private final String originalSkin;
	private String pendingCape;
	private String pendingSkin;
	private boolean capesTab = true;
	private boolean saved = false;
	private int page = 0;
	private int pages = 1;
	private int left, top, panelH, listTop, rows;
	private List<String> items = List.of();
	private Button prevButton, nextButton;

	public YskinsScreen() {
		super(Component.literal("Yskins"));
		originalCape = ExampleModClient.selectedCape;
		originalSkin = ExampleModClient.selectedSkin;
		pendingCape = originalCape;
		pendingSkin = originalSkin;
	}

	@Override
	protected void init() {
		panelH = Math.min(240, this.height - 12);
		left = (this.width - W) / 2;
		top = (this.height - panelH) / 2;
		listTop = top + HEADER;
		rows = Math.max(1, (panelH - HEADER - FOOTER - PAGER) / ROW_H);

		items = ExampleModClient.names(capesTab ? ExampleModClient.capesDir : ExampleModClient.skinsDir);
		pages = Math.max(1, (items.size() + rows - 1) / rows);
		page = Math.min(page, pages - 1);

		// Tabs: banner = capes, player head = skins
		addRenderableWidget(Button.builder(Component.empty(), b -> switchTab(true))
			.bounds(left + W - 58, top + 5, 24, 24).build());
		addRenderableWidget(Button.builder(Component.empty(), b -> switchTab(false))
			.bounds(left + W - 30, top + 5, 24, 24).build());

		// Rows
		for (int i = 0; i < rows; i++) {
			int idx = page * rows + i;
			if (idx >= items.size()) break;
			String name = items.get(idx);
			boolean sel = name.equals(pending());
			addRenderableWidget(Button.builder(Component.literal(sel ? "Selected" : "Select"), b -> {
				if (capesTab) pendingCape = name; else pendingSkin = name;
				rebuildWidgets();
			}).bounds(left + W - 76, listTop + i * ROW_H + 9, 64, 20).build());
		}

		// Pager
		int pagerY = listTop + rows * ROW_H + 2;
		prevButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
			page--;
			rebuildWidgets();
		}).bounds(left + W / 2 - 56, pagerY, 24, 18).build());
		nextButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
			page++;
			rebuildWidgets();
		}).bounds(left + W / 2 + 32, pagerY, 24, 18).build());
		prevButton.active = page > 0;
		nextButton.active = page < pages - 1;

		// Footer
		int y1 = top + panelH - 54;
		int y2 = top + panelH - 28;
		addRenderableWidget(Button.builder(Component.literal("Set Cape"), b -> {
			if (pendingCape != null) ExampleModClient.selectedCape = pendingCape;
		}).bounds(left + 10, y1, 135, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Set Skin"), b -> {
			if (pendingSkin != null) ExampleModClient.selectedSkin = pendingSkin;
		}).bounds(left + 155, y1, 135, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Clear Skin"), b -> {
			pendingSkin = null;
			ExampleModClient.selectedSkin = null;
			rebuildWidgets();
		}).bounds(left + 10, y2, 88, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
			ExampleModClient.saveConfig();
			saved = true;
			onClose();
		}).bounds(left + 106, y2, 88, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
			.bounds(left + 202, y2, 88, 20).build());
	}

	private void switchTab(boolean capes) {
		capesTab = capes;
		page = 0;
		rebuildWidgets();
	}

	private String pending() {
		return capesTab ? pendingCape : pendingSkin;
	}

	private String applied() {
		return capesTab ? ExampleModClient.selectedCape : ExampleModClient.selectedSkin;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		if (!saved) {
			ExampleModClient.selectedCape = originalCape;
			ExampleModClient.selectedSkin = originalSkin;
		}
		super.onClose();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		super.renderBackground(g, mx, my, delta);
		// Vanilla-style gray panel with dark border
		g.fill(left - 2, top - 2, left + W + 2, top + panelH + 2, 0xFF000000);
		g.fill(left, top, left + W, top + panelH, 0xFFC6C6C6);
		g.fill(left, top, left + W, top + 2, 0xFFFFFFFF);
		g.fill(left, top, left + 2, top + panelH, 0xFFFFFFFF);
		g.fill(left, top + panelH - 2, left + W, top + panelH, 0xFF555555);
		g.fill(left + W - 2, top, left + W, top + panelH, 0xFF555555);
		// Dark list area
		g.fill(left + 6, listTop - 2, left + W - 6, listTop + rows * ROW_H + 2, 0xFF1E1E1E);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);

		g.drawString(this.font, "Change skin & cape", left + 10, top + 12, 0xFF404040, false);

		// Tab icons and selected-tab outline
		g.renderItem(new ItemStack(Items.WHITE_BANNER), left + W - 54, top + 9);
		g.renderItem(new ItemStack(Items.PLAYER_HEAD), left + W - 26, top + 9);
		outline(g, capesTab ? left + W - 58 : left + W - 30, top + 5, 24, 24, 0xFFFFFFFF);

		if (items.isEmpty()) {
			g.drawString(this.font, "No .png files in Yskins/" + (capesTab ? "capes" : "skins"),
				left + 14, listTop + 10, 0xFFFFFFFF, false);
		}

		for (int i = 0; i < rows; i++) {
			int idx = page * rows + i;
			if (idx >= items.size()) break;
			String name = items.get(idx);
			int y = listTop + i * ROW_H;

			if (name.equals(pending())) outline(g, left + 8, y, W - 16, ROW_H - 2, 0xFFFFFFFF);

			if (capesTab) drawCape(g, name, left + 14, y + 3);
			else drawSkin(g, name, left + 16, y + 3);

			g.drawString(this.font, trim(name, 170), left + 44, y + 10, 0xFFFFFFFF, false);
			if (name.equals(applied())) {
				g.drawString(this.font, "Active", left + 44, y + 22, 0xFF55FF55, false);
			}
		}

		String pageText = (page + 1) + "/" + pages;
		g.drawString(this.font, pageText,
			left + W / 2 - this.font.width(pageText) / 2, listTop + rows * ROW_H + 7, 0xFF404040, false);
	}

	private void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y, x + 1, y + h, color);
		g.fill(x + w - 1, y, x + w, y + h, color);
	}

	private String trim(String s, int maxW) {
		if (this.font.width(s) <= maxW) return s;
		while (s.length() > 1 && this.font.width(s + "...") > maxW) {
			s = s.substring(0, s.length() - 1);
		}
		return s + "...";
	}

	private void part(GuiGraphics g, Identifier id, int x, int y, int dw, int dh,
	                  int u, int v, int rw, int rh, int[] sz) {
		float s = sz[0] / 64f;
		g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, u * s, v * s, dw, dh,
			Math.round(rw * s), Math.round(rh * s), sz[0], sz[1]);
	}

	private void drawSkin(GuiGraphics g, String file, int x, int y) {
		Identifier id = YskinsTextures.load(ExampleModClient.skinsDir, "skins", file);
		int[] sz = YskinsTextures.size("skins", file);
		if (id == null || sz == null) return;
		boolean modern = sz[1] >= sz[0];
		part(g, id, x + 4, y, 8, 8, 8, 8, 8, 8, sz);
		part(g, id, x + 4, y, 8, 8, 40, 8, 8, 8, sz);
		part(g, id, x + 4, y + 8, 8, 12, 20, 20, 8, 12, sz);
		part(g, id, x, y + 8, 4, 12, 44, 20, 4, 12, sz);
		part(g, id, x + 12, y + 8, 4, 12, modern ? 36 : 44, modern ? 52 : 20, 4, 12, sz);
		part(g, id, x + 4, y + 20, 4, 12, 4, 20, 4, 12, sz);
		part(g, id, x + 8, y + 20, 4, 12, modern ? 20 : 4, modern ? 52 : 20, 4, 12, sz);
	}

	private void drawCape(GuiGraphics g, String file, int x, int y) {
		Identifier id = YskinsTextures.load(ExampleModClient.capesDir, "capes", file);
		int[] sz = YskinsTextures.size("capes", file);
		if (id == null || sz == null) return;
		part(g, id, x, y, 20, 32, 1, 1, 10, 16, sz);
	}
	}
