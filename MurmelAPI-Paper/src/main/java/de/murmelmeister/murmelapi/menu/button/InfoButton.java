package de.murmelmeister.murmelapi.menu.button;

import de.murmelmeister.murmelapi.MurmelPlugin;
import de.murmelmeister.murmelapi.menu.Menu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class InfoButton {
    public static final NamespacedKey KEY = new NamespacedKey(MurmelPlugin.getInstance(), "MenuItems");

    public static Button createInfoButton(int slot, String... description) {
        ItemStack itemStack = new ItemStack(Material.NETHER_STAR);
        ItemMeta itemMeta = itemStack.getItemMeta();

        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.set(KEY, PersistentDataType.BOOLEAN, true);

        itemMeta.displayName(MiniMessage.miniMessage().deserialize("<#00cc88>Info Button"));
        itemMeta.addEnchant(Enchantment.MENDING, 1, true);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(" "));
        for (String line : description)
            lore.add(MiniMessage.miniMessage().deserialize(line));
        itemMeta.lore(lore);

        for (ItemFlag flag : ItemFlag.values())
            itemMeta.addItemFlags(flag);

        itemStack.setItemMeta(itemMeta);
        return new Button(slot) {
            @Override
            public ItemStack getIcon() {
                return itemStack;
            }

            @Override
            public void click(Player player, Menu menu, ClickType clickType) {

            }
        };
    }
}
