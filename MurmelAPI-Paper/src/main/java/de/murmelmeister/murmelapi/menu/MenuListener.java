package de.murmelmeister.murmelapi.menu;

import de.murmelmeister.murmelapi.menu.button.Button;
import de.murmelmeister.murmelapi.menu.model.MenuClickLocation;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class MenuListener implements Listener {
    @EventHandler
    public void handleMenuClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Menu menu = Menu.getMenu(player);
        if (menu != null) menu.handleMenuClean(event.getInventory());
    }

    @EventHandler
    public void handleMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Menu menu = Menu.getMenu(player);
        if (menu == null) return;

        int slot = event.getSlot();
        ItemStack slotItem = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        Inventory clickedInventory = event.getClickedInventory();
        ClickType clickType = event.getClick();

        InventoryAction action = event.getAction();
        MenuClickLocation whereClicked = clickedInventory != null ? clickedInventory.getType() == InventoryType.CHEST
                ? MenuClickLocation.MENU : MenuClickLocation.PLAYER_INVENTORY : MenuClickLocation.OUTSIDE;

        boolean allowed = menu.isActionAllowed(whereClicked, slot, slotItem, cursor, action);
        boolean clickAllowed = ((menu.isAllowShiftClick() || menu.isAllowShiftClick(slot)) &&
                                (clickType.isShiftClick() || clickType.isRightClick() || clickType.isLeftClick()))
                               || action.toString().contains("PICKUP") || action.toString().contains("PLACE") || action.equals(InventoryAction.SWAP_WITH_CURSOR);

        if (clickAllowed || action == InventoryAction.CLONE_STACK) {
            if (whereClicked == MenuClickLocation.MENU && slotItem != null) {
                Button button = menu.getButton(slot);
                if (button != null) menu.handleButtonClick(player, slot, action, clickType, button);
                else menu.handleMenuClick(player, slot, action, clickType, cursor, slotItem, !allowed);
            }

            if (!allowed) {
                event.setCancelled(true);
                player.updateInventory();
            }
        } else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY || whereClicked != MenuClickLocation.PLAYER_INVENTORY) {
            if (!allowed) {
                event.setCancelled(true);
                player.updateInventory();
            }

            if (player.getGameMode() == GameMode.CREATIVE && clickType.equals(ClickType.SWAP_OFFHAND))
                player.getInventory().setItemInOffHand(null);
        }
    }

    @EventHandler
    public void handleSwapItem(PlayerSwapHandItemsEvent event) {
        if (Menu.getMenu(event.getPlayer()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void handleInventoryDragTop(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Menu menu = Menu.getMenu(player);
        InventoryType type = event.getInventory().getType();

        if (menu != null && type == InventoryType.CHEST) {
            Inventory topInventory = event.getView().getTopInventory();
            int size = topInventory.getSize();

            for (int slot : event.getRawSlots()) {
                if (slot > size) continue;
                ItemStack cursor = event.getCursor() != null ? event.getCursor() : event.getOldCursor();
                if (!menu.isActionAllowed(MenuClickLocation.MENU, slot, event.getNewItems().get(slot), cursor, InventoryAction.PLACE_SOME)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
