package com.roucky44.letsgamble.client;

import com.roucky44.letsgamble.KeyInputHandler;
import com.roucky44.letsgamble.GambleClientNetworking;
import net.fabricmc.api.ClientModInitializer;

public class LetSGambleClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KeyInputHandler.register();
		GambleClientNetworking.register();
	}
}
