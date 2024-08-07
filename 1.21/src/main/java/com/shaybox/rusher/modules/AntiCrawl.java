package com.shaybox.rusher.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.rusherhack.client.api.accessors.client.IMixinMultiPlayerGameMode;
import org.rusherhack.client.api.events.network.EventPacket;
import org.rusherhack.client.api.events.player.EventPlayerUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;

public class AntiCrawl extends ToggleableModule {

    /* Minecraft */
    private final Minecraft minecraft = Minecraft.getInstance();

    /* Settings */
    private final BooleanSetting packetOnly = new BooleanSetting("PacketMine", false);
    private final BooleanSetting pearlPhase = new BooleanSetting("PearlPhase", false);
    private final BooleanSetting crawlBreak = new BooleanSetting("CrawlBreak", false);

    /* Initialize */
    public AntiCrawl() {
        super("AntiCrawl", ModuleCategory.COMBAT);
        this.registerSettings(this.packetOnly, this.pearlPhase, this.crawlBreak);
    }

    @Subscribe
    private void onPacket(EventPacket.Receive event) {
        final Level level = this.minecraft.level;
        final LocalPlayer player = this.minecraft.player;
        final MultiPlayerGameMode gameMode = this.minecraft.gameMode;
        if (level == null || player == null || gameMode == null) return;

        final Packet<?> packet = event.getPacket();

        // Start breaking the block your head is in if the block your feet are in starts to get mined to keep you from crawling
        if (this.pearlPhase.getValue() && packet instanceof ClientboundBlockDestructionPacket destructionPacket) {
            final BlockPos playerFeetPos = player.blockPosition();
            final BlockPos playerHeadPos = playerFeetPos.offset(0, 1, 0);
            final BlockPos blockPos = destructionPacket.getPos();
            final BlockState blockState = level.getBlockState(blockPos);
            final Block block = blockState.getBlock();
            if (!blockPos.equals(playerFeetPos) || block.equals(Blocks.AIR)) return;

            gameMode.startDestroyBlock(playerHeadPos, Direction.DOWN);
        }
    }

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate event) {
        final Level level = this.minecraft.level;
        final LocalPlayer player = this.minecraft.player;
        final MultiPlayerGameMode gameMode = this.minecraft.gameMode;
        final IMixinMultiPlayerGameMode gameModeMixin = (IMixinMultiPlayerGameMode) gameMode;
        if (level == null || player == null || gameMode == null) return;

        final BlockPos destroyBlockPos = gameModeMixin.getDestroyBlockPos();
        final BlockPos playerFeetPos = player.blockPosition();

        // Finish vanilla breaking the block in your head to keep you from crawling
        if (this.pearlPhase.getValue() && !this.packetOnly.getValue()) {
            final BlockPos playerHeadPos = playerFeetPos.offset(0, 1, 0);
            final BlockState blockState = level.getBlockState(playerHeadPos);
            final Block block = blockState.getBlock();

            if (destroyBlockPos.equals(playerHeadPos) && !block.equals(Blocks.AIR)) {
                gameMode.continueDestroyBlock(playerHeadPos, Direction.DOWN);
            }
        }

        // Finish vanilla breaking the block above your head to stop you from crawling
        if (this.crawlBreak.getValue() && player.isVisuallyCrawling()) {
            final BlockPos playerHeadPos = playerFeetPos.offset(0, 1, 0);

            if (!destroyBlockPos.equals(playerHeadPos)) {
                gameMode.startDestroyBlock(playerHeadPos, Direction.DOWN);
            } else if (!this.packetOnly.getValue()) {
                gameMode.continueDestroyBlock(playerHeadPos, Direction.DOWN);
            }
        }
    }

}
