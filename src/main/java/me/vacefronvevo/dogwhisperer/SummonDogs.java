package me.vacefronvevo.dogwhisperer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SummonDogs implements CommandExecutor {
    private final Map<UUID, Set<UUID>> _playerDogMap;
    private final Set<Wolf> _unloadedDogs;
    private final Set<UUID> _wolvesToRemove;
    private final DogWhisperer _dogWhisperer;

    public SummonDogs(DogWhisperer dogWhisperer) {

        _dogWhisperer = dogWhisperer;
        _playerDogMap = dogWhisperer.getPlayerDogMap();
        _unloadedDogs = dogWhisperer.getUnloadedDogs();
        _wolvesToRemove = dogWhisperer.getWolvesToRemove();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player))
            return false;

        var wolves = _playerDogMap.get(player.getUniqueId());
        var wolvesNotTeleported = new HashSet<UUID>();
        var wolvesTeleportedCount = 0;

        if (wolves != null && wolves.size() > 0) {

            for (var wolfId : wolves.stream().toList()) {
                var wolf = (Wolf)Bukkit.getEntity(wolfId);
                if (wolf == null || wolf.isDead()) {
                    if (_unloadedDogs.stream().noneMatch(x -> x.getUniqueId() == wolfId))
                        wolvesNotTeleported.add(wolfId);
                    continue;
                }

                wolf.teleport(player);
                wolvesTeleportedCount++;
            }

            wolves.removeIf(wolvesNotTeleported::contains);

            var world = player.getWorld();

            for (var wolf : _unloadedDogs) {
                if (wolves.contains(wolf.getUniqueId())) {
                    var newWolf = (Wolf)world.spawnEntity(player.getLocation(), EntityType.WOLF);
                    copyToNewWolf(wolf, newWolf);
                    wolvesTeleportedCount++;
                }
            }
        }

        if (wolvesTeleportedCount < 1) {
            player.sendMessage(ChatColor.RED + "You have no dogs in your party");
            return true;
        }

        player.sendMessage("Teleporting " + ChatColor.GOLD + wolves.size() + ChatColor.WHITE + (wolves.size() == 1 ? " dog" : " dogs") + " to your location");

        return true;
    }

    private void copyToNewWolf(Wolf from, Wolf to) {
        to.setTamed(from.isTamed());
        to.setOwner(from.getOwner());
        to.setCollarColor(from.getCollarColor());
        to.setSitting(from.isSitting());
        to.setCustomName(from.getCustomName());
        to.setHealth(from.getHealth());
        to.setAge(from.getAge());

        if (from.getOwner() != null) {
            var ownerWolves = _playerDogMap.get(from.getOwner().getUniqueId());
            ownerWolves.remove(from.getUniqueId());
            ownerWolves.add(to.getUniqueId());
            _wolvesToRemove.add(from.getUniqueId());
            Utils.handleDogPathfinding(_dogWhisperer, to, true);
        }
    }
}
