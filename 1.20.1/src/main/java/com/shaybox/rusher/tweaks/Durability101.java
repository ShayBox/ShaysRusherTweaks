package com.shaybox.rusher.tweaks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.render.EventRender2D;
import org.rusherhack.client.api.feature.hud.HudElement;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.render.IRenderer2D;
import org.rusherhack.client.api.render.font.IFontRenderer;
import org.rusherhack.client.api.system.IHudManager;
import org.rusherhack.client.api.ui.theme.IThemeManager;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.stage.Stage;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.feature.IFeatureManager;
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
    private final IFeatureManager<IModule> moduleManager = RusherHackAPI.getModuleManager();
    private final Screen hudEditorScreen = themeManager.getHudEditorScreen();
    private final ToggleableModule hudModule = (ToggleableModule) moduleManager.getFeature("Hud").orElseThrow();

    /* Armor Hud Element & Settings */
    private final HudElement armor = hudManager.getFeature("Armor").orElseThrow();
    private final EnumSetting<?> axis = (EnumSetting<?>) armor.getSetting("Axis");
    private final EnumSetting<?> durability = (EnumSetting<?>) armor.getSetting("Durability");

    /* Custom Settings */
    private final BooleanSetting durability101 = new BooleanSetting("Durability101", "Durability101 Mod for Armor HUD", false);
    private final BooleanSetting unbreaking = new BooleanSetting("Unbreaking", "Estimates Unbreaking Durability", false);
    private final NumberSetting<Integer> yOffset = new NumberSetting<>("Y Offset", "Manual Y Offset", 0, -15, 15);

    /* Initialize */
    public Durability101() {
        this.durability101.addSubSettings(this.unbreaking, this.yOffset);
        this.armor.registerSettings(this.durability101);
    }

    @Override
    public boolean isListening() {
        return this.hudModule.isToggled() && this.armor.isToggled() && this.durability101.getValue();
    }

    @Subscribe(priority = -10000, stage = Stage.ALL)
    private void onRender2D(EventRender2D event) {
        final Stage stage = event.getStage();
        if (this.minecraft.screen == this.hudEditorScreen) {
            if (stage != Stage.POST) return;
        } else if (stage != Stage.ON) return;

        final LocalPlayer player = this.minecraft.player;
        if (player == null || this.minecraft.options.renderDebug) {
            return;
        }

        /* Armor HUD Element: Position, Size, and Scale */
        final float scale = (float) this.armor.getScale();
        final double startX = this.armor.getStartX(), startY = this.armor.getStartY();
        final double width = this.armor.getWidth(), height = this.armor.getHeight();

        /* Font Renderer */
        final PoseStack matrixStack = event.getMatrixStack();
        final IRenderer2D renderer = this.armor.getRenderer();
        final IFontRenderer fontRenderer = this.armor.getFontRenderer();
        renderer.begin(matrixStack, fontRenderer);

        /* Matrix Stack */
        matrixStack.pushPose();
        matrixStack.translate(startX, startY + this.yOffset.getValue(), 1);
        matrixStack.scale(scale * 0.5F, scale * 0.5F, 1);

        /* Player Armor Slots (Clone & Reverse) */
        final Inventory inventory = player.getInventory();
        final ArrayList<ItemStack> armorSlots = new ArrayList<>(inventory.armor);
        Collections.reverse(armorSlots);

        for (int slot = 0; slot < armorSlots.size(); slot++) {
            final ItemStack itemStack = armorSlots.get(slot);
            if (!itemStack.isDamaged()) {
                continue;
            }

            /* Damage & Color */
            final int maxDamage = itemStack.getMaxDamage();
            final int damage = itemStack.getDamageValue();
            final int color = itemStack.getBarColor();
            final int unbreaking = this.unbreaking.getValue() ? EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, itemStack) : 0;

            /* Text Offset */
            String text = format((maxDamage - damage) * (unbreaking + 1));
            double textWidth = fontRenderer.getStringWidth(text);
            double textOffset = 1 + textWidth / 2 - textWidth;

            /* Axis Offsets */
            double x = 0, y = 0;
            if (this.axis.getDisplayValue().equals("X")) {
                x += (slot * 38) + textOffset + 16;
                y += 18;

                if (!this.durability.getDisplayValue().equals("Off")) y += 20;
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
        final DecimalFormat decimalFormat = new DecimalFormat("0.#");

        if (number >= 1000000000) return decimalFormat.format(number / 1000000000) + "b";
        if (number >= 1000000) return decimalFormat.format(number / 1000000) + "m";
        if (number >= 1000) return decimalFormat.format(number / 1000) + "k";

        return Float.toString(number).replaceAll("\\.?0*$", "");
    }

}

