package de.murmelmeister.murmelapi.menu.event;

import de.murmelmeister.murmelapi.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public final class MenuCloseEvent extends Event implements Cancellable {
    public static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    private final Menu menu;
    private final Inventory inventory;
    private final Player player;

    public MenuCloseEvent(Menu menu, Inventory inventory, Player player) {
        this.menu = menu;
        this.inventory = inventory;
        this.player = player;
    }

    public Menu getMenu() {
        return menu;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
