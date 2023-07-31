package me.vacefronvevo.dogwhisperer;

import me.gamercoder215.mobchip.ai.goal.WrappedPathfinder;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class DogWhisperer extends JavaPlugin {

    private final Map<UUID, Set<UUID>> _playerDogMap = new HashMap<>();
    private final Map<UUID, Set<WrappedPathfinder>> _dogPathfinderMap = new HashMap<>();
    private final Set<Wolf> _unloadedDogs = new HashSet<>();
    private final Set<UUID> _wolvesToRemove = new HashSet<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new EventService(this), this);
        getServer().getPluginCommand("dogwhisperer").setExecutor(new DogWhispererHelp());
        getServer().getPluginCommand("summondogs").setExecutor(new SummonDogs(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public Map<UUID, Set<UUID>> getPlayerDogMap() {
        return _playerDogMap;
    }
    public Map<UUID, Set<WrappedPathfinder>> getDogPathfinderMap() {
        return _dogPathfinderMap;
    }

    public Set<Wolf> getUnloadedDogs() {
        return _unloadedDogs;
    }

    public Set<UUID> getWolvesToRemove() {
        return _wolvesToRemove;
    }
}
