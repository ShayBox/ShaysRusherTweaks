package com.shaybox.rusher.tweaks;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.player.EventPlayerUpdate;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.feature.IFeatureManager;
import org.rusherhack.core.setting.BooleanSetting;

public class NightVision implements EventListener {

    /* RusherHackAPI Managers & Modules */
    private final IFeatureManager<IModule> moduleManager = RusherHackAPI.getModuleManager();
    private final ToggleableModule fullBright = (ToggleableModule) moduleManager.getFeature("FullBright").orElseThrow();

    /* Custom Settings */
    private final BooleanSetting nightVisionSetting = new BooleanSetting("NightVision", "Client Side Night Vision Effect", false);

    /* Custom Effects */
    private final MobEffectInstance nightVisionEffect = new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, true, false, false);

    /* Previous State */
    private boolean isApplied = false;

    /* Initialize */
    public NightVision() {
        this.fullBright.registerSettings(this.nightVisionSetting);
    }

    @Override
    public boolean isListening() {
        return this.nightVisionSetting.getValue() || this.isApplied;
    }

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate event) {
        final LocalPlayer player = event.getPlayer();
        final boolean isFullBright = this.fullBright.isToggled();
        final boolean isNightVision = this.nightVisionSetting.getValue();
        final boolean hasNightVision = player.hasEffect(MobEffects.NIGHT_VISION);

        if (!hasNightVision && (isFullBright && isNightVision)) {
            isApplied = true;
            player.addEffect(this.nightVisionEffect);
        } else if (isApplied && (!isFullBright || !isNightVision)) {
            isApplied = false;
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

}

