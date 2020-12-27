package dev.rosewood.rosestacker.listener;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosestacker.manager.ConfigurationManager.Setting;
import dev.rosewood.rosestacker.manager.LocaleManager;
import dev.rosewood.rosestacker.manager.StackManager;
import dev.rosewood.rosestacker.manager.StackSettingManager;
import dev.rosewood.rosestacker.stack.StackedEntity;
import dev.rosewood.rosestacker.stack.StackedSpawner;
import dev.rosewood.rosestacker.stack.settings.EntityStackSettings;
import dev.rosewood.rosestacker.utils.StackerUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {

    private RosePlugin rosePlugin;

    public InteractListener(RosePlugin rosePlugin) {
        this.rosePlugin = rosePlugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        ItemStack item = event.getItem();
        if (item == null || event.getAction() != Action.RIGHT_CLICK_BLOCK || clickedBlock == null)
            return;

        StackManager stackManager = this.rosePlugin.getManager(StackManager.class);
        if (stackManager.isWorldDisabled(event.getPlayer().getWorld()))
            return;

        if (stackManager.isSpawnerStackingEnabled()) {
            // Handle spawner conversion before we try to spawn entities
            if (clickedBlock.getType() == Material.SPAWNER
                    && StackerUtils.isSpawnEgg(item.getType())
                    && StackerUtils.getStackedItemStackAmount(item) == 1) {

                CreatureSpawner creatureSpawner = (CreatureSpawner) clickedBlock.getState();
                EntityStackSettings stackSettings = this.rosePlugin.getManager(StackSettingManager.class).getEntityStackSettings(item.getType());
                if (stackSettings != null && stackSettings.getEntityType() == creatureSpawner.getSpawnedType()) {
                    // Don't allow converting spawners if it's the exact same type... that just wastes spawn eggs
                    event.setCancelled(true);
                    return;
                }

                if (!event.getPlayer().hasPermission("rosestacker.spawnerconvert")) {
                    event.setCancelled(true);
                    return;
                }

                StackedSpawner stackedSpawner = stackManager.getStackedSpawner(clickedBlock);
                if (stackedSpawner == null)
                    return;

                if (Setting.SPAWNER_CONVERT_REQUIRE_SAME_AMOUNT.getBoolean()
                        && item.getAmount() < stackedSpawner.getStackSize()
                        && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    event.setCancelled(true);
                    this.rosePlugin.getManager(LocaleManager.class).sendMessage(event.getPlayer(), "spawner-convert-not-enough");
                    return;
                }

                Bukkit.getScheduler().runTask(this.rosePlugin, () -> {
                    // Make sure spawners convert and update their display properly
                    stackedSpawner.updateSpawnerProperties();
                    stackedSpawner.updateDisplay();

                    if (stackedSpawner.getStackSize() != 1 && event.getPlayer().getGameMode() != GameMode.CREATIVE)
                        item.setAmount(item.getAmount() - stackedSpawner.getStackSize() + 1);
                });

                return;
            }
        }

        Location spawnLocation = clickedBlock.getRelative(event.getBlockFace()).getLocation();
        spawnLocation.add(0.5, 0, 0.5); // Center on block

        if (this.spawnEntities(null, spawnLocation, item)) {
            StackerUtils.takeOneItem(event.getPlayer(), event.getHand());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity))
            return;

        StackManager stackManager = this.rosePlugin.getManager(StackManager.class);
        if (stackManager.isWorldDisabled(event.getPlayer().getWorld()))
            return;

        if (!stackManager.isEntityStackingEnabled())
            return;

        LivingEntity entity = (LivingEntity) event.getRightClicked();
        StackedEntity stackedEntity = stackManager.getStackedEntity(entity);
        if (stackedEntity == null)
            return;

        Player player = event.getPlayer();
        ItemStack itemStack = event.getHand() == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        if (itemStack.getType() == Material.NAME_TAG) {
            Bukkit.getScheduler().runTask(this.rosePlugin, stackedEntity::updateDisplay);
            return;
        } else if (itemStack.getType() == Material.WATER_BUCKET) {
            switch (entity.getType()) {
                case COD:
                case SALMON:
                case PUFFERFISH:
                case TROPICAL_FISH:
                    break;
                default: return;
            }

            if (stackedEntity.getStackSize() != 1)
                Bukkit.getScheduler().runTask(this.rosePlugin, stackedEntity::decreaseStackSize);
            return;
        }

        if (this.spawnEntities(entity, entity.getLocation(), itemStack)) {
            StackerUtils.takeOneItem(event.getPlayer(), event.getHand());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDispenserDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DISPENSER)
            return;

        ItemStack itemStack = event.getItem();
        Location spawnLocation = block.getRelative(((Directional) block.getBlockData()).getFacing()).getLocation();
        spawnLocation.add(0.5, 0, 0.5);

        if (this.spawnEntities(null, spawnLocation, itemStack)) {
            Inventory inventory = ((Container) block.getState()).getInventory();
            Bukkit.getScheduler().runTask(this.rosePlugin, () -> {
                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    ItemStack item = inventory.getItem(slot);
                    if (item == null || !item.isSimilar(itemStack))
                        continue;
                    item.setAmount(item.getAmount() - 1);
                    if (item.getAmount() < 0)
                        inventory.setItem(slot, null);
                    break;
                }
            });

            event.setCancelled(true);
        }
    }

    private boolean spawnEntities(Entity original, Location spawnLocation, ItemStack itemStack) {
        if (!StackerUtils.isSpawnEgg(itemStack.getType()))
            return false;

        int spawnAmount = StackerUtils.getStackedItemStackAmount(itemStack);
        if (spawnAmount == 1)
            return false;

        EntityStackSettings stackSettings = this.rosePlugin.getManager(StackSettingManager.class).getEntityStackSettings(itemStack.getType());
        EntityType entityType = stackSettings.getEntityType();
        if (original != null && original.getType() != entityType)
            return false;

        if (spawnLocation.getWorld() == null)
            return false;

        StackManager stackManager = this.rosePlugin.getManager(StackManager.class);
        if (stackManager.isWorldDisabled(spawnLocation.getWorld()))
            return false;

        stackManager.preStackEntities(entityType, spawnAmount, spawnLocation, SpawnReason.SPAWNER_EGG);

        return true;
    }

}
