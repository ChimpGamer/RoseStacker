package dev.rosewood.rosestacker.nms.v1_16_R3;

import com.google.common.collect.Lists;
import dev.rosewood.rosestacker.nms.NMSHandler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkStatus;
import net.minecraft.server.v1_16_R3.ControllerMove;
import net.minecraft.server.v1_16_R3.DamageSource;
import net.minecraft.server.v1_16_R3.DataWatcher;
import net.minecraft.server.v1_16_R3.DataWatcher.Item;
import net.minecraft.server.v1_16_R3.DataWatcherObject;
import net.minecraft.server.v1_16_R3.DataWatcherRegistry;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityCreeper;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.EnumMobSpawn;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.IChunkAccess;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagDouble;
import net.minecraft.server.v1_16_R3.NBTTagList;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_16_R3.PathfinderGoalFloat;
import net.minecraft.server.v1_16_R3.PathfinderGoalSelector;
import net.minecraft.server.v1_16_R3.PathfinderGoalWrapped;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftCreeper;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("unchecked")
public class NMSHandlerImpl implements NMSHandler {

    private static Method method_EntityLiving_a; // Method to update the EntityLiving LootTable, normally protected
    private static Field field_PacketPlayOutEntityMetadata_a; // Field to set the entity ID for the packet, normally private
    private static Field field_PacketPlayOutEntityMetadata_b; // Field to set the datawatcher changes for the packet, normally private

    private static Method method_WorldServer_registerEntity; // Method to register an entity into a world

    private static DataWatcherObject<Boolean> value_EntityCreeper_d; // DataWatcherObject that determines if a creeper is ignited, normally private
    private static Field field_EntityCreeper_fuseTicks; // Field to set the remaining fuse ticks of a creeper, normally private

    private static Field field_PathfinderGoalSelector_d; // Field to get a PathfinderGoalSelector of an insentient entity, normally private
    private static Field field_EntityInsentient_moveController; // Field to set the move controller of an insentient entity, normally protected

