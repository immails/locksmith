package gg.moonflower.locksmith.core;

import gg.moonflower.locksmith.client.lock.ClientLockManager;
import gg.moonflower.locksmith.client.screen.LocksmithingTableScreen;
import gg.moonflower.locksmith.common.lock.ServerLockManager;
import gg.moonflower.locksmith.common.menu.LocksmithingTableMenu;
import gg.moonflower.locksmith.common.network.LocksmithMessages;
import gg.moonflower.locksmith.core.registry.LocksmithBlocks;
import gg.moonflower.locksmith.core.registry.LocksmithItems;
import gg.moonflower.locksmith.core.registry.LocksmithLocks;
import gg.moonflower.locksmith.core.registry.LocksmithMenus;
import gg.moonflower.pollen.api.event.events.registry.RegisterAtlasSpriteEvent;
import gg.moonflower.pollen.api.platform.Platform;
import gg.moonflower.pollen.api.registry.ClientRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;

public class Locksmith {
    public static final String MOD_ID = "locksmith";
    public static final Platform PLATFORM = Platform.builder(MOD_ID)
            .clientInit(Locksmith::onClientInit)
            .clientPostInit(Locksmith::onClientPostInit)
            .commonInit(Locksmith::onCommonInit)
            .commonPostInit(Locksmith::onCommonPostInit)
            .build();

    public static void onClientInit() {
        ClientRegistries.registerScreenFactory(LocksmithMenus.LOCKSMITHING_TABLE_MENU.get(), LocksmithingTableScreen::new);
        RegisterAtlasSpriteEvent.event(InventoryMenu.BLOCK_ATLAS).register((atlas, registry) -> registry.accept(LocksmithingTableMenu.EMPTY_SLOT_KEY));
        ClientRegistries.registerItemOverride(LocksmithItems.KEYRING.get(), new ResourceLocation(Locksmith.MOD_ID, "keys"), (stack, level, livingEntity) -> Mth.clamp(4 / 4F, 0, 1));
    }

    public static void onClientPostInit(Platform.ModSetupContext ctx) {
        ClientLockManager.init();
    }

    public static void onCommonInit() {
        LocksmithBlocks.BLOCKS.register(Locksmith.PLATFORM);
        LocksmithItems.ITEMS.register(Locksmith.PLATFORM);
        LocksmithLocks.LOCKS.register(Locksmith.PLATFORM);
        LocksmithMessages.init();
        ServerLockManager.init();
    }

    public static void onCommonPostInit(Platform.ModSetupContext ctx) {
    }
}
