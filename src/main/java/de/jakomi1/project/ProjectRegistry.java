package de.jakomi1.project;

import de.jakomi1.database.Table;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public abstract class ProjectRegistry {
    public abstract List<Registerable> getRegisterable();
}
