package com.luminblades.sword;

import com.luminblades.client.ClientHooks;
import com.luminblades.config.LuminConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Espada genérica: toda a personalidade vem do {@link SwordType} (habilidade + config). */
public class LuminSwordItem extends SwordItem {
    public final SwordType type;

    public LuminSwordItem(SwordType type) {
        super(LuminTier.INSTANCE, 3, -2.2F, new Item.Properties().rarity(Rarity.EPIC).fireResistant().stacksTo(1));
        this.type = type;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::onActivate);
        } else if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            SwordSettings s = LuminConfig.settings(type);
            type.ability.activate(sl, sp, s);
            player.getCooldowns().addCooldown(this, s.cooldown());
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel sl) {
            type.ability.onHit(sl, attacker, target, LuminConfig.settings(type));
        }
        return super.hurtEnemy(stack, target, attacker);
    }
}
