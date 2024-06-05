package com.shaybox.rusher.tweaks;

import com.shaybox.rusher.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.events.player.EventPlayerUpdate;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.setting.BindSetting;
import org.rusherhack.client.api.system.INotificationManager;
import org.rusherhack.core.bind.key.IKey;
import org.rusherhack.core.bind.key.NullKey;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.feature.IFeatureManager;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.EnumSetting;
import org.rusherhack.core.setting.NumberSetting;

import java.util.LinkedList;
import java.util.Queue;

public class AutoRestart implements EventListener {

    /* Minecraft */
    private final Minecraft minecraft = Minecraft.getInstance();

    /* RusherHackAPI & Modules */
    private final IFeatureManager<IModule> moduleManager = RusherHackAPI.getModuleManager();
    private final INotificationManager notificationManager = RusherHackAPI.getNotificationManager();
    private final ToggleableModule autoWalk = (ToggleableModule) moduleManager.getFeature("AutoWalk").orElseThrow();
    private final ToggleableModule elytraFly = (ToggleableModule) moduleManager.getFeature("ElytraFly").orElseThrow();
    private final ToggleableModule rotationLock = (ToggleableModule) moduleManager.getFeature("RotationLock").orElseThrow();

    /* Custom Settings */
    private final NumberSetting<Double> minSpeed = new NumberSetting<>("MinSpeed", "Minimum speed to restart", 0.14, 0.1, 10.0);
    private final NumberSetting<Integer> averageTicks = new NumberSetting<>("AverageTicks", "How many ticks to average", 20, 1, 20 * 10);
    private final NumberSetting<Integer> cooldownTicks = new NumberSetting<>("CooldownTicks", "How many ticks to pause", 60, 1, 20 * 10);
    private final BooleanSetting autoRestart = new BooleanSetting("AutoRestart", "Automatically restart packet fly if your speed drops", false);
    private final BooleanSetting autoSneak = new BooleanSetting("AutoSneak", "Automatically sneak if your speed drops", true);
    private final BooleanSetting holdSneak = new BooleanSetting("Hold", "Hold down sneak", true);
    private final BooleanSetting debug = new BooleanSetting("DebugNotifications", true);
    private final BindSetting autoRestartBind = new BindSetting("Bind", NullKey.INSTANCE);
    private final EnumSetting<?> mode = (EnumSetting<?>) this.elytraFly.getSetting("Mode");

    /* Previous State */
    private int pauseCooldown = 0;
    private boolean isAutoRestartDown = false;
    private boolean isPaused = this.autoRestart.getValue();
    private boolean lastAutoWalk = this.autoWalk.isToggled();
    private boolean lastElytraFly = this.elytraFly.isToggled();
    private boolean lastRotationLock = this.rotationLock.isToggled();
    private final Queue<Double> speedBuffer = new LinkedList<>();

    /* Initialize */
    public AutoRestart() {
        this.autoSneak.addSubSettings(this.holdSneak);
        this.autoRestart.addSubSettings(this.minSpeed, this.averageTicks, this.cooldownTicks, this.autoSneak, this.debug, this.autoRestartBind);
        this.elytraFly.registerSettings(this.autoRestart);
    }

    @Override
    public boolean isListening() {
        IKey autoRestartBind = this.autoRestartBind.getValue();

        return this.elytraFly.isToggled() || this.autoRestart.getValue() || this.isAutoRestartDown || autoRestartBind.isKeyDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    private void onUpdate(EventUpdate event) {
        if (minecraft.screen != null) return;

        Main.handleKey(this.autoRestartBind::getValue, this.isAutoRestartDown, (value) -> this.isAutoRestartDown = value, this.autoRestart::getValue, this.autoRestart::setValue);
    }

    @SuppressWarnings("unused")
    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate event) {
        LocalPlayer player = event.getPlayer();
        Vec3 position = player.position();

        if (!this.mode.getDisplayValue().equals("Packet")) {
            this.autoRestart.setValue(false);
            return;
        }

        /* Calculate Player Speed */
        double dx = position.x - player.xOld;
        double dy = position.y - player.yOld;
        double dz = position.z - player.zOld;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);

        /* Update speed buffer */
        this.speedBuffer.offer(speed);
        while (this.speedBuffer.size() >= this.averageTicks.getValue()) {
            this.speedBuffer.poll();
        }

        /* Calculate Average Speed */
        double averageSpeed = speedBuffer.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        /* Make sure modules do not de-sync */
        if (!this.autoRestart.getValue()) {
            if (!this.isPaused) {
                this.isPaused = true;
                this.pauseCooldown = 0;

                this.autoWalk.setToggled(false);
                this.elytraFly.setToggled(false);
                this.rotationLock.setToggled(false);

                if (this.autoSneak.getValue() && this.holdSneak.getValue()) minecraft.options.keyShift.setDown(false);
            }

            return;
        } else this.isPaused = false;

        if (this.elytraFly.isToggled()) {
            if (averageSpeed < this.minSpeed.getValue()) {
                if (this.pauseCooldown == -1) {
                    this.pauseCooldown = this.cooldownTicks.getValue();
                    this.lastAutoWalk = this.autoWalk.isToggled();
                    this.lastElytraFly = this.elytraFly.isToggled();
                    this.lastRotationLock = this.rotationLock.isToggled();

                    this.elytraFly.setToggled(false);

                    if (this.debug.getValue()) this.notificationManager.info("ElytraFly Paused");
                    if (this.autoSneak.getValue()) {
                        if (this.holdSneak.getValue()) minecraft.options.keyShift.setDown(true);
                        else player.setShiftKeyDown(true);
                    }
                }
            } else if (this.pauseCooldown == 0) {
                /* Above Minimum Speed */
                this.pauseCooldown = -1;
            }
        } else {
            if (this.pauseCooldown > 0) {
                --this.pauseCooldown;

                /* Make sure modules do not de-sync */
                this.autoWalk.setToggled(this.lastAutoWalk);
                this.rotationLock.setToggled(this.lastRotationLock);
            } else if (this.pauseCooldown == 0) {
                this.autoWalk.setToggled(this.lastAutoWalk);
                this.elytraFly.setToggled(this.lastElytraFly);
                this.rotationLock.setToggled(this.lastRotationLock);

                if (this.debug.getValue()) this.notificationManager.info("ElytraFly Resumed");
                if (this.autoSneak.getValue() && this.holdSneak.getValue()) minecraft.options.keyShift.setDown(false);
            }
        }
    }

}
