package com.luminblades.sword;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public enum LuminTier implements Tier {
    INSTANCE;

    @Override public int getUses() { return 2031; }
    @Override public float getSpeed() { return 8.0F; }
    @Override public float getAttackDamageBonus() { return 4.0F; }
    @Override public int getLevel() { return 4; }
    @Override public int getEnchantmentValue() { return 22; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.NETHER_STAR); }
}
