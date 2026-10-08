package com.yclient;

import com.yclient.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;

public class YClientClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ConfigManager.load();
	}
}
