package de.murmelmeister.murmelapi.menu.event;

import de.murmelmeister.murmelapi.menu.Menu;
import de.murmelmeister.murmelapi.menu.model.InventoryDrawer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class MenuOpenEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    private final Menu menu;
    private final InventoryDrawer drawer;
    private final Player player;

    public MenuOpenEvent(Menu menu, InventoryDrawer drawer, Player player) {
        this.menu = menu;
        this.drawer = drawer;
        this.player = player;
    }

    public Menu getMenu() {
        return menu;
    }

    public InventoryDrawer getDrawer() {
        return drawer;
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
