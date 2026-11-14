package com.luminblades.registry;

import com.luminblades.LuminBlades;
import com.luminblades.sword.SwordType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LuminBlades.MODID);

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + LuminBlades.MODID))
                .icon(() -> new ItemStack(ModItems.SWORDS.get(SwordType.AURORA_VORTEX).get()))
                .displayItems((params, out) -> ModItems.SWORDS.values().forEach(r -> out.accept(r.get())))
                .build());
    }

    private ModCreativeTabs() {
    }
}
