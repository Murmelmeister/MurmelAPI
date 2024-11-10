package de.murmelmeister.murmelapi.menu.button;

import de.murmelmeister.murmelapi.MurmelPlugin;
import de.murmelmeister.murmelapi.menu.Menu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BackButton extends Button {
    private final NamespacedKey key = new NamespacedKey(JavaPlugin.getPlugin(MurmelPlugin.class), "BackButton");

    private final Material material = Material.PURPLE_DYE;
    private final String title = "<#880088>Return Back";
    private final List<String> lore = Arrays.asList("", "<#858585>Return back.");
    private final Menu parent;

    public BackButton(int slot, Menu parent) {
        super(slot);
        this.parent = parent;
    }

    @Override
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(key, PersistentDataType.BOOLEAN, true);

        meta.displayName(MiniMessage.miniMessage().deserialize(title));
        List<Component> lores = new ArrayList<>();
        for (String line : lore)
            lores.add(MiniMessage.miniMessage().deserialize(line));
        meta.lore(lores);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void click(Player player, Menu menu, ClickType clickType) {
        this.parent.show(player);
    }

    public NamespacedKey getKey() {
        return key;
    }

    public Material getMaterial() {
        return material;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getLore() {
        return lore;
    }
}
