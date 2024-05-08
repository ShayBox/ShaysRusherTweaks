package com.shaybox.rushertweaks;

import net.minecraft.client.player.LocalPlayer;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.player.EventPlayerUpdate;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.feature.IFeatureManager;
import org.rusherhack.core.setting.BooleanSetting;

@SuppressWarnings("unused")
public class PauseOnUse implements EventListener {

    /* RusherHackAPI & Modules */
    private final IFeatureManager<IModule> moduleManager = RusherHackAPI.getModuleManager();
    private final ToggleableModule autoEat = (ToggleableModule) moduleManager.getFeature("AutoEat").orElseThrow();
    private final ToggleableModule autoWalk = (ToggleableModule) moduleManager.getFeature("AutoWalk").orElseThrow();
    private final ToggleableModule elytraFly = (ToggleableModule) moduleManager.getFeature("ElytraFly").orElseThrow();
    private final ToggleableModule rotationLock = (ToggleableModule) moduleManager.getFeature("RotationLock").orElseThrow();

    /* Custom Settings */
//    private final NullSetting pauseOnEat = new NullSetting("PauseOnEat", "Pause While Eating");
    private final BooleanSetting pauseAutoWalk = new BooleanSetting("PauseOnUse", "Pause AutoWalk While Eating", false);
    private final BooleanSetting pauseElytraFly = new BooleanSetting("PauseOnUse", "Pause ElytraFly While Eating", false);
    private final BooleanSetting pauseRotationLock = new BooleanSetting("PauseOnUse", "Pause RotationLock While Eating", false);

    public PauseOnUse() {
//        this.pauseOnEat.addSubSettings(this.pauseAutoWalk, this.pauseElytraFly, this.pauseRotationLock);
//        this.autoEat.registerSettings(this.pauseOnEat);
        this.autoWalk.registerSettings(this.pauseAutoWalk);
        this.elytraFly.registerSettings(this.pauseElytraFly);
        this.rotationLock.registerSettings(this.pauseRotationLock);
    }

    @Override
    public boolean isListening() {
//        return this.autoEat.isToggled();
        return this.pauseAutoWalk.getValue() || this.pauseElytraFly.getValue() || this.pauseRotationLock.getValue();
    }

    /* Previous State */
    private boolean isPaused = false;
    private boolean lastAutoWalk = this.autoWalk.isToggled();
    private boolean lastElytraFly = this.elytraFly.isToggled();
    private boolean lastRotationLock = this.rotationLock.isToggled();

    @Subscribe
    private void onUpdate(EventPlayerUpdate event) {
        LocalPlayer player = event.getPlayer();
        boolean usingItem = player.isUsingItem();

        if (usingItem && !isPaused) {
            isPaused = true;

            if (this.pauseAutoWalk.getValue()) {
                lastAutoWalk = this.autoWalk.isToggled();
                this.autoWalk.setToggled(false);
            }

            if (this.pauseElytraFly.getValue()) {
                lastElytraFly = this.elytraFly.isToggled();
                this.elytraFly.setToggled(false);
            }

            if (this.pauseRotationLock.getValue()) {
                lastRotationLock = this.rotationLock.isToggled();
                this.rotationLock.setToggled(false);
            }
        } else if (!usingItem && isPaused) {
            isPaused = false;

            if (this.pauseAutoWalk.getValue()) {
                this.autoWalk.setToggled(this.lastAutoWalk);
            }

            if (this.pauseElytraFly.getValue()) {
                this.elytraFly.setToggled(this.lastElytraFly);
            }

            if (this.pauseRotationLock.getValue()) {
                this.rotationLock.setToggled(this.lastRotationLock);
            }
        } else if (!usingItem) {
            lastAutoWalk = this.autoWalk.isToggled();
            lastElytraFly = this.elytraFly.isToggled();
            lastRotationLock = this.rotationLock.isToggled();
        }
    }

}

