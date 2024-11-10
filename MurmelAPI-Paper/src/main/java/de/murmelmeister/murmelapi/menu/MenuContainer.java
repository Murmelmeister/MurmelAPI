package de.murmelmeister.murmelapi.menu;

import de.murmelmeister.murmelapi.menu.model.MenuClickLocation;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public abstract class MenuContainer extends Menu {
    private ItemStack bottomBarFillerItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);

    protected MenuContainer() {
        this(null);
    }

    protected MenuContainer(Menu parent) {
        super(parent);
        this.setSize(9 * 3);
    }

    @Override
    public ItemStack getItemAt(int slot) {
        final ItemStack customDrop = this.getDropAt(slot);
        if (customDrop != null) return customDrop;
        if (slot > this.getSize() - 9) return this.bottomBarFillerItem;
        return null;
    }

    @Override
    protected boolean isActionAllowed(MenuClickLocation location, int slot, ItemStack clicked, ItemStack cursor, InventoryAction action) {
        if (location != MenuClickLocation.MENU && action != InventoryAction.MOVE_TO_OTHER_INVENTORY)
            return true;
        return this.canEditItem(location, slot, clicked, cursor, action);
    }

    protected boolean canEditItem(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor, InventoryAction action) {
        return this.canEditItem(slot);
    }

    protected boolean canEditItem(int slot) {
        return slot < this.getSize() - 9;
    }

    protected abstract ItemStack getDropAt(int slot);

    protected final void handleMenuClick(Player player, int slot, InventoryAction action, ClickType clickType, ItemStack cursor, ItemStack clicked, boolean cancelled) {
        if (this.canEditItem(slot) && slot < this.getSize() - 9) {
            clicked = this.handleItemClick(slot, clickType, clicked);
            this.setItem(slot, clicked);
        }
    }

    protected ItemStack handleItemClick(int slot, ClickType clickType, ItemStack item) {
        return item;
    }

    @Override
    protected void handleMenuClose(Player player, Inventory inventory) {
        final Map<Integer, ItemStack> items = new HashMap<>();

        for (int slot = 0; slot < this.getSize() - 9; slot++)
            if (this.canEditItem(slot)) {
                final ItemStack item = this.getDropAt(slot);
                items.put(slot, item);
            }
        this.handleMenuClose(items);
    }

    protected abstract void handleMenuClose(Map<Integer, ItemStack> items);

    @Override
    protected String[] getInfo() {
        return new String[]{
                "This menu allows you to drop",
                "items to this container.",
                "",
                "Simply <green>drag and drop</green> items",
                "from your inventory here."
        };
    }

    public ItemStack getBottomBarFillerItem() {
        return bottomBarFillerItem;
    }

    public void setBottomBarFillerItem(ItemStack bottomBarFillerItem) {
        this.bottomBarFillerItem = bottomBarFillerItem;
    }
}
