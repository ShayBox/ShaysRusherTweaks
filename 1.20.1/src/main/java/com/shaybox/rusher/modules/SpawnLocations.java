package com.shaybox.rusher.modules;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.rusherhack.client.api.events.render.EventRender3D;
import org.rusherhack.client.api.events.world.EventEntity;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.render.IRenderer3D;
import org.rusherhack.client.api.setting.ColorSetting;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.utils.ColorUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SpawnLocations extends ToggleableModule {

    /* Settings */
    private final BooleanSetting pause = new BooleanSetting("Pause", false);
    private final BooleanSetting players = new BooleanSetting("Players", false);
    private final BooleanSetting hostiles = new BooleanSetting("Hostiles", true);
    private final BooleanSetting passives = new BooleanSetting("Passives", false);
    private final BooleanSetting neutrals = new BooleanSetting("Neutrals", false);
    private final ColorSetting color = new ColorSetting("Color", Color.CYAN)
            .setAlphaAllowed(false)
            .setThemeSync(true);

    /* Previous State */
    private final List<BlockPos> positions = new ArrayList<>();

    /* Initialize */
    public SpawnLocations() {
        super("SpawnLocations", ModuleCategory.RENDER);
        this.registerSettings(this.pause, this.players, this.hostiles, this.passives, this.neutrals);
    }

    @Override
    public void onDisable() {
        this.positions.clear();
        super.onDisable();
    }

    @Subscribe
    private void onEntityAdd(EventEntity.Add event) {
        final Entity entity = event.getEntity();
        final BlockPos blockPos = entity.blockPosition();

        if (this.pause.getValue()) return;
        if (this.players.getValue() && !(entity instanceof Player)) return;
        if (this.hostiles.getValue() && !(entity instanceof Monster)) return;
        if (this.passives.getValue() && !(entity instanceof Animal)) return;
        if (this.neutrals.getValue() && !(entity instanceof NeutralMob)) return;

        this.positions.add(blockPos);
    }

    @Subscribe
    private void onRender3D(EventRender3D event) {
        final PoseStack matrixStack = event.getMatrixStack();
        final IRenderer3D renderer = event.getRenderer();
        renderer.begin(matrixStack);

        final int color = ColorUtils.transparency(this.color.getValueRGB(), 0.5f);
        for (BlockPos blockPos : this.positions) {
            renderer.drawBox(blockPos, true, true, color);
        }

        renderer.end();
    }
}
