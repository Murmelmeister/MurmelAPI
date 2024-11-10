package de.murmelmeister.murmelapi.menu.button;

import de.murmelmeister.murmelapi.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public abstract class Button {
    private final int slot;

    public Button(int slot) {
        this.slot = slot;
    }

    public Button(int slot, Menu currentMenu) {
        this.slot = slot;
        currentMenu.registerButton(slot, this);
    }

    public int getSlot() {
        return slot;
    }

    public abstract ItemStack getIcon();

    public abstract void click(Player player, Menu menu, ClickType clickType);
}
