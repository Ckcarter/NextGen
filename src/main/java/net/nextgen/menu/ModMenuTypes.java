package net.nextgen.menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.nextgen.NextGen;

public final class ModMenuTypes {

    private ModMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, NextGen.MOD_ID);

    public static final RegistryObject<MenuType<CompanionSkinMenu>> COMPANION_SKIN = MENU_TYPES.register(
            "companion_skin", () -> IForgeMenuType.create(CompanionSkinMenu::new));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}