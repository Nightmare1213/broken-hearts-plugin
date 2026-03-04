package com.example.brokenhearts;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeartEquipListener implements Listener {
    private static final long SWIFT_COOLDOWN_MS = 4_000;
    private static final long NIGHT_COOLDOWN_MS = 10_000;
    private static final long FROST_COOLDOWN_MS = 12_000;
    private static final long THUNDER_COOLDOWN_MS = 3_000;
    private static final long DRAGON_COOLDOWN_MS = 20_000;
    private static final long WITHER_COOLDOWN_MS = 12_000;

    private final JavaPlugin plugin;
    private final HeartItemService itemService;
    private final Map<UUID, Long> swiftCooldowns = new HashMap<>();
    private final Map<UUID, Long> nightCooldowns = new HashMap<>();
    private final Map<UUID, Long> frostCooldowns = new HashMap<>();
    private final Map<UUID, Long> thunderCooldowns = new HashMap<>();
    private final Map<UUID, Long> dragonCooldowns = new HashMap<>();
    private final Map<UUID, Long> witherCooldowns = new HashMap<>();
    private final Map<UUID, Long> stormTargets = new HashMap<>();
    private final Map<UUID, Long> stunnedMonsters = new HashMap<>();
    private final Map<UUID, EnumMap<HeartType, Integer>> bossKills = new HashMap<>();

    public HeartEquipListener(JavaPlugin plugin, HeartItemService itemService) {
        this.plugin = plugin;
        this.itemService = itemService;
    }

    public void startTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickStatuses, 20L, 20L);
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        HeartType equipped = getEquippedHeart(killer);
        if (equipped == null || !equipped.isBossHeart()) {
            return;
        }

        int kills = addBossKill(killer.getUniqueId(), equipped, 1);
        int tier = toBossTier(kills);
        killer.sendMessage(Component.text(equipped.abilityName() + " grew stronger. Kills: " + kills + " (Tier " + tier + ")"));
    }

    @EventHandler
    public void onDamageOther(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        HeartType equipped = getEquippedHeart(player);
        if (equipped == null) {
            return;
        }

        int bossTier = getBossTier(player.getUniqueId(), equipped);
        switch (equipped) {
            case FLAME -> {
                target.setFireTicks(Math.max(target.getFireTicks(), 160));
                event.setDamage(event.getDamage() + 2.0D);
            }
            case VAMPIRIC -> {
                double drainAmount = Math.min(4.0D, Math.max(0.0D, target.getHealth() - 1.0D));
                if (drainAmount > 0.0D) {
                    target.setHealth(target.getHealth() - drainAmount);
                    player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + drainAmount));
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false, true));
            }
            case THUNDER -> {
                long now = System.currentTimeMillis();
                long readyAt = thunderCooldowns.getOrDefault(player.getUniqueId(), 0L);
                if (readyAt <= now) {
                    Location strike = target.getLocation();
                    if (strike.getWorld() != null) {
                        strike.getWorld().strikeLightningEffect(strike);
                    }
                    event.setDamage(event.getDamage() + 4.0D);
                    long stormDurationMs = (6L + bossTier * 2L) * 1000L;
                    stormTargets.put(target.getUniqueId(), now + stormDurationMs);
                    thunderCooldowns.put(player.getUniqueId(), now + THUNDER_COOLDOWN_MS);
                }
            }
            case WARDEN -> {
                event.setDamage(event.getDamage() + 3.0D + bossTier);
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100 + (bossTier * 20), 0, true, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60 + (bossTier * 20), Math.min(4, 2 + bossTier / 2), true, false, true));
            }
            case WITHER -> {
                event.setDamage(event.getDamage() + 2.0D + (bossTier * 0.5D));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80 + (bossTier * 20), Math.min(3, 1 + bossTier / 2), true, false, true));
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 1.0D + (bossTier * 0.5D)));
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        HeartType equipped = getEquippedHeart(player);
        if (equipped == HeartType.STONE) {
            event.setDamage(event.getDamage() * 0.55D);
        } else if (equipped == HeartType.DRAGON) {
            double reduction = 0.70D - (getBossTier(player.getUniqueId(), HeartType.DRAGON) * 0.03D);
            event.setDamage(event.getDamage() * Math.max(0.50D, reduction));
        } else if (equipped == HeartType.WITHER) {
            event.setDamage(event.getDamage() * 0.80D);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (getEquippedHeart(player) != HeartType.SWIFT) {
            return;
        }

        event.setCancelled(true);
        long now = System.currentTimeMillis();
        long readyAt = swiftCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) {
            sendCooldownMessage(player, "Tempest Dash", readyAt - now);
            return;
        }

        Vector direction = player.getLocation().getDirection().normalize().multiply(2.0);
        direction.setY(Math.max(0.45, direction.getY() + 0.35));
        player.setVelocity(direction);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, true, false, true));
        swiftCooldowns.put(player.getUniqueId(), now + SWIFT_COOLDOWN_MS);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!event.getAction().isRightClick()) {
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        HeartType equipped = getEquippedHeart(player);
        if (equipped == HeartType.NIGHT) {
            handleNightPulse(player);
        } else if (equipped == HeartType.FROST) {
            handleFrostNova(player);
        } else if (equipped == HeartType.DRAGON) {
            handleDragonRoar(player);
        } else if (equipped == HeartType.WITHER) {
            handleWitherBurst(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        swiftCooldowns.remove(id);
        nightCooldowns.remove(id);
        frostCooldowns.remove(id);
        thunderCooldowns.remove(id);
        dragonCooldowns.remove(id);
        witherCooldowns.remove(id);
    }

    private void handleNightPulse(Player player) {
        long now = System.currentTimeMillis();
        long readyAt = nightCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) {
            sendCooldownMessage(player, "Abyss Pulse", readyAt - now);
            return;
        }

        boolean isNight = isNightTime(player);
        int glowDuration = isNight ? 220 : 140;
        int slowDuration = isNight ? 140 : 80;
        int slowAmplifier = isNight ? 2 : 1;

        int revealed = 0;
        for (Entity entity : player.getChunk().getEntities()) {
            if (!(entity instanceof LivingEntity living) || !living.isValid() || living.isDead()) {
                continue;
            }

            living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowDuration, 0, true, false, true));
            if (entity instanceof Monster) {
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, true, false, true));
            }
            revealed++;
        }

        String timeBoost = isNight ? " Night empowered!" : "";
        player.sendMessage(Component.text("Abyss Pulse revealed " + revealed + " living entity(s) in your chunk." + timeBoost));
        nightCooldowns.put(player.getUniqueId(), now + NIGHT_COOLDOWN_MS);
    }

    private boolean isNightTime(Player player) {
        long time = player.getWorld().getTime();
        return time >= 13000L && time <= 23000L;
    }

    private void handleFrostNova(Player player) {
        long now = System.currentTimeMillis();
        long readyAt = frostCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) {
            sendCooldownMessage(player, "Glacial Prison", readyAt - now);
            return;
        }

        int frozen = 0;
        for (LivingEntity entity : player.getLocation().getNearbyLivingEntities(10, target -> target instanceof Monster)) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10, true, false, true));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 3, true, false, true));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 100, 3, true, false, true));
            if (entity instanceof Monster monster) {
                monster.setAI(false);
                stunnedMonsters.put(monster.getUniqueId(), now + 3_000L);
            }
            frozen++;
        }

        player.sendMessage(Component.text("Glacial Prison locked down " + frozen + " monster(s)."));
        frostCooldowns.put(player.getUniqueId(), now + FROST_COOLDOWN_MS);
    }

    private void handleDragonRoar(Player player) {
        long now = System.currentTimeMillis();
        long readyAt = dragonCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) {
            sendCooldownMessage(player, "Dragon Roar", readyAt - now);
            return;
        }

        int tier = getBossTier(player.getUniqueId(), HeartType.DRAGON);
        int duration = 160 + (tier * 40);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, Math.min(3, 1 + tier / 2), true, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, Math.min(3, 1 + tier / 2), true, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, Math.min(2, tier / 2), true, false, true));

        int blasted = 0;
        for (LivingEntity entity : player.getLocation().getNearbyLivingEntities(12 + tier, target -> target instanceof Monster)) {
            Vector push = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.4 + tier * 0.1);
            push.setY(0.45);
            entity.setVelocity(push);
            entity.setFireTicks(Math.max(entity.getFireTicks(), 80 + (tier * 20)));
            blasted++;
        }

        player.sendMessage(Component.text("Dragon Roar (Tier " + tier + ") blasted " + blasted + " monster(s)."));
        dragonCooldowns.put(player.getUniqueId(), now + DRAGON_COOLDOWN_MS);
    }

    private void handleWitherBurst(Player player) {
        long now = System.currentTimeMillis();
        long readyAt = witherCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (readyAt > now) {
            sendCooldownMessage(player, "Wither Reign", readyAt - now);
            return;
        }

        int tier = getBossTier(player.getUniqueId(), HeartType.WITHER);
        int cursed = 0;
        for (LivingEntity entity : player.getLocation().getNearbyLivingEntities(10 + tier, target -> target instanceof Monster)) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100 + (tier * 30), Math.min(3, 1 + tier / 2), true, false, true));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80 + (tier * 20), 1, true, false, true));
            cursed++;
        }

        player.sendMessage(Component.text("Wither Reign (Tier " + tier + ") cursed " + cursed + " monster(s)."));
        witherCooldowns.put(player.getUniqueId(), now + WITHER_COOLDOWN_MS);
    }

    private void tickStatuses() {
        long now = System.currentTimeMillis();

        stormTargets.entrySet().removeIf(entry -> {
            if (entry.getValue() <= now) {
                return true;
            }

            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || !living.isValid() || living.isDead()) {
                return true;
            }

            Location location = living.getLocation();
            if (location.getWorld() != null) {
                location.getWorld().strikeLightningEffect(location);
            }
            living.damage(1.5D);
            living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, true, false, true));
            return false;
        });

        stunnedMonsters.entrySet().removeIf(entry -> {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof Monster monster) || !monster.isValid() || monster.isDead()) {
                return true;
            }
            if (entry.getValue() <= now) {
                monster.setAI(true);
                return true;
            }
            return false;
        });
    }

    private int addBossKill(UUID playerId, HeartType type, int amount) {
        EnumMap<HeartType, Integer> map = bossKills.computeIfAbsent(playerId, key -> new EnumMap<>(HeartType.class));
        int total = map.getOrDefault(type, 0) + amount;
        map.put(type, total);
        return total;
    }

    private int getBossTier(UUID playerId, HeartType type) {
        if (!type.isBossHeart()) {
            return 0;
        }
        int kills = bossKills.getOrDefault(playerId, new EnumMap<>(HeartType.class)).getOrDefault(type, 0);
        return toBossTier(kills);
    }

    private int toBossTier(int kills) {
        return Math.min(5, kills / 5);
    }

    private void sendCooldownMessage(Player player, String abilityName, long remainingMs) {
        double seconds = remainingMs / 1000.0;
        player.sendMessage(Component.text(abilityName + " cooldown: " + String.format("%.1f", seconds) + "s"));
    }

    private HeartType getEquippedHeart(Player player) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        return itemService.detectHeart(offHand).orElse(null);
    }
}
