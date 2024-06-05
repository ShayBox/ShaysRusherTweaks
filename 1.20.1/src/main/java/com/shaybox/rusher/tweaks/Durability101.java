package com.shaybox.rusher.tweaks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.render.EventRender2D;
import org.rusherhack.client.api.feature.hud.HudElement;
import org.rusherhack.client.api.feature.hud.TextHudElement;
import org.rusherhack.client.api.render.IRenderer2D;
import org.rusherhack.client.api.render.font.IFontRenderer;
import org.rusherhack.client.api.system.IHudManager;
import org.rusherhack.client.api.ui.theme.IThemeManager;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.stage.Stage;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.EnumSetting;
import org.rusherhack.core.setting.NumberSetting;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

public class Durability101 implements EventListener {

    /* Minecraft & Screens */
    private final Minecraft minecraft = Minecraft.getInstance();

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
    private final BooleanSetting unbreaking = new BooleanSetting("Unbreaking", "Estimates Unbreaking Durability", false);
    private final NumberSetting<Integer> yOffset = new NumberSetting<>("Y Offset", "Manual Y Offset", 0, -15, 15);

    /* Temporary */
    private final TextHudElement watermark = (TextHudElement) hudManager.getFeature("Watermark").orElseThrow();

    /* Initialize */
    public Durability101() {
        this.durability101.addSubSettings(this.unbreaking, this.yOffset);
        this.armor.registerSettings(this.durability101);
    }

    @Override
    public boolean isListening() {
        return this.armor.isToggled() && this.durability101.getValue();
    }

    @SuppressWarnings("unused")
    @Subscribe(priority = -10000, stage = Stage.ALL)
    private void onRender2D(EventRender2D event) {
        Stage stage = event.getStage();
        if (this.minecraft.screen == this.hudEditorScreen) {
            if (stage != Stage.POST) return;
        } else if (stage != Stage.ON) return;

        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }

        /* Armor HUD Element: Position, Size, and Scale */
        float scale = (float) this.armor.getScale();
        double startX = this.armor.getStartX(), startY = this.armor.getStartY();
        double width = this.armor.getWidth(), height = this.armor.getHeight();

        /* Font Renderer */
        PoseStack matrixStack = event.getMatrixStack();
        IRenderer2D renderer = this.armor.getRenderer();
        IFontRenderer fontRenderer = this.armor.getFontRenderer();
        renderer.begin(matrixStack, fontRenderer);

        /* Matrix Stack */
        matrixStack.pushPose();
        matrixStack.translate(startX, startY + this.yOffset.getValue(), 1);
        matrixStack.scale(scale * 0.5F, scale * 0.5F, 1);

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
            int unbreaking = 0;
            if (this.unbreaking.getValue()) {
                unbreaking = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, itemStack);
            }

            /* Text Offset */
            String text = format((maxDamage - damage) * (unbreaking + 1));
            double textWidth = fontRenderer.getStringWidth(text);
            double textOffset = 1 + textWidth / 2 - textWidth;

            /* Axis Offsets */
            double x = 0, y = 0;
            if (this.axis.getDisplayValue().equals("X")) {
                x += (slot * 38) + textOffset + 16;
                y += 18;

                boolean isStable203 = this.watermark.getText().endsWith("v2.0.3");
                if (isStable203 && this.hotbarLock.getValue() && this.autoAdjust.getValue()) {
                    if (player.showVehicleHealth() && player.getVehicle() instanceof LivingEntity living) {
                        if (living.getMaxHealth() > 20) y -= 20;
                    } else if (player.isCreative()) {
                        y += 34;
                    } else if (player.isUnderWater()) {
                        y -= 20;
                    }
                }

                if (!this.durability.getDisplayValue().equals("Off")) {
                    y += 20;
                }
            } else {
                x += textOffset + 16;
                y += (slot * 38) + 18;
            }

            fontRenderer.drawString(text, x, y, color, true);
        }

        renderer.end();
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

