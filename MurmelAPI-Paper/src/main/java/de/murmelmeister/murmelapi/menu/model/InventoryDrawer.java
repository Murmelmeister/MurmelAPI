package de.murmelmeister.murmelapi.menu.model;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class InventoryDrawer {
    private final int size;
    private String title;
    private final ItemStack[] content;

    private InventoryDrawer(int size, String title) {
        this.size = size;
        this.title = title;

        this.content = new ItemStack[size];
    }

    public int getSize() {
        return size;
    }

    public void pushItem(ItemStack item) {
        boolean added = false;

        for (int i = 0; i < this.content.length; i++) {
            final ItemStack currentItem = this.content[i];

            if (currentItem == null) {
                this.content[i] = item;
                added = true;
                break;
            }
        }

        if (!added) this.content[this.size - 1] = item;
    }

    public boolean isSet(int slot) {
        return this.getItem(slot) != null;
    }

    public ItemStack getItem(int slot) {
        return slot < this.content.length ? this.content[slot] : null;
    }

    public void setItem(int slot, ItemStack item) {
        this.content[slot] = item;
    }

    public void setContent(ItemStack[] newContent) {
        for (int i = 0; i < this.content.length; i++)
            this.content[i] = i < newContent.length ? newContent[i] : new ItemStack(Material.AIR);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void show(Player player) {
        final Inventory inventory = this.build(player);
        player.openInventory(inventory);
    }

    public Inventory build() {
        return this.build(null);
    }

    public Inventory build(InventoryHolder holder) {
        final Inventory inventory = Bukkit.createInventory(holder, this.size, MiniMessage.miniMessage().deserialize(this.title));
        inventory.setContents(this.content);
        return inventory;
    }

    public static InventoryDrawer of(int size, String title) {
        return new InventoryDrawer(size, title);
    }
}
