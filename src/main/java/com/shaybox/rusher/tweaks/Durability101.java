package com.shaybox.rusher.tweaks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.render.EventRender2D;
import org.rusherhack.client.api.feature.hud.HudElement;
import org.rusherhack.client.api.render.font.IFontRenderer;
import org.rusherhack.client.api.system.IHudManager;
import org.rusherhack.client.api.ui.theme.IThemeManager;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.stage.Stage;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.EnumSetting;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("unused")
public class Durability101 implements EventListener {

    /* Minecraft & Screens */
    private final Minecraft minecraft = Minecraft.getInstance();
    private final DebugScreenOverlay debugOverlay = minecraft.getDebugOverlay();

    /* RusherHackAPI Managers & Screens */
    private final IHudManager hudManager = RusherHackAPI.getHudManager();
    private final IThemeManager themeManager = RusherHackAPI.getThemeManager();
    private final Screen hudEditorScreen = themeManager.getHudEditorScreen();

    /* Armor Hud Element & Settings */
    private final HudElement armor = hudManager.getFeature("Armor").orElseThrow();
    private final EnumSetting<?> axis = (EnumSetting<?>) armor.getSetting("Axis");
    private final EnumSetting<?> durability = (EnumSetting<?>) armor.getSetting("Durability");
    private final BooleanSetting hotbarLock = (BooleanSetting) armor.getSetting("HotbarLock");
    private final BooleanSetting autoAdjust = (BooleanSetting) hotbarLock.getSubSetting("AutoAdjust");

    /* Custom Settings */
    private final BooleanSetting durability101 = new BooleanSetting("Durability101", "Durability101 Mod for Armor HUD", false);

    /* Initialize */
    public Durability101() {
        this.armor.registerSettings(this.durability101);
    }

    @Override
    public boolean isListening() {
        return this.armor.isToggled() && this.durability101.getValue();
    }

    @Subscribe(priority = -10000, stage = Stage.ALL)
    private void onUpdate(EventRender2D event) {
        Stage stage = event.getStage();
        if (this.minecraft.screen == this.hudEditorScreen) {
            if (stage != Stage.POST) return;
        } else {
            if (stage != Stage.ON) return;
        }

        LocalPlayer player = this.minecraft.player;
        if (player == null || debugOverlay.showDebugScreen()) {
            return;
        }

        /* Armor HUD Element: Position, Size, and Scale */
        float scale = (float) this.armor.getScale();
        double startX = (this.armor.getStartX() * 2) / scale, startY = (this.armor.getStartY() * 2) / scale;
        double width = (this.armor.getScaledWidth() * 2) / scale, height = (this.armor.getScaledHeight() * 2) / scale;
        double fourWidth = width / 4, fourHeight = height / 4, halfWidth = width / 2, halfHeight = height / 2;

        /* Matrix Stack */
        PoseStack matrixStack = event.getMatrixStack();
        matrixStack.pushPose();
        matrixStack.scale(0.5F * scale, 0.5F * scale, 0.5F * scale);

        /* Font Renderer */
        IFontRenderer fontRenderer = this.armor.getFontRenderer();
        fontRenderer.begin(matrixStack);

        /* Player Armor Slots (Clone & Reverse) */
        Inventory inventory = player.getInventory();
        ArrayList<ItemStack> armorSlots = new ArrayList<>(inventory.armor);
        Collections.reverse(armorSlots);

        for (int slot = 0; slot < armorSlots.size(); slot++) {
            ItemStack itemStack = armorSlots.get(slot);
            if (!itemStack.isDamaged()) {
                continue;
            }

            /* Damage & Color */
            int maxDamage = itemStack.getMaxDamage();
            int damage = itemStack.getDamageValue();
            int color = itemStack.getBarColor();

            /* Text Offset */
            String text = format(maxDamage - damage);
            double textWidth = fontRenderer.getStringWidth(text);

            /* Axis Offsets */
            double x, y, slotOffset = 0.5 + slot;
            if (this.axis.getDisplayValue().equals("X")) {
                /* Horizontal Offset */
                int[] offsets = {10, 8, 6, 4};
                int xOffset = offsets[slot];

                /* AutoAdjust Vertical Offset */
                int autoAdjustOffset = 0;
                if (this.hotbarLock.getValue() && this.autoAdjust.getValue()) {
                    /* RusherHack Behavior */
                    if (player.showVehicleHealth() && player.getVehicle() instanceof LivingEntity living) {
                        if (living.getMaxHealth() > 20) autoAdjustOffset -= 20;
                    } else if (player.isCreative()) {
                        autoAdjustOffset += 34;
                    } else if (player.isUnderWater()) {
                        autoAdjustOffset -= 20;
                    }
                    /* Fixed Behavior */
//                if (player.showVehicleHealth() && player.getVehicle() instanceof LivingEntity living) {
//                    float maxHealth = living.getMaxHealth();
//                    int rows = (int) Math.ceil(maxHealth / 20);
//                    autoAdjustOffset -= 20 * rows;
//                }
//
//                if (player.isCreative()) {
//                    autoAdjustOffset += 34;
//                } else if (player.isUnderWater()) {
//                    autoAdjustOffset -= 20;
//                }
                }

                x = startX + (fourWidth * slotOffset) - xOffset;
                y = startY + halfHeight + autoAdjustOffset;
            } else {
                int xOffset = 10;
                if (!this.durability.getDisplayValue().equals("Off")) {
                    xOffset += 20;
                }

                x = startX + halfWidth - xOffset;
                y = startY + (fourHeight * slotOffset) + (slot * 2);
            }

            fontRenderer.drawString(text, x, y, color, true);
        }

        fontRenderer.end();
        matrixStack.popPose();
    }

    public String format(float number) {
        DecimalFormat decimalFormat = new DecimalFormat("0.#");

        if (number >= 1000000000) return decimalFormat.format(number / 1000000000) + "b";
        if (number >= 1000000) return decimalFormat.format(number / 1000000) + "m";
        if (number >= 1000) return decimalFormat.format(number / 1000) + "k";

        return Float.toString(number).replaceAll("\\.?0*$", "");
    }

}

