package com.edgefan.edgerunners.cyber;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** Stat bonuses granted by external chrome sets, kept in sync with what is installed. */
public final class ChromeBonuses {
    private record Bonus(Cyberware set, Attribute attribute, UUID id, double amount, AttributeModifier.Operation op) {}

    private static final Bonus[] BONUSES = {
            new Bonus(Cyberware.STREET_SAMURAI, Attributes.ATTACK_DAMAGE, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0001"), 2.0, AttributeModifier.Operation.ADDITION),
            new Bonus(Cyberware.STREET_SAMURAI, Attributes.ATTACK_SPEED, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0002"), 0.4, AttributeModifier.Operation.ADDITION),
            new Bonus(Cyberware.HEAVY_BORG, Attributes.ARMOR, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0003"), 6.0, AttributeModifier.Operation.ADDITION),
            new Bonus(Cyberware.HEAVY_BORG, Attributes.ARMOR_TOUGHNESS, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0004"), 2.0, AttributeModifier.Operation.ADDITION),
            new Bonus(Cyberware.HEAVY_BORG, Attributes.KNOCKBACK_RESISTANCE, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0005"), 0.4, AttributeModifier.Operation.ADDITION),
            new Bonus(Cyberware.HEAVY_BORG, Attributes.MOVEMENT_SPEED, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0006"), -0.05, AttributeModifier.Operation.MULTIPLY_BASE),
            new Bonus(Cyberware.MERC_ARMS, Attributes.MOVEMENT_SPEED, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0007"), 0.10, AttributeModifier.Operation.MULTIPLY_BASE),
            new Bonus(Cyberware.MERC_ARMS, Attributes.ATTACK_DAMAGE, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0008"), 1.0, AttributeModifier.Operation.ADDITION),
            new Bonus(Cyberware.MERC_ARMS, Attributes.MAX_HEALTH, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0009"), 4.0, AttributeModifier.Operation.ADDITION),
    };

    private ChromeBonuses() {}

    public static void sync(Player p) {
        int mask = CyberData.implants(p);
        for (Bonus b : BONUSES) {
            AttributeInstance inst = p.getAttribute(b.attribute());
            if (inst == null) continue;
            boolean want = (mask & b.set().bit()) != 0;
            boolean has = inst.getModifier(b.id()) != null;
            if (want && !has)
                inst.addPermanentModifier(new AttributeModifier(b.id(), "edgerunners:" + b.set().id, b.amount(), b.op()));
            else if (!want && has) inst.removeModifier(b.id());
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Netrunner rig: faster ability recharge. */
    public static int cooldown(Player p, int base) {
        return CyberData.has(p, Cyberware.NETRUNNER_RIG) ? Math.round(base * 0.6f) : base;
    }
}
