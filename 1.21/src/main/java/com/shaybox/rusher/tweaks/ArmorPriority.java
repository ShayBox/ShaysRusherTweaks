package com.shaybox.rusher.tweaks;

import com.shaybox.rusher.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.events.player.EventPlayerUpdate;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.setting.BindSetting;
import org.rusherhack.client.api.utils.InventoryUtils;
import org.rusherhack.core.bind.key.IKey;
import org.rusherhack.core.bind.key.NullKey;
import org.rusherhack.core.event.listener.EventListener;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.feature.IFeatureManager;
import org.rusherhack.core.setting.BooleanSetting;

import java.util.Comparator;
import java.util.List;

public class ArmorPriority implements EventListener {

    /* Minecraft */
    private final Minecraft minecraft = Minecraft.getInstance();

    /* RusherHackAPI Managers & Modules */
    private final IFeatureManager<IModule> moduleManager = RusherHackAPI.getModuleManager();
    private final ToggleableModule autoArmor = (ToggleableModule) moduleManager.getFeature("AutoArmor").orElseThrow();

    /* AutoArmor Settings */
    private final BooleanSetting soft = (BooleanSetting) autoArmor.getSetting("Soft");
    private final BooleanSetting inventory = (BooleanSetting) autoArmor.getSetting("Inventory");
    private final BooleanSetting elytraPriority = (BooleanSetting) autoArmor.getSetting("ElytraPriority");
    private final BooleanSetting repairPriority = new BooleanSetting("RepairPriority", "Prioritize low durability armor with mending", false);
    private final BooleanSetting goldenPriority = new BooleanSetting("GoldenPriority", "Prioritize one piece of golden armor for piglins", false);
    private final BindSetting elytraPriorityBind = new BindSetting("Bind", NullKey.INSTANCE);
    private final BindSetting repairPriorityBind = new BindSetting("Bind", NullKey.INSTANCE);
    private final BindSetting goldenPriorityBind = new BindSetting("Bind", NullKey.INSTANCE);

    /* Equipment Slots & Golden Armor Priority */
    private final EquipmentSlot[] equipmentSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final List<Item> goldenArmorPriority = List.of(Items.GOLDEN_BOOTS, Items.GOLDEN_HELMET, Items.GOLDEN_LEGGINGS, Items.GOLDEN_CHESTPLATE);

    /* Previous State */
    private boolean isPaused = false;
    private boolean isElytraDown = false;
    private boolean isRepairDown = false;
    private boolean isGoldenDown = false;
    private boolean lastSoft = this.soft.getValue();

    /* Initialize */
    public ArmorPriority() {
        this.elytraPriority.addSubSettings(this.elytraPriorityBind);
        this.repairPriority.addSubSettings(this.repairPriorityBind);
        this.goldenPriority.addSubSettings(this.goldenPriorityBind);
        this.autoArmor.registerSettings(this.repairPriority, this.goldenPriority);
    }

    @Override
    public boolean isListening() {
        final IKey elytraBind = this.elytraPriorityBind.getValue();
        final IKey repairBind = this.repairPriorityBind.getValue();
        final IKey goldenBind = this.goldenPriorityBind.getValue();

        return this.isPaused || this.autoArmor.isToggled() && (
                (this.repairPriority.getValue() || this.goldenPriority.getValue()) || (
                        (this.isElytraDown || this.isRepairDown || this.isGoldenDown) || (
                                (elytraBind.isKeyDown() || repairBind.isKeyDown() || goldenBind.isKeyDown())
                        )
                )
        );
    }

    @Subscribe
    private void onUpdate(EventUpdate event) {
        if (minecraft.screen != null) return;

        Main.handleKey(this.elytraPriorityBind::getValue, this.isElytraDown, (value) -> this.isElytraDown = value, this.elytraPriority::getValue, this.elytraPriority::setValue);
        Main.handleKey(this.repairPriorityBind::getValue, this.isRepairDown, (value) -> this.isRepairDown = value, this.repairPriority::getValue, this.repairPriority::setValue);
        Main.handleKey(this.goldenPriorityBind::getValue, this.isGoldenDown, (value) -> this.isGoldenDown = value, this.goldenPriority::getValue, this.goldenPriority::setValue);
    }

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate event) {
        final LocalPlayer player = event.getPlayer();
        final Inventory inventory = player.getInventory();
        final ClientLevel level = this.minecraft.level;
        if (level == null) return;

        /* Prevent moving items in other containers */
        if (this.minecraft.screen instanceof InventoryScreen) if (!this.inventory.getValue()) return;
        else if (this.minecraft.screen instanceof AbstractContainerScreen) return;

        final boolean prioritizeRepair = this.repairPriority.getValue();
        final boolean prioritizeGolden = this.goldenPriority.getValue();

        /* There can only be one enabled at a time */
        if (prioritizeRepair || prioritizeGolden) {
            if (!isPaused) {
                isPaused = true;
                this.lastSoft = this.soft.getValue();
            }

            this.soft.setValue(true);

            if (prioritizeRepair) {
                this.goldenPriority.setValue(false);

                for (EquipmentSlot equipmentSlot : equipmentSlots) {
                    final int armorSlotId = InventoryUtils.getInventorySlot(equipmentSlot);
                    final Holder.Reference<Enchantment> mending = level
                            .registryAccess()
                            .registryOrThrow(Registries.ENCHANTMENT)
                            .getHolderOrThrow(Enchantments.MENDING);

                    player.inventoryMenu.slots.stream()
                            .filter(slot -> slot.getItem().getItem() instanceof Equipable e && e.getEquipmentSlot() == equipmentSlot)
                            .filter(slot -> EnchantmentHelper.getItemEnchantmentLevel(mending, slot.getItem()) > 0)
                            .max(Comparator.comparingInt(slot -> slot.getItem().getDamageValue()))
                            .ifPresent(slot -> {
                                Slot armorSlot = player.inventoryMenu.getSlot(armorSlotId);
                                ItemStack armorStack = armorSlot.getItem();
                                ItemStack itemStack = slot.getItem();

                                final int itemDamage = itemStack.getDamageValue();
                                final int armorDamage = armorStack.getDamageValue();
                                if (itemDamage > armorDamage) swapSlotArmorId(slot, armorSlotId);
                            });
                }
            }

            if (prioritizeGolden) {
                this.repairPriority.setValue(false);

                for (Item goldenArmor : this.goldenArmorPriority)
                    player.inventoryMenu.slots.stream()
                            .filter(slot -> slot.getItem().getItem() instanceof Equipable e && e == goldenArmor)
                            .filter(slot -> { /* Only one piece of golden armor */
                                for (Item item : this.goldenArmorPriority)
                                    if (inventory.armor.stream().map(ItemStack::getItem).toList().contains(item))
                                        return false;

                                return true;
                            }).findFirst().ifPresent(slot -> {
                                Equipable equipable = (Equipable) slot.getItem().getItem();
                                EquipmentSlot equipmentSlot = equipable.getEquipmentSlot();
                                int armorSlotId = InventoryUtils.getInventorySlot(equipmentSlot);
                                this.swapSlotArmorId(slot, armorSlotId);
                            });
            }
        } else if (isPaused) {
            isPaused = false;
            this.soft.setValue(this.lastSoft);
        }
    }

    private void swapSlotArmorId(Slot slot, int armorSlotId) {
        InventoryUtils.clickSlot(slot.index, false);
        InventoryUtils.clickSlot(armorSlotId, false);
        InventoryUtils.clickSlot(slot.index, false);
    }

}
