package com.yclient.config;

import java.util.HashMap;
import java.util.Map;

public class YConfig {
	// GUI
	public int guiScale = 100;        // percent
	public int guiOpacity = 90;       // percent
	public int accentColor = 0xFF4DD0E1; // ARGB, soft cyan

	// Feature toggles by id, e.g. "hud.fps". Missing id means enabled.
	public Map<String, Boolean> features = new HashMap<>();

	public boolean isEnabled(String featureId) {
		return features.getOrDefault(featureId, true);
	}

	public void setEnabled(String featureId, boolean enabled) {
		features.put(featureId, enabled);
	}
}
