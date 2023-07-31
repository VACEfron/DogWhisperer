package me.vacefronvevo.dogwhisperer;

import me.gamercoder215.mobchip.ai.goal.Pathfinder;
import me.gamercoder215.mobchip.ai.goal.PathfinderFollowOwner;
import me.gamercoder215.mobchip.ai.goal.WrappedPathfinder;
import me.gamercoder215.mobchip.bukkit.BukkitBrain;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

public class Utils {

    private static final BukkitScheduler _scheduler = Bukkit.getScheduler();

    public static Boolean isWolfOwnedByPlayer(Wolf wolf, Player player) {
        var owner = wolf.getOwner();
        return owner != null && owner.getUniqueId() == player.getUniqueId();
    }

    public static void handleDogPathfinding(DogWhisperer dogWhisperer, Wolf wolf, boolean isWolfAdded) {
        _scheduler.runTaskLater(dogWhisperer, () -> {
            if (isWolfAdded)
                removeFollowOwnerPathfinder(dogWhisperer.getDogPathfinderMap(), wolf);
            else
                addFollowOwnerPathfinder(dogWhisperer.getDogPathfinderMap(), wolf);
        }, 20L);
    }

    private static void removeFollowOwnerPathfinder(Map<UUID, Set<WrappedPathfinder>> dogPathfinderMap, Wolf wolf) {
        if (wolf == null)
            return;

        var brain = BukkitBrain.getBrain(wolf);
        if (brain == null)
            return;

        var goalAi = brain.getGoalAI();

        var optionalPathfinder = goalAi.stream().filter(x -> x.getPathfinder() instanceof PathfinderFollowOwner).findFirst();
        if (optionalPathfinder.isEmpty())
            return;

        var set = new HashSet<>(goalAi);
        dogPathfinderMap.put(wolf.getUniqueId(), set);

        var pathfinder = optionalPathfinder.get();
        goalAi.remove(pathfinder);
    }

    private static void addFollowOwnerPathfinder(Map<UUID, Set<WrappedPathfinder>> dogPathfinderMap, Wolf wolf) {
        if (wolf == null)
            return;

        var brain = BukkitBrain.getBrain(wolf);
        if (brain == null || wolf.isSitting())
            return;

        var goalAi = brain.getGoalAI();
        var pathfinders = dogPathfinderMap.get(wolf.getUniqueId());

        if (pathfinders == null)
            return;

        var map = new HashMap<Pathfinder, Integer>();
        for (var x : pathfinders) {
            map.put(x.getPathfinder(), x.getPriority());
        }
        goalAi.clear();
        goalAi.putAll(map);
    }
}
