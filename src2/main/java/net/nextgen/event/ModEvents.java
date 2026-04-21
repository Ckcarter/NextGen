package net.nextgen.event;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nextgen.NextGen;
import net.nextgen.entity.ModEntityTypes;
import net.nextgen.entity.custom.CompanionEntity;

@Mod.EventBusSubscriber(modid = NextGen.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {

    private ModEvents() {}

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.COMPANION.get(), CompanionEntity.createAttributes().build());
    }
}
