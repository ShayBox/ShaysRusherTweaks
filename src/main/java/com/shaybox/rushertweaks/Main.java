package com.shaybox.rushertweaks;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;
import org.rusherhack.core.event.IEventBus;
import org.rusherhack.core.event.listener.EventListener;

@SuppressWarnings("unused")
public class Main extends Plugin {

	private final IEventBus eventBus = RusherHackAPI.getEventBus();
	private final EventListener nightVision = new NightVision();
	private final EventListener pauseOnUse = new PauseOnUse();

	@Override
	public void onLoad() {
		eventBus.subscribe(nightVision);
		eventBus.subscribe(pauseOnUse);
	}
	
	@Override
	public void onUnload() {
		eventBus.unsubscribe(nightVision);
		eventBus.unsubscribe(pauseOnUse);
	}
	
}