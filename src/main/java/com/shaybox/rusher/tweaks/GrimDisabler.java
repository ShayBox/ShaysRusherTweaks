package com.shaybox.rusher.tweaks;

import com.shaybox.rusher.Main;
import net.minecraft.client.Minecraft;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.setting.BindSetting;
import org.rusherhack.core.bind.key.IKey;
import org.rusherhack.core.bind.key.NullKey;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.feature.IFeatureManager;
import org.rusherhack.core.setting.BooleanSetting;

public class GrimDisabler implements EventListener {

    /* Minecraft */
    private final Minecraft minecraft = Minecraft.getInstance();

    /* RusherHackAPI Managers & Modules */
    private final IFeatureManager<IModule> moduleManager = RusherHackAPI.getModuleManager();
    private final ToggleableModule autoArmor = (ToggleableModule) moduleManager.getFeature("AutoArmor").orElseThrow();
    private final ToggleableModule elytraTweaks = (ToggleableModule) moduleManager.getFeature("ElytraTweaks").orElseThrow();

    /* Custom Settings */
    private final BooleanSetting elytraPriority = (BooleanSetting) autoArmor.getSetting("ElytraPriority");
    private final BooleanSetting grimDisabler = (BooleanSetting) this.elytraTweaks.getSetting("GrimDisabler");
    private final BooleanSetting autoChestplate = new BooleanSetting("AutoChestplate", "Automatically disable AutoArmor ElytraPriority", true);
    private final BindSetting grimDisablerBind = new BindSetting("Bind", NullKey.INSTANCE);

    /* Previous State */
    private boolean isRunning = false;
    private boolean isGrimDisablerDown = false;
    private boolean lastElytraPriority = this.elytraPriority.getValue();

    /* Initialize */
    public GrimDisabler() {
        this.grimDisabler.addSubSettings(this.autoChestplate, this.grimDisablerBind);
    }

    @Override
    public boolean isListening() {
        IKey grimDisablerBind = this.grimDisablerBind.getValue();

        return this.grimDisabler.getValue() | this.isGrimDisablerDown || grimDisablerBind.isKeyDown() || this.isRunning;
    }

    @SuppressWarnings("unused")
    @Subscribe
    private void onUpdate(EventUpdate event) {
        if (this.autoChestplate.getValue()) {
            if (this.grimDisabler.getValue()) {
                if (!this.isRunning) {
                    this.lastElytraPriority = this.elytraPriority.getValue();
                    this.elytraPriority.setValue(false);
                    this.isRunning = true;
                }
            } else if (this.isRunning) {
                this.elytraPriority.setValue(lastElytraPriority);
                this.isRunning = false;
            }
        }

        if (minecraft.screen != null) return;

        Main.handleKey(this.grimDisablerBind::getValue, this.isGrimDisablerDown, (value) -> this.isGrimDisablerDown = value, this.grimDisabler::getValue, this.grimDisabler::setValue);
    }

}

