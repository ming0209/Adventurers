package dev.adventurers.forge;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.*;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,AdventurersMod.ID);
    public static final RegistryObject<Item> CHRONICLE=ITEMS.register("chronicle",()->new Item(new Item.Properties().setId(ITEMS.key("chronicle")).stacksTo(1)) {
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
            if(player instanceof ServerPlayer serverPlayer)serverPlayer.sendSystemMessage(Component.literal(AdventCommands.help()));
            return InteractionResult.SUCCESS;
        }
    });
    private ModItems() {}
}
