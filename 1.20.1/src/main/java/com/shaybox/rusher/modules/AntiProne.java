package com.shaybox.rusher.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import org.rusherhack.client.api.events.network.EventPacket;
import org.rusherhack.client.api.events.player.EventPlayerUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;

public class AntiProne extends ToggleableModule {

    /* Minecraft */
    private final Minecraft minecraft = Minecraft.getInstance();

    /* Settings */
    private final BooleanSetting packetOnly = new BooleanSetting("PacketOnly", "Only start mining the block", false);

    /* Initialize */
    public AntiProne() {
        super("AntiProne", ModuleCategory.COMBAT);
        this.registerSettings(this.packetOnly);
    }

    @Subscribe
    private void onPacket(EventPacket.Receive event) {
        final MultiPlayerGameMode gameMode = this.minecraft.gameMode;
        final LocalPlayer player = this.minecraft.player;
        if (gameMode == null || player == null) return;

        final Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundBlockDestructionPacket destructionPacket) {
            final BlockPos playerFeetPos = player.blockPosition();
            final BlockPos playerHeadPos = playerFeetPos.offset(0, 1, 0);
            final BlockPos blockPos = destructionPacket.getPos();
            if (!blockPos.equals(playerFeetPos)) return;

            gameMode.startDestroyBlock(playerHeadPos, Direction.DOWN);
        }
    }

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate event) {
        if (this.packetOnly.getValue()) return;

        final LocalPlayer player = event.getPlayer();
        final MultiPlayerGameMode gameMode = this.minecraft.gameMode;
        if (gameMode == null) return;

        final BlockPos playerFeetPos = player.blockPosition();
        final BlockPos playerHeadPos = playerFeetPos.offset(0, 1, 0);
        if (gameMode.isDestroying()) gameMode.continueDestroyBlock(playerHeadPos, Direction.DOWN);
    }

}
