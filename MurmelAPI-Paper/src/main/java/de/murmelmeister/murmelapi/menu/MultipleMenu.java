package de.murmelmeister.murmelapi.menu;

import de.murmelmeister.murmelapi.MurmelPlugin;
import de.murmelmeister.murmelapi.menu.button.Button;
import de.murmelmeister.murmelapi.menu.model.InventoryDrawer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public abstract class MultipleMenu<T> extends Menu {
    private Material activePageButton = Material.LIME_DYE;
    private Material inactivePageButton = Material.GRAY_DYE;

    private final List<Integer> slots;
    private final Iterable<T> items;
    private final Integer manualPageSize;
    private final Map<Integer, List<T>> pages = new HashMap<>();
    private int currentPage = 1;
    private final boolean mainMenu;

    private Button nextButton;
    private Button previousButton;

    public final NamespacedKey key = new NamespacedKey(MurmelPlugin.getInstance(), "MenuItems");

    @SafeVarargs
    public MultipleMenu(Menu parent, boolean mainMenu, T... items) {
        this(parent, mainMenu, Arrays.asList(items));
    }

    public MultipleMenu(Menu parent, boolean mainMenu, List<T> items) {
        this(null, parent, new ArrayList<>(), items, mainMenu);
    }

    private MultipleMenu(Integer pageSize, Menu parent, List<Integer> slots, Iterable<T> items, boolean mainMenu) {
        super(parent);
        this.slots = slots;
        this.items = items;
        this.manualPageSize = pageSize;
        this.mainMenu = mainMenu;

        this.calculatePages();
        this.setButtons();
    }

    protected abstract ItemStack convertToItemStack(T item);

    protected abstract void handlePageClick(Player player, T item, ClickType clickType);

    private void calculatePages() {
        int items = getItemAmount(this.items);
        int autoPageSize;

        if (this.slots.isEmpty()) {
            autoPageSize = this.manualPageSize != null ? this.manualPageSize : getDynamicPageSize(items);
            for (int i = 0; i < autoPageSize; i++) this.slots.add(i);
            this.setSize(mainMenu ? autoPageSize : autoPageSize + 9);
        } else autoPageSize = this.slots.size();

        this.pages.clear();
        this.pages.putAll(fillPages(autoPageSize, this.items));
    }

    private void setButtons() {
        if (!mainMenu) setBackButton(getPositionBackButton(), getParent());
        if (this.hasMorePages()) {
            this.previousButton = fromPreviousButton();
            this.nextButton = fromNextButton();
        }
    }

    private Button fromPreviousButton() {
        return new Button(getPositionPreviousButton(), this) {
            private final boolean canGo = getCurrentPage() > 1;

            @Override
            public ItemStack getIcon() {
                int previousPage = getCurrentPage() - 1;
                ItemStack item = new ItemStack(canGo ? activePageButton : inactivePageButton);
                ItemMeta meta = item.getItemMeta();

                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(key, PersistentDataType.BOOLEAN, true);

                meta.displayName(MiniMessage.miniMessage().deserialize(previousPage == 0 ? "<#b68200>First Page" : "<#e6c200><< Page " + previousPage));
                item.setItemMeta(meta);
                return item;
            }

            @Override
            public void click(Player player, Menu menu, ClickType clickType) {
                if (canGo) setCurrentPage(Math.min(Math.max(getCurrentPage() - 1, 1), getPages().size()));
            }
        };
    }

    private Button fromNextButton() {
        return new Button(getPositionNextButton(), this) {
            private final boolean canGo = getCurrentPage() < getPages().size();

            @Override
            public ItemStack getIcon() {
                boolean lastPage = getCurrentPage() == getPages().size();
                ItemStack item = new ItemStack(canGo ? activePageButton : inactivePageButton);
                ItemMeta meta = item.getItemMeta();

                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(key, PersistentDataType.BOOLEAN, true);

                meta.displayName(MiniMessage.miniMessage().deserialize(lastPage ? "<#b68200>Last Page" : "<#e6c200>Page " + (getCurrentPage() + 1) + " >>"));
                item.setItemMeta(meta);
                return item;
            }

            @Override
            public void click(Player player, Menu menu, ClickType clickType) {
                if (canGo) setCurrentPage(Math.min(Math.max(getCurrentPage() + 1, 1), getPages().size()));
            }
        };
    }

    protected boolean hasMorePages() {
        return this.pages.size() > 1;
    }

    private List<T> getCurrentPageItems() {
        return this.pages.get(currentPage - 1);
    }

    protected String getTitleWithPageNumbers() {
        boolean canAddNumbers = this.hasMorePages();
        return this.getTitle() + (canAddNumbers ? " <#858585>" + this.currentPage + "/" + this.pages.size() + "</#858585>" : "");
    }

    private void updatePage() {
        this.setButtons();
        this.restartMenu(this.getTitleWithPageNumbers());
    }

    @Override
    public void handleRestart() {
        this.calculatePages();
    }

    @Override
    protected void handlePreDisplay(InventoryDrawer drawer) {
        drawer.setTitle(this.getTitleWithPageNumbers());
        this.calculatePages();
        this.handlePostDisplay(drawer);
    }

    @Override
    public ItemStack getItemAt(int slot) {
        if (this.hasMorePages()) {
            if (slot == this.getPositionPreviousButton()) return this.previousButton.getIcon();
            if (slot == this.getPositionNextButton()) return this.nextButton.getIcon();
        }

        if (this.slots.contains(slot) && this.slots.indexOf(slot) < this.getCurrentPageItems().size()) {
            final T object = this.getCurrentPageItems().get(this.slots.indexOf(slot));
            if (object != null) return this.convertToItemStack(object);
        }
        return super.getItemAt(slot);
    }

    @Override
    protected void handleMenuClick(Player player, int slot, InventoryAction action, ClickType clickType, ItemStack cursor, ItemStack clicked, boolean cancelled) {
        if (this.slots.contains(slot) && this.slots.indexOf(slot) < this.getCurrentPageItems().size()) {
            T object = this.getCurrentPageItems().get(this.slots.indexOf(slot));
            if (object != null) {
                InventoryType type = player.getOpenInventory().getTopInventory().getType();
                this.handlePageClick(player, object, clickType);
                if (type == player.getOpenInventory().getTopInventory().getType()) {
                    Inventory topInventory = player.getOpenInventory().getTopInventory();
                    topInventory.setItem(slot, this.getItemAt(slot));
                }
            }
        }
    }

    @Override
    protected void handleButtonClick(Player player, int slot, InventoryAction action, ClickType clickType, Button button) {
        super.handleButtonClick(player, slot, action, clickType, button);
    }

    @Override
    protected void handleMenuClick(Player player, int slot, ItemStack clicked) {
        throw new IllegalArgumentException("Simplest click unsupported");
    }

    private int getDynamicPageSize(int items) {
        if (items <= 9) return 9;
        else if (items <= 18) return 18;
        else if (items <= 27) return 27;
        else if (items <= 36) return 36;
        else return 45;
    }

    private <I> Map<Integer, List<I>> fillPages(int pageSize, Iterable<I> items) {
        List<I> allItems = toList(items);
        Map<Integer, List<I>> pages = new HashMap<>();

        for (int i = 0; i <= (allItems.size() - 1) / pageSize; i++) {
            int fromIndex = i * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, allItems.size());
            List<I> pageItems = allItems.subList(fromIndex, toIndex);
            if (!pageItems.isEmpty()) pages.put(i, pageItems);
        }
        return pages;
    }

    private <I> List<I> toList(Iterable<I> iterable) {
        List<I> list = new ArrayList<>();
        if (iterable != null)
            iterable.forEach(i -> {
                if (i != null) list.add(i);
            });
        return list;
    }

    private int getItemAmount(Iterable<T> pages) {
        int amount = 0;
        for (T ignored : pages) amount++;
        return amount;
    }

    protected int getPositionPreviousButton() {
        return this.getPositionBackButton() - 5;
    }

    protected int getPositionNextButton() {
        return this.getPositionBackButton() - 3;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    protected void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
        this.updatePage();
    }

    public Map<Integer, List<T>> getPages() {
        return pages;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public Material getActivePageButton() {
        return activePageButton;
    }

    public Material getInactivePageButton() {
        return inactivePageButton;
    }

    protected void setActivePageButton(Material activePageButton) {
        this.activePageButton = activePageButton;
    }

    protected void setInactivePageButton(Material inactivePageButton) {
        this.inactivePageButton = inactivePageButton;
    }
}
