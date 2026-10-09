package com.yclient.gui;

import net.minecraft.client.gui.GuiGraphics;

public interface Tab {
	String name();

	/** Called when the screen is built. Add buttons here via screen.addButton(). */
	default void init(YScreen screen, int x, int y, int w, int h) {
	}

	void render(GuiGraphics g, YScreen screen, int x, int y, int w, int h, int mouseX, int mouseY);
}