    static {
        try {
            method_EntityLiving_a = EntityLiving.class.getDeclaredMethod("a", DamageSource.class, boolean.class);
            method_EntityLiving_a.setAccessible(true);

            field_PacketPlayOutEntityMetadata_a = PacketPlayOutEntityMetadata.class.getDeclaredField("a");
            field_PacketPlayOutEntityMetadata_a.setAccessible(true);

            field_PacketPlayOutEntityMetadata_b = PacketPlayOutEntityMetadata.class.getDeclaredField("b");
            field_PacketPlayOutEntityMetadata_b.setAccessible(true);

            method_WorldServer_registerEntity = WorldServer.class.getDeclaredMethod("registerEntity", Entity.class);
            method_WorldServer_registerEntity.setAccessible(true);

            Field field_EntityCreeper_d = EntityCreeper.class.getDeclaredField("d");
            field_EntityCreeper_d.setAccessible(true);
            value_EntityCreeper_d = (DataWatcherObject<Boolean>) field_EntityCreeper_d.get(null);

            field_EntityCreeper_fuseTicks = EntityCreeper.class.getDeclaredField("fuseTicks");
            field_EntityCreeper_fuseTicks.setAccessible(true);

            field_PathfinderGoalSelector_d = PathfinderGoalSelector.class.getDeclaredField("d");
            field_PathfinderGoalSelector_d.setAccessible(true);

            field_EntityInsentient_moveController = EntityInsentient.class.getDeclaredField("moveController");
            field_EntityInsentient_moveController.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] getEntityAsNBT(LivingEntity livingEntity, boolean includeAttributes) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream dataOutput = new ObjectOutputStream(outputStream)) {

            NBTTagCompound nbt = new NBTTagCompound();
            EntityLiving craftEntity = ((CraftLivingEntity) livingEntity).getHandle();
            craftEntity.save(nbt);

            // Don't store attributes, it's pretty large and doesn't usually matter
            if (!includeAttributes)
                nbt.remove("Attributes");

            // Write entity type
            String entityType = IRegistry.ENTITY_TYPE.getKey(craftEntity.getEntityType()).toString();
            dataOutput.writeUTF(entityType);

            // Write NBT
            NBTCompressedStreamTools.a(nbt, (OutputStream) dataOutput);

            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public LivingEntity spawnEntityFromNBT(byte[] serialized, Location location) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(serialized);
             ObjectInputStream dataInput = new ObjectInputStream(inputStream)) {

            // Read entity type
            String entityType = dataInput.readUTF();

            // Read NBT
            NBTTagCompound nbt = NBTCompressedStreamTools.a((InputStream) dataInput);

            NBTTagList positionTagList = nbt.getList("Pos", 6);
            positionTagList.set(0, NBTTagDouble.a(location.getX()));
            positionTagList.set(1, NBTTagDouble.a(location.getY()));
            positionTagList.set(2, NBTTagDouble.a(location.getZ()));
            nbt.set("Pos", positionTagList);
            nbt.a("UUID", UUID.randomUUID()); // Reset the UUID to resolve possible duplicates

            Optional<EntityTypes<?>> optionalEntity = EntityTypes.a(entityType);
            if (optionalEntity.isPresent()) {
                WorldServer world = ((CraftWorld) location.getWorld()).getHandle();

                Entity entity = optionalEntity.get().createCreature(
                        world,
                        nbt,
                        null,
                        null,
                        new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                        EnumMobSpawn.COMMAND,
                        true,
                        false
                );

                if (entity == null)
                    throw new NullPointerException("Unable to create entity from NBT");

                IChunkAccess ichunkaccess = world.getChunkAt(MathHelper.floor(entity.locX() / 16.0D), MathHelper.floor(entity.locZ() / 16.0D), ChunkStatus.FULL, entity.attachedToPlayer);
                if (!(ichunkaccess instanceof Chunk))
                    throw new NullPointerException("Unable to spawn entity from NBT, couldn't get chunk");

                ichunkaccess.a(entity);
                method_WorldServer_registerEntity.invoke(world, entity);

                entity.load(nbt);

                return (LivingEntity) entity.getBukkitEntity();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public LivingEntity getNBTAsEntity(EntityType entityType, Location location, byte[] serialized) {
        if (location.getWorld() == null)
            return null;

        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        EntityLiving entity = (EntityLiving) craftWorld.createEntity(location, entityType.getEntityClass());

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(serialized);
             ObjectInputStream dataInput = new ObjectInputStream(inputStream)) {

            // Read entity type, don't need the value
            dataInput.readUTF();

            // Read NBT
            NBTTagCompound nbt = NBTCompressedStreamTools.a((InputStream) dataInput);

            // Set NBT
            entity.load(nbt);

            // Update loot table
            method_EntityLiving_a.invoke(entity, DamageSource.GENERIC, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return (LivingEntity) entity.getBukkitEntity();
    }

    @Override
    public LivingEntity createEntityUnspawned(EntityType entityType, Location location) {
        if (location.getWorld() == null)
            return null;

        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        return (LivingEntity) craftWorld.createEntity(location, entityType.getEntityClass()).getBukkitEntity();
    }

    @Override
    public LivingEntity spawnEntityWithReason(EntityType entityType, Location location, SpawnReason spawnReason) {
        World world = location.getWorld();
        if (world == null)
            throw new IllegalArgumentException("Cannot spawn into null world");

        Class<? extends org.bukkit.entity.Entity> entityClass = entityType.getEntityClass();
        if (entityClass == null || !LivingEntity.class.isAssignableFrom(entityClass))
            throw new IllegalArgumentException("EntityType must be of a LivingEntity");

        CraftWorld craftWorld = (CraftWorld) world;
        return (LivingEntity) craftWorld.spawn(location, entityClass, null, spawnReason);
    }

    @Override
    public void updateEntityNameTagForPlayer(Player player, org.bukkit.entity.Entity entity, String customName, boolean customNameVisible) {
        try {
            List<Item<?>> dataWatchers = new ArrayList<>();
            Optional<IChatBaseComponent> nameComponent = Optional.ofNullable(CraftChatMessage.fromStringOrNull(customName));
            dataWatchers.add(new DataWatcher.Item<>(DataWatcherRegistry.f.a(2), nameComponent));
            dataWatchers.add(new DataWatcher.Item<>(DataWatcherRegistry.i.a(3), customNameVisible));

            PacketPlayOutEntityMetadata packetPlayOutEntityMetadata = new PacketPlayOutEntityMetadata();
            field_PacketPlayOutEntityMetadata_a.set(packetPlayOutEntityMetadata, entity.getEntityId());
            field_PacketPlayOutEntityMetadata_b.set(packetPlayOutEntityMetadata, dataWatchers);

            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetPlayOutEntityMetadata);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateEntityNameTagVisibilityForPlayer(Player player, org.bukkit.entity.Entity entity, boolean customNameVisible) {
        try {
            PacketPlayOutEntityMetadata packetPlayOutEntityMetadata = new PacketPlayOutEntityMetadata();
            field_PacketPlayOutEntityMetadata_a.set(packetPlayOutEntityMetadata, entity.getEntityId());
            field_PacketPlayOutEntityMetadata_b.set(packetPlayOutEntityMetadata, Lists.newArrayList(new DataWatcher.Item<>(DataWatcherRegistry.i.a(3), customNameVisible)));

            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packetPlayOutEntityMetadata);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unigniteCreeper(Creeper creeper) {
        EntityCreeper entityCreeper = ((CraftCreeper) creeper).getHandle();

        entityCreeper.getDataWatcher().set(value_EntityCreeper_d, false);
        try {
            field_EntityCreeper_fuseTicks.set(entityCreeper, entityCreeper.maxFuseTicks);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeEntityGoals(LivingEntity livingEntity) {
        EntityLiving nmsEntity = ((CraftLivingEntity) livingEntity).getHandle();
        if (!(nmsEntity instanceof EntityInsentient))
            return;

        try {
            EntityInsentient insentient = (EntityInsentient) nmsEntity;

            // Remove all goal AI other than floating in water
            Set<PathfinderGoalWrapped> goals = (Set<PathfinderGoalWrapped>) field_PathfinderGoalSelector_d.get(insentient.goalSelector);
            Iterator<PathfinderGoalWrapped> goalsIterator = goals.iterator();
            while (goalsIterator.hasNext()) {
                PathfinderGoalWrapped goal = goalsIterator.next();
                if (goal.j() instanceof PathfinderGoalFloat)
                    continue;

                goalsIterator.remove();
            }

            // Remove all targetting AI
            ((Set<PathfinderGoalWrapped>) field_PathfinderGoalSelector_d.get(insentient.targetSelector)).clear();

            // Forget any existing targets
            insentient.setGoalTarget(null);

            // Remove the move controller and replace it with a dummy one
            ControllerMove dummyMoveController = new ControllerMove(insentient) {
                @Override
                public void a() { }
            };

            field_EntityInsentient_moveController.set(insentient, dummyMoveController);
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public ItemStack setItemStackNBT(ItemStack itemStack, String key, String value) {
        net.minecraft.server.v1_16_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
        tagCompound.setString(key, value);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public ItemStack setItemStackNBT(ItemStack itemStack, String key, int value) {
        net.minecraft.server.v1_16_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
        tagCompound.setInt(key, value);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public String getItemStackNBTString(ItemStack itemStack, String key) {
        net.minecraft.server.v1_16_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
        return tagCompound.getString(key);
    }

    @Override
    public int getItemStackNBTInt(ItemStack itemStack, String key) {
        net.minecraft.server.v1_16_R3.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
        return tagCompound.getInt(key);
    }

}
