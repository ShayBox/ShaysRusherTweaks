package com.shaybox.rusher;

import com.shaybox.rusher.tweaks.*;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;
import org.rusherhack.core.event.IEventBus;
import org.rusherhack.core.event.listener.EventListener;

@SuppressWarnings("unused")
public class Main extends Plugin {

    /* EventBus & Listeners */
    private final IEventBus eventBus = RusherHackAPI.getEventBus();
    private final EventListener armorPriority = new ArmorPriority();
    private final EventListener durability101 = new Durability101();
    private final EventListener nightVision = new NightVision();
    private final EventListener pauseOnUse = new PauseOnUse();

    @Override
    public void onLoad() {
        eventBus.subscribe(armorPriority);
        eventBus.subscribe(durability101);
        eventBus.subscribe(nightVision);
        eventBus.subscribe(pauseOnUse);
    }

    @Override
    public void onUnload() {
        eventBus.unsubscribe(armorPriority);
        eventBus.unsubscribe(durability101);
        eventBus.unsubscribe(nightVision);
        eventBus.unsubscribe(pauseOnUse);
    }

}