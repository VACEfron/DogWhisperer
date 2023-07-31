package me.vacefronvevo.dogwhisperer;

import me.gamercoder215.mobchip.bukkit.BukkitBrain;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class EventService implements Listener {
    private final int _maxDistance = 50;
    private final Map<UUID, LocalDateTime> _playerAndRightClickLastHandled = new HashMap<>();
    private final Map<UUID, Set<UUID>> _playerDogMap;
    private final Set<Wolf> _deloadedDogs;
    private final Set<UUID> _wolvesToRemove;
    private final DogWhisperer _dogWhisperer;

    public EventService(DogWhisperer dogWhisperer) {
        _dogWhisperer = dogWhisperer;
        _playerDogMap = dogWhisperer.getPlayerDogMap();
        _deloadedDogs = dogWhisperer.getUnloadedDogs();
        _wolvesToRemove = dogWhisperer.getWolvesToRemove();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        var itemHeld = event.getItem();
        var player = event.getPlayer();
        var action = event.getAction();

        if (itemHeld == null || itemHeld.getType() != Material.STICK)
            return;

        if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR)
            handleLeftClick(player);
        else if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)
            handleRightClick(player);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Wolf wolf) || !Utils.isWolfOwnedByPlayer(wolf, player))
            return;

        var itemHeld = player.getInventory().getItemInMainHand();
        if (itemHeld.getType() != Material.STICK)
            return;

        addOrRemoveWolf(wolf, player);
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        var entity = event.getEntity();

        var isInList = false;
        for (var i : _playerDogMap.values()) {
            for (var j : i) {
                if (entity.getUniqueId() == j) {
                    isInList = true;
                }
            }
        }

        if (isInList)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
            var player = event.getPlayer();

            if (player.getGameMode() != GameMode.CREATIVE) {
                var location = player.getEyeLocation();
                var world = location.getWorld();
                if (world == null)
                    return;

                var result = location.getWorld().rayTrace(location, location.getDirection(), 4.5D, FluidCollisionMode.NEVER, false, 0.1, entity -> entity != player && player.canSee(entity));

                if (result != null && result.getHitEntity() != null && result.getHitPosition().distance(location.toVector()) > 3) {
                    var itemHeld = player.getInventory().getItemInMainHand();
                    if (itemHeld.getType() != Material.STICK)
                        return;

                    handleLeftClick(player);
                }
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        var entities = event.getChunk().getEntities();
        var values = _playerDogMap.values();

        for (var entity : entities) {
            var entityId = entity.getUniqueId();
            for (var wolfIds : values) {
                if (wolfIds.contains(entityId))
                    _deloadedDogs.add((Wolf)entity);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        var entities = event.getChunk().getEntities();
        var values = _playerDogMap.values();

        for (var entity : entities) {
            var entityId = entity.getUniqueId();
            for (var wolfIds : values) {
                if (wolfIds.contains(entityId)) {
                    _deloadedDogs.removeIf(x -> x.getUniqueId() == entityId);
                }
                if (_wolvesToRemove.contains(entityId)) {
                    entity.remove();
                    _wolvesToRemove.remove(entityId);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf))
            return;

        for (var entry : _playerDogMap.entrySet()) {
            for (var wolfId : entry.getValue()) {
                if (wolf.getUniqueId() == wolfId) {
                    _playerDogMap.get(entry.getKey()).remove(wolfId);
                }
            }
        }
    }

    private void handleLeftClick(Player player) {

        var world = player.getWorld();
        var wolves = _playerDogMap.getOrDefault(player.getUniqueId(), null);

        var rayTraceEntities = world.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), _maxDistance,
                entity -> !entity.getUniqueId().equals(player.getUniqueId()));

        if (rayTraceEntities != null && rayTraceEntities.getHitEntity() != null
                && rayTraceEntities.getHitEntity() instanceof LivingEntity livingEntity) {

            if (livingEntity instanceof Wolf wolf && Utils.isWolfOwnedByPlayer(wolf, player)) {
                addOrRemoveWolf(wolf, player);
                return;
            }

            setWolvesTarget(wolves, livingEntity);
            return;
        }

        if (wolves == null)
            return;

        var block = player.getTargetBlockExact(_maxDistance);
        if (block == null)
            return;

        var location = block.getLocation();

        walkWolves(wolves, location);
    }

    private void handleRightClick(Player player) {
        if (!LocalDateTime.now().isAfter(_playerAndRightClickLastHandled.getOrDefault(player.getUniqueId(), LocalDateTime.now().minusMinutes(1)).plus(500, ChronoUnit.MILLIS)))
            return;

        _playerAndRightClickLastHandled.put(player.getUniqueId(), LocalDateTime.now());

        var location = player.getLocation();
        var wolves = _playerDogMap.getOrDefault(player.getUniqueId(), null);

        if (wolves == null)
            return;

        walkWolves(wolves, location);
    }

    private void addOrRemoveWolf(Wolf wolf, Player player) {
        var playerId = player.getUniqueId();
        var wolfId = wolf.getUniqueId();
        boolean isWolfAdded;

        if (_playerDogMap.containsKey(playerId)) {
            var set = _playerDogMap.get(playerId);
            if (!set.contains(wolfId)) {
                set.add(wolf.getUniqueId());
                isWolfAdded = true;
            } else {
                set.remove(wolfId);
                isWolfAdded = false;
            }
        }
        else {
            var set = new HashSet<UUID>();
            set.add(wolf.getUniqueId());
            _playerDogMap.put(playerId, set);
            isWolfAdded = true;
        }

        wolf.setSitting(false);

        var text = isWolfAdded ? "Added" : "Released";
        var text2 = isWolfAdded ? "to" : "from";

        if (wolf.getCustomName() == null)
            player.sendMessage((isWolfAdded ? ChatColor.GREEN : ChatColor.RED) + text + ChatColor.WHITE + " " + text2 + " the party");
        else
            player.sendMessage((isWolfAdded ? ChatColor.GREEN : ChatColor.RED) + text + " " + ChatColor.GOLD + wolf.getCustomName() + ChatColor.WHITE + " " + text2 + " the party");

        Utils.handleDogPathfinding(_dogWhisperer, wolf, isWolfAdded);
    }

    private void walkWolves(Set<UUID> wolves, Location location) {
        for (var wolfId : wolves) {
            var wolf = getWolf(wolfId);
            if (wolf == null || wolf.isSitting())
                continue;

            var brain = BukkitBrain.getBrain(wolf);

            if (brain == null)
                continue;

            wolf.setTarget(null);
            brain.getController().moveTo(location, 1.2);
        }
    }

    private void setWolvesTarget(Set<UUID> wolves, LivingEntity entity) {
        if (wolves == null)
            return;

        for (var wolfId : wolves) {
            var wolf = getWolf(wolfId);
            var owner = wolf.getOwner();
            if (entity instanceof Wolf targetWolf && targetWolf.getOwner() != null || owner != null && entity.getUniqueId() == owner.getUniqueId())
                return;
            wolf.setTarget(entity);
        }
    }

    private Wolf getWolf(UUID id) {
        return (Wolf)Bukkit.getEntity(id);
    }
}
