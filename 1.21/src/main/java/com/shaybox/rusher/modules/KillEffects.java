package com.shaybox.rusher.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import org.rusherhack.client.api.events.network.EventPacket;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;

import java.util.HashMap;
import java.util.Map;

public class KillEffects extends ToggleableModule {

    /* Minecraft */
    private final Minecraft minecraft = Minecraft.getInstance();

    /* Settings */
    private final BooleanSetting self = new BooleanSetting("Self", "Only when you kill", false);

    /* Previous State */
    private final Map<Entity, Entity> playerAttacker = new HashMap<>();

    /* Initialize */
    public KillEffects() {
        super("KillEffects", ModuleCategory.RENDER);
        this.registerSettings(this.self);
    }

    @Subscribe
    private void onPacket(EventPacket.Receive event) {
        final Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundDamageEventPacket damagePacket) {
            assert this.minecraft.level != null;

            final DamageSource source = damagePacket.getSource(this.minecraft.level);
            if (!source.is(DamageTypes.PLAYER_ATTACK) && !source.is(DamageTypes.PLAYER_EXPLOSION)) return;

            final Entity entity = this.minecraft.level.getEntity(damagePacket.entityId());
            if (entity instanceof Player) {
                final Entity attacker = this.minecraft.level.getEntity(damagePacket.sourceCauseId());
                this.playerAttacker.put(entity, attacker);
            }
        } else if (packet instanceof ClientboundEntityEventPacket entityPacket) {
            assert this.minecraft.level != null;

            byte eventId = entityPacket.getEventId();
            if (eventId != 3) return;

            final Entity entity = entityPacket.getEntity(this.minecraft.level);
            boolean isPlayer = entity instanceof Player;
            if (!isPlayer) return;

            final Entity attacker = this.playerAttacker.get(entity);
            if (attacker == null) return;
            else this.playerAttacker.remove(entity);

            /* Only show lightning when the local player is the attacker */
            if (this.self.getValue() && attacker != this.minecraft.player) return;

            final LightningBolt lightningBolt = new LightningBolt(EntityType.LIGHTNING_BOLT, this.minecraft.level);
            lightningBolt.setPos(entity.position());
            this.minecraft.level.addEntity(lightningBolt);
        }
    }

}
