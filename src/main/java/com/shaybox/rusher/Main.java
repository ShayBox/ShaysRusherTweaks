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
    private final EventListener durability101 = new Durability101();
    private final EventListener nightVision = new NightVision();
    private final EventListener pauseOnUse = new PauseOnUse();
    private final EventListener repairPriority = new RepairPriority();

    @Override
    public void onLoad() {
        eventBus.subscribe(durability101);
        eventBus.subscribe(nightVision);
        eventBus.subscribe(pauseOnUse);
        eventBus.subscribe(repairPriority);
    }

    @Override
    public void onUnload() {
        eventBus.unsubscribe(durability101);
        eventBus.unsubscribe(nightVision);
        eventBus.unsubscribe(pauseOnUse);
        eventBus.unsubscribe(repairPriority);
    }

}