package com.shaybox.rusher.tweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.effect.MobEffects;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.player.EventPlayerUpdate;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.feature.IFeatureManager;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.NumberSetting;

public class AutoDeploy implements EventListener {

    /* RusherHackAPI Managers & Modules */
    private final IFeatureManager<IModule> moduleManager = RusherHackAPI.getModuleManager();
    private final ToggleableModule autoArmor = (ToggleableModule) moduleManager.getFeature("AutoArmor").orElseThrow();
    private final ToggleableModule elytraTweaks = (ToggleableModule) moduleManager.getFeature("ElytraTweaks").orElseThrow();

    /* AutoArmor Settings */
    private final BooleanSetting elytraPriority = (BooleanSetting) autoArmor.getSetting("ElytraPriority");

    /* Custom Settings */
    private final BooleanSetting autoDeploy = new BooleanSetting("AutoDeploy", "Automatically deploy elytra while falling", false);
    private final NumberSetting<Double> fallingDelay = new NumberSetting<>("FallingDelay", 1.5, 0.0, 10.0);

    /* Previous State */
    private boolean lastElytraPriority = this.elytraPriority.getValue();
    private long fallingTicks = 0;

    /* Initialize */
    public AutoDeploy() {
        this.autoDeploy.addSubSettings(this.fallingDelay);
        this.elytraTweaks.registerSettings(this.autoDeploy);
    }

    @Override
    public boolean isListening() {
        return this.elytraTweaks.isToggled() && this.autoDeploy.getValue();
    }

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate event) {
        final LocalPlayer player = event.getPlayer();
        final boolean isInWater = player.isInWater();
        final boolean isOnGround = player.onGround();
        final boolean isFallFlying = player.isFallFlying();
        final boolean hasLevitation = player.hasEffect(MobEffects.LEVITATION);
        if (isInWater || isOnGround || isFallFlying || hasLevitation) {
            this.lastElytraPriority = this.elytraPriority.getValue();
            this.fallingTicks = Math.round(this.fallingDelay.getValue() * 20);
            return;
        }

        if (this.fallingTicks >= -3) this.fallingTicks--;
        if (this.fallingTicks == -1) this.elytraPriority.setValue(true);
        if (this.fallingTicks == -3) {
            final var startFallFlying = new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING);
            player.connection.send(startFallFlying);
            player.startFallFlying();
        }
    }

}

