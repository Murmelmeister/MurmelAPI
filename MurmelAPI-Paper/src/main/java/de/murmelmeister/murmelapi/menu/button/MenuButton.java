package de.murmelmeister.murmelapi.menu.button;

import de.murmelmeister.murmelapi.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public final class MenuButton extends Button {
    private final Menu nextMenu;
    private final ItemStack item;

    public MenuButton(int slot, Menu currentMenu, Menu nextMenu, ItemStack item) {
        super(slot, currentMenu);
        this.nextMenu = nextMenu;
        this.item = item;
    }

    @Override
    public ItemStack getIcon() {
        return item;
    }

    @Override
    public void click(Player player, Menu menu, ClickType clickType) {
        if (nextMenu == null) throw new NullPointerException("nextMenu is null");
        this.nextMenu.show(player);
    }
}
