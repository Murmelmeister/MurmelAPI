package de.murmelmeister.murmelapi;

import de.murmelmeister.murmelapi.menu.MenuListener;
import org.bukkit.plugin.java.JavaPlugin;

public class MurmelPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
    }

    public static MurmelPlugin getInstance() {
        return getPlugin(MurmelPlugin.class);
    }
}
