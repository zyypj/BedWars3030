package com.tomkeuper.bedwars.listeners;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.arena.LastHit;
import com.tomkeuper.bedwars.arena.team.BedWarsTeam;
import com.tomkeuper.bedwars.listeners.chat.ChatFormatting;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FireballListener implements Listener {

    private static final double MIN_LENGTH_SQUARED = 1.0E-4D;
    private static final double ENEMY_VERTICAL_BOOST = 1.5D;
    private static final long FIREBALL_FALL_DAMAGE_WINDOW_MILLIS = 8_000L;
    private static final Map<UUID, Long> RECENT_FIREBALL_KNOCKBACKS = new ConcurrentHashMap<>();
    private static volatile double fireballFallDamageReduction = 0.0D;

    private static FireballListener instance;

    private List<String> explosionProofMaterials;
    private double fireballExplosionSize, fireballHorizontalSelf, fireballHorizontalOthers, fireballVerticalSelf, fireballVerticalOthers;
    private double fireballJumpTolerance;
    private double damageSelf, damageEnemy, damageTeammates;
    private double fireballSpeedMultiplier, fireballCooldown;
    private boolean fireballMakeFire;

    public FireballListener() {
        instance = this;
        loadSettings();
    }

    /**
     * Read the fireball settings again, after the main config was reloaded.
     */
    public static void reloadSettings() {
        if (instance != null) instance.loadSettings();
    }

    private void loadSettings() {
        YamlConfiguration config = BedWars.config.getYml();
        explosionProofMaterials = config.getList(ConfigPath.GENERAL_FIREBALL_EXPLOSION_PROOF_BLOCKS).stream().map(Object::toString).collect(Collectors.toList());
        fireballExplosionSize = config.getDouble(ConfigPath.GENERAL_FIREBALL_EXPLOSION_SIZE);
        fireballMakeFire = config.getBoolean(ConfigPath.GENERAL_FIREBALL_MAKE_FIRE);
        fireballHorizontalSelf = Math.abs(config.getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL_SELF));
        fireballHorizontalOthers = Math.abs(config.getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_HORIZONTAL_OTHERS));
        fireballVerticalSelf = config.getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL_SELF);
        fireballVerticalOthers = config.getDouble(ConfigPath.GENERAL_FIREBALL_KNOCKBACK_VERTICAL_OTHERS);
        fireballJumpTolerance = config.getDouble(ConfigPath.GENERAL_FIREBALL_JUMP_TOLERANCE);
        damageSelf = config.getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_SELF);
        damageEnemy = config.getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_ENEMY);
        damageTeammates = config.getDouble(ConfigPath.GENERAL_FIREBALL_DAMAGE_TEAMMATES);
        fireballSpeedMultiplier = config.getDouble(ConfigPath.GENERAL_FIREBALL_SPEED_MULTIPLIER);
        fireballCooldown = config.getDouble(ConfigPath.GENERAL_FIREBALL_COOLDOWN);
        fireballFallDamageReduction = config.getDouble(ConfigPath.GENERAL_FIREBALL_FALL_DAMAGE_REDUCTION);
    }

    @EventHandler
    public void onFireballInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack handItem = e.getItem();
        Action action = e.getAction();

        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR || handItem == null) return;

        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || arena.getStatus() != GameState.playing || handItem.getType() != BedWars.nms.materialFireball()) return;

        e.setCancelled(true);

        long cooldown = (long) (fireballCooldown * 1000);
        long timeDifference = System.currentTimeMillis() - arena.getFireballCooldowns().getOrDefault(player.getUniqueId(), 0L);
        if (timeDifference <= cooldown) {
            if (fireballCooldown >= 1.0) {
                String msg = Language.getMsg(player, Messages.ARENA_FIREBALL_COOLDOWN)
                        .replace("%bw_cooldown%", String.valueOf((cooldown - timeDifference)/1000));
                BedWars.plugin.adventure().player(player).sendMessage(ChatFormatting.parseLegacyMini(msg));
            }
            return;
        }

        arena.getFireballCooldowns().put(player.getUniqueId(), System.currentTimeMillis());
        Fireball fireball = player.launchProjectile(Fireball.class);
        Vector direction = player.getEyeLocation().getDirection();
        fireball = BedWars.nms.setFireballDirection(fireball, direction);
        fireball.setVelocity(fireball.getDirection().multiply(fireballSpeedMultiplier));
        fireball.setYield((float) fireballExplosionSize);
        fireball.setMetadata("bw2023", new FixedMetadataValue(BedWars.plugin, "ceva"));
        BedWars.nms.minusAmount(player, handItem, 1);
    }

    @EventHandler
    public void fireballHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Fireball)) return;

        Location location = e.getEntity().getLocation();
        ProjectileSource projectileSource = e.getEntity().getShooter();
        if (!(projectileSource instanceof Player))  return;

        Player source = (Player) projectileSource;
        IArena arena = Arena.getArenaByPlayer(source);

        if (arena == null || arena.getStatus() != GameState.playing)  return;

        Vector vector = location.toVector();
        World world = location.getWorld();
        if (world == null)  return;

        Collection<Entity> nearbyEntities = world.getNearbyEntities(location, fireballExplosionSize, fireballExplosionSize, fireballExplosionSize);

        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Player)) continue;
            Player player = (Player) entity;

            if (!Arena.isInArena(player) || arena.isSpectator(player) || arena.isReSpawning(player)) continue;

            UUID playerUUID = player.getUniqueId();
            long respawnInvulnerability = BedWarsTeam.reSpawnInvulnerability.getOrDefault(playerUUID, 0L);

            if (respawnInvulnerability > System.currentTimeMillis()) continue;
            BedWarsTeam.reSpawnInvulnerability.remove(playerUUID);

            Vector blastDirection = blastDirection(player.getLocation().toVector(), vector);
            Vector launchVelocity = player.getUniqueId().equals(source.getUniqueId())
                    ? selfLaunch(player, blastDirection)
                    : enemyLaunch(blastDirection);

            // FIXED: Delay velocity application for newer versions to avoid being overridden
            final Player finalPlayer = player;
            Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
                try {
                    markFireballKnockback(finalPlayer);
                    finalPlayer.setVelocity(launchVelocity);
                } catch (IllegalArgumentException ignored) {}
            });

            LastHit lh = LastHit.getLastHit(player);
            if (lh != null) {
                lh.setDamager(source);
                lh.setTime(System.currentTimeMillis());
            } else  new LastHit(player, source, System.currentTimeMillis());

            if (player.equals(source)) {
                if (damageSelf > 0) player.damage(damageSelf);
            } else {
                ITeam playerTeam = arena.getTeam(player);
                ITeam sourceTeam = arena.getTeam(source);

                if (playerTeam != null && playerTeam.equals(sourceTeam)) damagePlayer(player, damageTeammates);
                else damagePlayer(player, damageEnemy);
            }
        }
    }

    @EventHandler
    public void onFireballExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Fireball)) return;

        ProjectileSource projectileSource = ((Fireball) event.getEntity()).getShooter();
        if (!(projectileSource instanceof Player)) return;

        Player source = (Player) projectileSource;
        IArena arena = Arena.getArenaByPlayer(source);

        if (arena == null || arena.getStatus() != GameState.playing)  return;

        Location explosionLocation = event.getLocation();
        World world = explosionLocation.getWorld();
        if (world == null) return;

        event.blockList().removeIf( block -> explosionProofMaterials.contains(block.getType().toString()));
    }

    private void damagePlayer(Player player, double damageTeammates) {
        if (damageTeammates > 0) {
            EntityDamageEvent damageEvent = new EntityDamageEvent(
                    player,
                    EntityDamageEvent.DamageCause.ENTITY_EXPLOSION,
                    new EnumMap<>(ImmutableMap.of(EntityDamageEvent.DamageModifier.BASE, damageTeammates)),
                    new EnumMap<>(ImmutableMap.of(EntityDamageEvent.DamageModifier.BASE, Functions.constant(damageTeammates)))
            );
            player.setLastDamageCause(damageEvent);
            player.damage(damageTeammates); // damage teammates
        }
    }

    @EventHandler
    public void fireballDirectHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Fireball) || !(e.getEntity() instanceof Player)) return;

        Player player = (Player) e.getEntity();
        if (!Arena.isInArena(player))  return;
        e.setCancelled(true);
    }

    @EventHandler
    public void fireballPrime(ExplosionPrimeEvent e) {
        if (!(e.getEntity() instanceof Fireball)) return;

        Fireball fireball = (Fireball) e.getEntity();
        ProjectileSource shooter = fireball.getShooter();

        if (!(shooter instanceof Player) || !Arena.isInArena((Player) shooter))  return;

        e.setFire(fireballMakeFire);
    }

    public static void markFireballKnockback(Player player) {
        RECENT_FIREBALL_KNOCKBACKS.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public static boolean consumeRecentFireballKnockback(Player player) {
        Long markedAt = RECENT_FIREBALL_KNOCKBACKS.remove(player.getUniqueId());
        if (markedAt == null) return false;
        return markedAt >= System.currentTimeMillis() - FIREBALL_FALL_DAMAGE_WINDOW_MILLIS;
    }

    public static double getFireballFallDamageReduction() {
        return fireballFallDamageReduction;
    }

    // A blast right on the player's position has no direction; normalizing it gives NaN and the
    // velocity is rejected, so the player got no knockback at all. Treat it as straight up.
    private static Vector blastDirection(Vector target, Vector explosion) {
        Vector direction = target.clone().subtract(explosion);
        return direction.lengthSquared() < MIN_LENGTH_SQUARED ? new Vector(0D, 1D, 0D) : direction.normalize();
    }

    // The launcher gets a fixed impulse: the blast only picks the direction, never the strength.
    // Scaling by the blast vector made a fireball at the feet fire straight up and one a step away
    // fire almost flat, so the same jump never repeated.
    private Vector selfLaunch(Player player, Vector blast) {
        Vector horizontal = new Vector(blast.getX(), 0D, blast.getZ());
        horizontal = horizontal.lengthSquared() < MIN_LENGTH_SQUARED ? horizontalFacing(player) : horizontal.normalize();
        return new Vector(horizontal.getX() * fireballHorizontalSelf, fireballVerticalSelf, horizontal.getZ() * fireballHorizontalSelf);
    }

    private Vector enemyLaunch(Vector blast) {
        double horizontalDistance = Math.sqrt(blast.getX() * blast.getX() + blast.getZ() * blast.getZ());
        double vertical = horizontalDistance <= fireballJumpTolerance
                ? fireballVerticalOthers * ENEMY_VERTICAL_BOOST
                : Math.abs(blast.getY()) * fireballVerticalOthers * ENEMY_VERTICAL_BOOST;
        return new Vector(blast.getX() * fireballHorizontalOthers, vertical, blast.getZ() * fireballHorizontalOthers);
    }

    // Where the player's body points, ignoring pitch. Always unit length, so the launch keeps the
    // same strength whether the player looks up, down or straight ahead.
    private static Vector horizontalFacing(Player player) {
        double yaw = Math.toRadians(player.getLocation().getYaw());
        return new Vector(-Math.sin(yaw), 0D, Math.cos(yaw));
    }
}