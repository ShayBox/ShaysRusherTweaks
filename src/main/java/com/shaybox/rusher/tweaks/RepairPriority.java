package com.shaybox.rusher.tweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.player.EventPlayerUpdate;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.utils.InventoryUtils;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.feature.IFeatureManager;
import org.rusherhack.core.setting.BooleanSetting;

import java.util.Comparator;
import java.util.List;

@SuppressWarnings("unused")
public class RepairPriority implements EventListener {

    /* Minecraft */
    private final Minecraft minecraft = Minecraft.getInstance();

    /* RusherHackAPI Managers & Modules */
    private final IFeatureManager<IModule> moduleManager = RusherHackAPI.getModuleManager();
    private final ToggleableModule autoArmor = (ToggleableModule) moduleManager.getFeature("AutoArmor").orElseThrow();
    private final BooleanSetting soft = (BooleanSetting) autoArmor.getSetting("Soft");

    /* Custom Settings */
    private final BooleanSetting repairPriority = new BooleanSetting("RepairPriority", "Prioritize low durability armor with mending", false);

    /* Equipment Slots */
    private final EquipmentSlot[] equipmentSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /* Previous State */
    private boolean isPaused = false;
    private boolean lastSoft = this.soft.getValue();

    /* Initialize */
    public RepairPriority() {
        this.autoArmor.registerSettings(this.repairPriority);
    }

    @Override
    public boolean isListening() {
        boolean isInventory = minecraft.screen == null || minecraft.screen instanceof InventoryScreen;
        return isInventory && ((this.autoArmor.isToggled() && this.repairPriority.getValue()) || this.isPaused);
    }

    @Subscribe
    private void onUpdate(EventPlayerUpdate event) {
        LocalPlayer player = event.getPlayer();

        if (this.repairPriority.getValue()) {
            if (!isPaused) {
                isPaused = true;
                this.lastSoft = this.soft.getValue();
            }

            this.soft.setValue(true);

            List<Slot> slots = player.inventoryMenu.slots
                    .stream()
                    .filter(slot -> slot.getItem().getItem() instanceof Equipable)
                    .filter(slot -> EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, slot.getItem()) > 0)
                    .sorted(Comparator.comparingInt(slot -> slot.getItem().getDamageValue()))
                    .toList();

            for (EquipmentSlot equipmentSlot : equipmentSlots) {
                for (Slot slot : slots) {
                    ItemStack itemStack = slot.getItem();
                    Item item = itemStack.getItem();
                    if (item instanceof Equipable equipable && equipable.getEquipmentSlot() != equipmentSlot) continue;

                    int armorSlotId = InventoryUtils.getInventorySlot(equipmentSlot);
                    Slot armorSlot = player.inventoryMenu.getSlot(armorSlotId);
                    ItemStack armorStack = armorSlot.getItem();

                    int itemDamageValue = itemStack.getDamageValue();
                    int armorDamageValue = armorStack.getDamageValue();
                    if (itemDamageValue > armorDamageValue) {
                        InventoryUtils.clickSlot(slot.index, false);
                        InventoryUtils.clickSlot(armorSlotId, false);
                        InventoryUtils.clickSlot(slot.index, false);
                        break;
                    }
                }
            }
        } else if (isPaused) {
            isPaused = false;
            this.soft.setValue(this.lastSoft);
        }
    }

}
