package com.shaybox.rusher;

import com.shaybox.rusher.modules.*;
import com.shaybox.rusher.tweaks.*;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.plugin.Plugin;
import org.rusherhack.core.bind.key.IKey;
import org.rusherhack.core.event.IEventBus;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.feature.IFeatureManager;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Main extends Plugin {

    /* ModuleManager & Modules */
    private final IFeatureManager<IModule> moduleManager = RusherHackAPI.getModuleManager();
    private final ToggleableModule killEffects = new KillEffects();

    /* EventBus & Listeners */
    private final IEventBus eventBus = RusherHackAPI.getEventBus();
    private final EventListener armorPriority = new ArmorPriority();
    private final EventListener autoRestart = new AutoRestart();
    private final EventListener durability101 = new Durability101();
    private final EventListener nightVision = new NightVision();
    private final EventListener pauseOnUse = new PauseOnUse();

    @Override
    public void onLoad() {
        this.moduleManager.registerFeature(this.killEffects);
        this.eventBus.subscribe(this.armorPriority);
        this.eventBus.subscribe(this.autoRestart);
        this.eventBus.subscribe(this.durability101);
        this.eventBus.subscribe(this.nightVision);
        this.eventBus.subscribe(this.pauseOnUse);
    }

    @Override
    public void onUnload() {
        this.eventBus.unsubscribe(this.armorPriority);
        this.eventBus.unsubscribe(this.autoRestart);
        this.eventBus.unsubscribe(this.durability101);
        this.eventBus.unsubscribe(this.nightVision);
        this.eventBus.unsubscribe(this.pauseOnUse);
    }

    public static void handleKey(Supplier<IKey> key, boolean isDown, Consumer<Boolean> setIsDown, Supplier<Boolean> getPriority, Consumer<Boolean> setPriority) {
        if (key.get().isKeyDown()) {
            if (!isDown) {
                setIsDown.accept(true);
                setPriority.accept(!getPriority.get());
            }
        } else {
            setIsDown.accept(false);
        }
    }

}