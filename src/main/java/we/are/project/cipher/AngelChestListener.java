package we.are.project.cipher;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Consumer;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AngelChestListener implements Listener {
    /* Spawn AngelChest */ @EventHandler public void onDeath(PlayerDeathEvent event) {

        ProjectCipher.getInstance().runIfElse(!ProjectCipher.getInstance().getConfig().getBoolean("only-when-inv-not-empty") || !event.getDrops().isEmpty(), () ->

        ProjectCipher.getInstance()

                .run(() -> System.setProperty("angelchest/" + event.getEntity().getUniqueId(), ProjectCipher.getInstance().getConfig().getStringList("angelchest.message").stream().collect(Collectors.joining("\n"))
                                .replace("{x}",String.valueOf(event.getEntity().getLocation().getBlock().getX()))
                        .replace("{y}",String.valueOf(event.getEntity().getLocation().getBlock().getY()))
                        .replace("{z}",String.valueOf(event.getEntity().getLocation().getBlock().getZ()))))

                /* Set BlockData */
                .run(() -> event.getEntity().getLocation().getBlock().setBlockData(Bukkit.createBlockData(ProjectCipher.getInstance().getConfig().getString("angelchest.material"))))

                /* Save Inventory to Chunk PDC */
                .run(() -> event.getEntity().getLocation().getChunk().getPersistentDataContainer().set(new NamespacedKey(ProjectCipher.getInstance(), event.getEntity().getLocation().getBlockX() + "/" + event.getEntity().getLocation().getBlockY() + "/" + event.getEntity().getLocation().getBlockZ()), PersistentDataType.BYTE_ARRAY, serializeItems(event.getDrops())))

                /* Clear drops */
                .run(() -> event.getDrops().clear())

                /* Spawn Hologram */
                .run(() -> event.getEntity().getWorld().spawn(event.getEntity().getLocation().getBlock().getLocation().add(0.5, ProjectCipher.getInstance().getConfig().getDouble("angelchest.hologram-height"), 0.5), ArmorStand.class, hologram -> {
                    ProjectCipher.getInstance()
                            /* Create Hologram */
                            .run(() -> hologram.setCustomName(ChatColor.translateAlternateColorCodes('&',ProjectCipher.getInstance().getConfig().getString("angelchest.hologram").replace("{player}",event.getEntity().getName()))))
                            /* Make text visible */
                            .run(() -> hologram.setCustomNameVisible(true))
                            /* Store UUID */
                            .run(() -> event.getEntity().getLocation().getChunk().getPersistentDataContainer().set(new NamespacedKey(ProjectCipher.getInstance(), "hologram/" + event.getEntity().getLocation().getBlockX() + "/" + event.getEntity().getLocation().getBlockY() + "/" + event.getEntity().getLocation().getBlockZ()),PersistentDataType.STRING,hologram.getUniqueId().toString()))
                            /* Marker */
                            .run(() -> hologram.setMarker(true))
                            /* Lock */
                            .run(() -> Arrays.stream(EquipmentSlot.values()).forEach(slot -> hologram.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING)))
                            /* Small */
                            .run(() -> hologram.setSmall(true))
                            /* Make invisible */
                            .run(() -> hologram.setVisible(false));
                }))

                /* Show message */
                .run(() -> Bukkit.getScheduler().runTaskTimer(ProjectCipher.getInstance(), task -> {
                    ProjectCipher.getInstance().runIfElse(event.getEntity().isDead(), null, () -> {
                        ProjectCipher.getInstance().run(() -> task.cancel()).run(() -> event.getEntity().sendMessage(ChatColor.translateAlternateColorCodes('&',System.getProperty("angelchest/" + event.getEntity().getUniqueId()))));
                    });
                },1L,1L))
        , null);

    }

    /* Open AngelChest */ @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR) public void onOpenAngelChest(BlockBreakEvent event) {

        ProjectCipher.getInstance().runIfElse(event.getBlock().getChunk().getPersistentDataContainer().has(new NamespacedKey(ProjectCipher.getInstance(),event.getBlock().getX() +"/"+event.getBlock().getY()+"/"+event.getBlock().getZ()),PersistentDataType.BYTE_ARRAY),() ->
        ProjectCipher.getInstance().run(() -> deserializeItems(event.getBlock().getChunk().getPersistentDataContainer().getOrDefault(new NamespacedKey(ProjectCipher.getInstance(),event.getBlock().getX() +"/"+event.getBlock().getY()+"/"+event.getBlock().getZ()),PersistentDataType.BYTE_ARRAY,null)).forEach(item -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5,0.5,0.5),item)))
                .run(() -> event.getBlock().getChunk().getPersistentDataContainer().remove(new NamespacedKey(ProjectCipher.getInstance(),event.getBlock().getX() +"/"+event.getBlock().getY()+"/"+event.getBlock().getZ())))
                .run(() -> { try { Bukkit.getEntity(UUID.fromString(event.getBlock().getChunk().getPersistentDataContainer().get(new NamespacedKey(ProjectCipher.getInstance(),"hologram/" + event.getBlock().getX() +"/"+event.getBlock().getY()+"/"+event.getBlock().getZ()),PersistentDataType.STRING))).remove(); } catch ( Throwable t ) { return; } })
                .run(() -> event.setDropItems(false))
                ,null);

    }

    public static byte [] serializeItems(final Collection<ItemStack> serializable) {
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); final BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(outputStream)) {
            for (final ItemStack t : serializable) {
                bukkitObjectOutputStream.writeObject(t);
            }
            return outputStream.toByteArray();
        } catch (IOException ignored) {
            return null;
        }
    }

    public static Collection<ItemStack> deserializeItems(byte[] bytes) {
        List < ItemStack > list = new ArrayList<>();
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes); BukkitObjectInputStream bukkitObjectInputStream = new BukkitObjectInputStream(inputStream)) {
            while(true) list.add((ItemStack) bukkitObjectInputStream.readObject());
        } catch (Throwable ignored) {

        }
        return list;
    }

    /*
     * NamespacedKey from "event.getEntity()":
     *   new NamespacedKey(ProjectCipher.getInstance(), event.getEntity().getLocation().getBlockX() + "/" + event.getEntity().getLocation().getBlockY() + "/" + event.getEntity().getLocation().getBlockZ())
     */
}

