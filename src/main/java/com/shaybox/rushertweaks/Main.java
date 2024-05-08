package com.shaybox.rushertweaks;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;
import org.rusherhack.core.event.IEventBus;
import org.rusherhack.core.event.listener.EventListener;

@SuppressWarnings("unused")
public class Main extends Plugin {

	private final IEventBus eventBus = RusherHackAPI.getEventBus();
	private final EventListener eventListener = new IEventListener();

	@Override
	public void onLoad() {
		eventBus.subscribe(eventListener);
	}
	
	@Override
	public void onUnload() {
		eventBus.unsubscribe(eventListener);
	}
	
}