package com.luminblades.registry;

import com.luminblades.LuminBlades;
import com.luminblades.sword.LuminSwordItem;
import com.luminblades.sword.SwordType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LuminBlades.MODID);
    public static final Map<SwordType, RegistryObject<Item>> SWORDS = new EnumMap<>(SwordType.class);

    static {
        for (SwordType t : SwordType.values()) {
            SWORDS.put(t, ITEMS.register(t.id, () -> new LuminSwordItem(t)));
        }
    }

    private ModItems() {
    }
}
