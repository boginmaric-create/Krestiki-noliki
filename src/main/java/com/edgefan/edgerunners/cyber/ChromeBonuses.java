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

    private record HeroBonus(com.edgefan.edgerunners.hero.HeroSpec hero, Attribute attribute, UUID id, double amount, AttributeModifier.Operation op) {}

    private static final HeroBonus[] HERO_BONUSES = {
            new HeroBonus(com.edgefan.edgerunners.hero.HeroSpec.RUNNER_KID, Attributes.MOVEMENT_SPEED, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0101"), 0.12, AttributeModifier.Operation.MULTIPLY_BASE),
            new HeroBonus(com.edgefan.edgerunners.hero.HeroSpec.GUNNER_GIRL, Attributes.MOVEMENT_SPEED, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0102"), 0.08, AttributeModifier.Operation.MULTIPLY_BASE),
            new HeroBonus(com.edgefan.edgerunners.hero.HeroSpec.VETERAN_BORG, Attributes.ARMOR, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0103"), 6, AttributeModifier.Operation.ADDITION),
            new HeroBonus(com.edgefan.edgerunners.hero.HeroSpec.VETERAN_BORG, Attributes.MAX_HEALTH, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0104"), 6, AttributeModifier.Operation.ADDITION),
            new HeroBonus(com.edgefan.edgerunners.hero.HeroSpec.VETERAN_BORG, Attributes.KNOCKBACK_RESISTANCE, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0105"), 0.5, AttributeModifier.Operation.ADDITION),
            new HeroBonus(com.edgefan.edgerunners.hero.HeroSpec.VETERAN_BORG, Attributes.ATTACK_DAMAGE, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0106"), 2, AttributeModifier.Operation.ADDITION),
            new HeroBonus(com.edgefan.edgerunners.hero.HeroSpec.CHROME_TYRANT, Attributes.ARMOR, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0107"), 12, AttributeModifier.Operation.ADDITION),
            new HeroBonus(com.edgefan.edgerunners.hero.HeroSpec.CHROME_TYRANT, Attributes.ARMOR_TOUGHNESS, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0108"), 4, AttributeModifier.Operation.ADDITION),
            new HeroBonus(com.edgefan.edgerunners.hero.HeroSpec.CHROME_TYRANT, Attributes.MAX_HEALTH, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c0109"), 16, AttributeModifier.Operation.ADDITION),
            new HeroBonus(com.edgefan.edgerunners.hero.HeroSpec.CHROME_TYRANT, Attributes.ATTACK_DAMAGE, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c010a"), 3, AttributeModifier.Operation.ADDITION),
            new HeroBonus(com.edgefan.edgerunners.hero.HeroSpec.CHROME_TYRANT, Attributes.KNOCKBACK_RESISTANCE, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c010b"), 0.8, AttributeModifier.Operation.ADDITION),
            new HeroBonus(com.edgefan.edgerunners.hero.HeroSpec.CHROME_TYRANT, Attributes.MOVEMENT_SPEED, UUID.fromString("6b0b3c1e-5c43-4a8e-9a41-1f0e2a7c010c"), -0.10, AttributeModifier.Operation.MULTIPLY_BASE),
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
        com.edgefan.edgerunners.hero.HeroSpec hero = com.edgefan.edgerunners.hero.HeroSpec.of(p);
        for (HeroBonus b : HERO_BONUSES) {
            AttributeInstance inst = p.getAttribute(b.attribute());
            if (inst == null) continue;
            boolean want = hero == b.hero();
            boolean has = inst.getModifier(b.id()) != null;
            if (want && !has) inst.addPermanentModifier(new AttributeModifier(b.id(), "edgerunners:hero_" + b.hero().id, b.amount(), b.op()));
            else if (!want && has) inst.removeModifier(b.id());
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Netrunner rig: faster ability recharge. */
    public static int cooldown(Player p, int base) {
        float m = CyberData.has(p, Cyberware.NETRUNNER_RIG) ? 0.6f : 1f;
        if (com.edgefan.edgerunners.hero.HeroSpec.of(p) == com.edgefan.edgerunners.hero.HeroSpec.NETRUNNER_GIRL) m *= 0.7f;
        return Math.round(base * m);
    }
}
