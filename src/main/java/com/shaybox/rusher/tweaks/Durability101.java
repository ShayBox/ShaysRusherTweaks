package com.shaybox.rusher.tweaks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.render.EventRender2D;
import org.rusherhack.client.api.feature.hud.HudElement;
import org.rusherhack.client.api.render.font.IFontRenderer;
import org.rusherhack.client.api.system.IHudManager;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;

import java.text.DecimalFormat;

@SuppressWarnings("unused")
public class Durability101 implements EventListener {

    /* Minecraft */
    private final Minecraft minecraft = Minecraft.getInstance();

    /* RusherHackAPI & Elements */
    private final IHudManager hudManager = RusherHackAPI.getHudManager();
    private final HudElement armor = hudManager.getFeature("Armor").orElseThrow();
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

    @Subscribe
    private void onUpdate(EventRender2D event) {
        if (minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        /* Armor HUD Element Position, Size, and Scale */
        float scale = (float) this.armor.getScale();
        double startX = (this.armor.getStartX() * 2) / scale, startY = (this.armor.getStartY() * 2) / scale;
        double width = (this.armor.getScaledWidth() * 2) / scale, height = (this.armor.getScaledHeight() * 2) / scale;
        double fourWidth = width / 4, halfHeight = height / 2;

        /* Matrix Stack */
        PoseStack matrixStack = event.getMatrixStack();
        matrixStack.pushPose();
        matrixStack.scale(0.5F * scale, 0.5F * scale, 0.5F * scale);

        /* Font Renderer */
        IFontRenderer fontRenderer = this.armor.getFontRenderer();
        fontRenderer.begin(matrixStack);

        /* Player Armor Slots */
        Inventory inventory = player.getInventory();
        NonNullList<ItemStack> armorSlots = inventory.armor;
        for (int slot = 0; slot < armorSlots.size(); slot++) {
            /* ItemStack Damage & Color */
            ItemStack itemStack = armorSlots.get(slot);
            int maxDamage = itemStack.getMaxDamage();
            int damage = itemStack.getDamageValue();
            int color = itemStack.getBarColor();

            /* Text Offset */
            String text = format(maxDamage - damage);
            double textWidth = fontRenderer.getStringWidth(text);

            /* Horizontal Offset */
            int[] offsets = {10, 8, 6, 4};
            int xOffset = offsets[slot];
            double slotOffset = 0.5 + slot;

            /* Vertical Offset */
            int yOffset = 0;
            if (this.hotbarLock.getValue() && this.autoAdjust.getValue()) {
                if (player.showVehicleHealth() && player.getVehicle() instanceof LivingEntity e && e.getMaxHealth() > 20) {
                    yOffset -= 20;
                } else if (player.isCreative()) {
                    yOffset += 34;
                } else if (player.isUnderWater()) {
                    yOffset -= 20;
                }
//                if (player.showVehicleHealth() && player.getVehicle() instanceof LivingEntity living) {
//                    float maxHealth = living.getMaxHealth();
//                    int rows = (int) Math.ceil(maxHealth / 20);
//
//                    yOffset -= 20 * rows;
//                }
//
//                if (player.isCreative()) {
//                    yOffset += 34;
//                } else if (player.isUnderWater()) {
//                    yOffset -= 20;
//                }
            }

            /* Durability101 */
            double x = startX + (fourWidth * slotOffset) - xOffset;
            double y = startY + halfHeight + yOffset;
            fontRenderer.drawString(text, x, y, color, false);
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

