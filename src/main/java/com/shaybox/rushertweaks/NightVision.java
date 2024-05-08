package com.shaybox.rushertweaks;

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

@SuppressWarnings("unused")
public class NightVision implements EventListener {

    /* RusherHackAPI & Modules */
    private final IFeatureManager<IModule> moduleManager = RusherHackAPI.getModuleManager();
    private final ToggleableModule fullBright = (ToggleableModule) moduleManager.getFeature("FullBright").orElseThrow();

    /* Custom Settings */
    private final BooleanSetting nightVisionSetting = new BooleanSetting("NightVision", "Client Side Night Vision Effect", false);

    public NightVision() {
        this.fullBright.registerSettings(this.nightVisionSetting);
    }

    @Override
    public boolean isListening() {
        return this.fullBright.isToggled() || this.nightVisionSetting.getValue();
    }

    boolean isApplied = false;

    @Subscribe
    private void onUpdate(EventPlayerUpdate event) {
        LocalPlayer player = event.getPlayer();
        boolean isFullBright = this.fullBright.isToggled();
        boolean isNightVision = this.nightVisionSetting.getValue();

        if (!isApplied && (isFullBright && isNightVision)) {
            isApplied = true;
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION,
                    -1,
                    1,
                    true,
                    false,
                    false
            ));
        } else if (isApplied && (!isFullBright || !isNightVision)) {
            isApplied = false;
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

}

