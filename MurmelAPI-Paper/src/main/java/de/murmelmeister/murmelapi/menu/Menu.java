package de.murmelmeister.murmelapi.menu;

import de.murmelmeister.murmelapi.MurmelPlugin;
import de.murmelmeister.murmelapi.menu.button.BackButton;
import de.murmelmeister.murmelapi.menu.button.Button;
import de.murmelmeister.murmelapi.menu.button.InfoButton;
import de.murmelmeister.murmelapi.menu.event.MenuCloseEvent;
import de.murmelmeister.murmelapi.menu.event.MenuOpenEvent;
import de.murmelmeister.murmelapi.menu.model.InventoryDrawer;
import de.murmelmeister.murmelapi.menu.model.MenuClickLocation;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class Menu {
    private final Map<Integer, Button> buttons = new HashMap<>();

    private static final String TAG = MurmelPlugin.getInstance().getName();
    private static final String MENU_TAG_CURRENT = TAG + "_Menu";
    private static final String MENU_TAG_PREVIOUS = TAG + "_PreviousMenu";
    private static final String MENU_TAG_LAST_CLOSED = TAG + "_LastClosedMenu";

    private final Menu parent;
    private BackButton backButton = null;

    private String title = "<gray>Menu";
    private int size = 27;
    private Player viewer;
    private boolean slotNumberVisible = false;
    private boolean opened = false;
    private boolean allowShiftClick = false;

    private int infoSlot = -1;
    private Material placeholder = null;

    protected Menu() {
        this(null);
    }

    protected Menu(Menu parent) {
        this.parent = parent;
        if (parent != null)
            setBackButton(getPositionBackButton(), parent);
    }

    public void show(Player player) {
        this.show(player, this.title);
    }

    public void show(Player player, String title) {
        this.viewer = player;
        InventoryDrawer drawer = InventoryDrawer.of(this.size, title);
        this.drawMenu().forEach(drawer::setItem);
        this.handlePreDisplay(drawer);
        this.debugSlotNumber(drawer);
        Bukkit.getPluginManager().callEvent(new MenuOpenEvent(this, drawer, player));

        Menu previousMenu = getMenu(player);
        if (previousMenu != null)
            player.setMetadata(MENU_TAG_PREVIOUS, new FixedMetadataValue(MurmelPlugin.getInstance(), previousMenu));
        new BukkitRunnable() {

            @Override
            public void run() {
                Menu.this.handleDisplay(drawer, player);
                player.setMetadata(MENU_TAG_CURRENT, new FixedMetadataValue(MurmelPlugin.getInstance(), Menu.this));
                Menu.this.opened = true;
                Menu.this.handlePostDisplay(drawer);
            }
        }.runTaskLater(MurmelPlugin.getInstance(), 1L);
    }

    private static Menu getMenu(Player player, String tag) {
        if (player.hasMetadata(tag)) {
            Menu menu = (Menu) player.getMetadata(tag).getFirst().value();
            if (menu == null)
                throw new NullPointerException("Menu missing from " + player.getName() + "'s metadata '" + tag + "' tag!");
            return menu;
        }
        return null;
    }

    public static Menu getMenu(Player player) {
        return getMenu(player, MENU_TAG_CURRENT);
    }

    public static Menu getPreviousMenu(Player player) {
        return getMenu(player, MENU_TAG_PREVIOUS);
    }

    public static Menu getLastClosedMenu(Player player) {
        return getMenu(player, MENU_TAG_LAST_CLOSED);
    }

    public void registerButton(int slot, Button button) {
        this.buttons.put(slot, button);
    }

    protected Button getButton(int slot) {
        return this.buttons.get(slot);
    }

    protected void handlePreDisplay(InventoryDrawer drawer) {
    }

    protected void handleDisplay(InventoryDrawer drawer, Player player) {
        drawer.show(player);
    }

    protected void handlePostDisplay(Player player) {
    }

    protected void handlePostDisplay(InventoryDrawer drawer) {
    }

    public void restartMenu() {
        this.restartMenu(null);
    }

    public void restartMenu(String title) {
        this.restartMenu(title, true);
    }

    public void restartMenu(String title, boolean menuClose) {
        Player player = this.viewer;
        if (player == null) throw new NullPointerException("Player is null");
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (!(inventory.getType() == InventoryType.CHEST))
            throw new NullPointerException(player.getName() + "'s inventory closed in the meanwhile (now == " + inventory.getType() + ").");

        if (menuClose) this.handleMenuClean(inventory);
        this.handleRestart();

        ItemStack[] contents = inventory.getContents();
        Map<Integer, ItemStack> newContents = this.drawMenu();

        for (int i = 0; i < contents.length; i++)
            contents[i] = newContents.get(i);
        inventory.setContents(contents);

        if (title != null)
            show(player, title);
        else show(player);
    }

    public void handleRestart() {
    }

    private Map<Integer, ItemStack> drawMenu() {
        final Map<Integer, Button> drawButtons = new HashMap<>(this.buttons);
        this.buttons.clear();
        final Map<Integer, ItemStack> items = new HashMap<>();

        for (int slot = 0; slot < this.size; slot++) {
            ItemStack item = this.getItemAt(slot);
            if (item != null && item.getType() == Material.AIR) item = null;
            items.put(slot, item);
        }

        for (final Map.Entry<Integer, Button> entry : drawButtons.entrySet()) {
            final int slot = entry.getKey();
            final Button button = entry.getValue();
            buttons.put(slot, button);
        }
        drawButtons.clear();

        if (this.getInfo() != null) {
            if (infoSlot == -1) infoSlot = getDefaultInfoSlot();
            Button infoButton = InfoButton.createInfoButton(getInfoSlot(), this.getInfo());
            items.put(infoButton.getSlot(), infoButton.getIcon());
        }
        if (backButton != null) items.put(backButton.getSlot(), backButton.getIcon());
        return items;
    }

    public ItemStack getItemAt(int slot) {
        if (backButton != null && slot == backButton.getSlot())
            return backButton.getIcon();
        return buildMenuPanel();
    }

    private ItemStack buildMenuPanel() {
        if (placeholder == null) return null;
        ItemStack item = new ItemStack(placeholder);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(""));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(JavaPlugin.getPlugin(MurmelPlugin.class), "menu"), PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public Menu getParent() {
        return parent;
    }

    public String getTitle() {
        return title;
    }

    protected void setTitle(String title) {
        this.title = title;
        if (this.viewer != null && this.opened) viewer.updateInventory();
    }

    public int getSize() {
        return size;
    }

    protected void setSize(int size) {
        this.size = size;
    }

    public Player getViewer() {
        return viewer;
    }

    protected void setViewer(Player viewer) {
        this.viewer = viewer;
    }

    protected final Inventory getInventory() {
        return this.viewer.getOpenInventory().getTopInventory();
    }

    protected final ItemStack[] getContents(int from, int to) {
        ItemStack[] contents = this.getInventory().getContents();
        ItemStack[] copy = new ItemStack[contents.length];

        for (int i = from; i < copy.length; i++) {
            ItemStack item = contents[i];
            copy[i] = item != null ? item.clone() : null;
        }

        return Arrays.copyOfRange(copy, from, to);
    }

    protected void setItem(int slot, ItemStack item) {
        this.getInventory().setItem(slot, item);
    }

    private void debugSlotNumber(InventoryDrawer drawer) {
        if (this.slotNumberVisible)
            for (int slot = 0; slot < drawer.getSize(); slot++) {
                final ItemStack item = drawer.getItem(slot);
                if (item == null) {
                    ItemStack newItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
                    ItemMeta meta = newItem.getItemMeta();
                    meta.displayName(MiniMessage.miniMessage().deserialize("Slot " + slot));
                    newItem.setItemMeta(meta);
                    drawer.setItem(slot, newItem);
                }
            }
    }

    protected void setSlotNumberVisible() {
        this.slotNumberVisible = true;
    }

    protected boolean isActionAllowed(MenuClickLocation location, int slot, ItemStack clicked, ItemStack cursor, InventoryAction action) {
        return isActionAllowed(location, slot, clicked, cursor);
    }

    protected boolean isActionAllowed(MenuClickLocation location, int slot, ItemStack clicked, ItemStack cursor) {
        return false;
    }

    protected boolean isAllowShiftClick() {
        return allowShiftClick;
    }

    protected boolean isAllowShiftClick(int slot) {
        return false;
    }

    protected void setAllowShiftClick(boolean allowShiftClick) {
        this.allowShiftClick = allowShiftClick;
    }

    public int getPositionBackButton() {
        return this.size - 1;
    }

    public int getDefaultInfoSlot() {
        return this.size - 9;
    }

    public int getInfoSlot() {
        return infoSlot;
    }

    public void setInfoSlot(int infoSlot) {
        this.infoSlot = infoSlot;
    }

    protected String[] getInfo() {
        return null;
    }

    protected void setBackButton(int slot, Menu parent) {
        this.backButton = new BackButton(slot, parent);
        this.buttons.put(slot, this.backButton);
    }

    public Material getPlaceholder() {
        return placeholder;
    }

    protected void setPlaceholder(Material placeholder) {
        this.placeholder = placeholder;
    }

    protected void handleMenuClick(Player player, int slot, InventoryAction action, ClickType clickType,
                                   ItemStack cursor, ItemStack clicked, boolean cancelled) {
        this.handleMenuClick(player, slot, clicked);
    }

    protected void handleMenuClick(Player player, int slot, ItemStack clicked) {
    }

    protected void handleButtonClick(Player player, int slot, InventoryAction action, ClickType clickType, Button button) {
        button.click(player, this, clickType);
    }

    protected void handleMenuClean(Inventory inventory) {
        this.viewer.removeMetadata(MENU_TAG_CURRENT, MurmelPlugin.getInstance());
        this.viewer.setMetadata(MENU_TAG_LAST_CLOSED, new FixedMetadataValue(MurmelPlugin.getInstance(), this));
        this.opened = false;
        this.handleMenuClose(this.viewer, inventory);
        Bukkit.getPluginManager().callEvent(new MenuCloseEvent(this, inventory, this.viewer));
    }

    protected void handleMenuClose(Player player, Inventory inventory) {
    }

    protected void animate(int periodTicks, MenuRunnable task) {
        this.wrapAnimation(task).runTaskTimer(MurmelPlugin.getInstance(), 2L, periodTicks);
    }

    protected void animateAsync(int periodTicks, MenuRunnable task) {
        this.wrapAnimation(task).runTaskTimerAsynchronously(MurmelPlugin.getInstance(), 2L, periodTicks);
    }

    private BukkitRunnable wrapAnimation(MenuRunnable task) {
        return new BukkitRunnable() {
            boolean cancelled = false;

            @Override
            public void run() {
                if (!Menu.this.opened) {
                    if (!this.cancelled) this.cancel();
                    return;
                }

                try {
                    task.run();
                } catch (RuntimeException e) {
                    this.cancelled = true;
                    this.cancel();
                }
            }
        };
    }

    @FunctionalInterface
    public interface MenuRunnable extends Runnable {
        default void cancel() {
            throw new RuntimeException();
        }
    }
}
