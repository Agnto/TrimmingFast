package me.argento.trimmingFast;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TrimCommand implements CommandExecutor, TabCompleter {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Component PREFIX = miniMessage.deserialize("<#FED000><b>Sʜᴏᴘ Sᴜʀᴠɪᴠᴀʟ</b><dark_gray> » ");
    private final Component ERROR_COLOR = miniMessage.deserialize("<#D32F2F>");
    private final Component MESSAGE_COLOR = miniMessage.deserialize("<white>");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX.append(MESSAGE_COLOR.append(Component.text("This command can only be used by a player."))));
            return true;
        }

        Player player = (Player) sender;
        if (args.length < 3) {
            player.sendMessage(PREFIX.append(ERROR_COLOR.append(Component.text("Usage: /trim <wearing/inventory> <armortrim> <material>"))));
            return false;
        }

        String mode = args[0].toLowerCase();
        String patternName = args[1].toLowerCase();
        String materialName = args[2].toLowerCase();

        Optional<TrimPattern> trimPatternOpt = Bukkit.getRegistry(TrimPattern.class)
                .stream()
                .filter(p -> p.key().value().endsWith(patternName))
                .findFirst();

        if (trimPatternOpt.isEmpty()) {
            player.sendMessage(PREFIX.append(ERROR_COLOR.append(Component.text("Invalid trim pattern: " + patternName))));
            return false;
        }

        Optional<TrimMaterial> trimMaterialOpt = Bukkit.getRegistry(TrimMaterial.class)
                .stream()
                .filter(m -> m.key().value().endsWith(materialName))
                .findFirst();

        if (trimMaterialOpt.isEmpty()) {
            player.sendMessage(PREFIX.append(ERROR_COLOR.append(Component.text("Invalid trim material: " + materialName))));
            return false;
        }

        TrimPattern pattern = trimPatternOpt.get();
        TrimMaterial material = trimMaterialOpt.get();

        ArmorTrim newTrim = new ArmorTrim(material, pattern);
        PlayerInventory inv = player.getInventory();
        int trimmedCount = 0;
        int maxTrimmable;

        if (mode.equals("wearing")) {
            ItemStack[] armor = {inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()};

            long trimmablePieces = Stream.of(armor)
                    .filter(item -> item != null && item.getItemMeta() instanceof ArmorMeta)
                    .count();

            if (trimmablePieces == 0) {
                player.sendMessage(PREFIX.append(MESSAGE_COLOR.append(Component.text("No suitable armor pieces were found to trim."))));
                return true;
            }

            if (!hasResources(player, pattern.key(), material.key(), (int) trimmablePieces)) {
                player.sendMessage(PREFIX.append(ERROR_COLOR.append(Component.text("You don't have enough resources to trim all your worn armor pieces."))));
                return false;
            }

            for (ItemStack item : armor) {
                if (item != null && item.getItemMeta() instanceof ArmorMeta) {
                    ArmorMeta meta = (ArmorMeta) item.getItemMeta();
                    meta.setTrim(newTrim);
                    item.setItemMeta(meta);
                    trimmedCount++;
                }
            }
            if (trimmedCount > 0) {
                consumeResources(player, pattern.key(), material.key(), trimmedCount);
                player.sendMessage(PREFIX.append(MESSAGE_COLOR.append(Component.text("Successfully trimmed " + trimmedCount + " armor pieces!"))));
            } else {
                player.sendMessage(PREFIX.append(MESSAGE_COLOR.append(Component.text("No suitable armor pieces were found to trim."))));
            }

        } else if (mode.equals("inventory")) {
            List<ItemStack> inventoryItems = new ArrayList<>();
            for (ItemStack item : inv.getStorageContents()) {
                if (item != null && item.getItemMeta() instanceof ArmorMeta) {
                    inventoryItems.add(item);
                }
            }

            if (inventoryItems.isEmpty()) {
                player.sendMessage(PREFIX.append(MESSAGE_COLOR.append(Component.text("No suitable armor pieces were found in your inventory."))));
                return true;
            }

            int availablePatterns = countItems(player, getTemplateMaterial(pattern.key()));
            int availableMaterials = countItems(player, getMaterialItem(material.key()));

            maxTrimmable = Math.min(availablePatterns, availableMaterials);

            if (maxTrimmable == 0) {
                player.sendMessage(PREFIX.append(ERROR_COLOR.append(Component.text("You don't have any resources to trim armor."))));
                return false;
            }

            for (ItemStack item : inventoryItems) {
                if (trimmedCount >= maxTrimmable) {
                    break;
                }
                ArmorMeta meta = (ArmorMeta) item.getItemMeta();
                meta.setTrim(newTrim);
                item.setItemMeta(meta);
                trimmedCount++;
            }

            if (trimmedCount > 0) {
                consumeResources(player, pattern.key(), material.key(), trimmedCount);
                player.sendMessage(PREFIX.append(MESSAGE_COLOR.append(Component.text("Successfully trimmed " + trimmedCount + " armor pieces!"))));
            }

            if (trimmedCount < inventoryItems.size()) {
                player.sendMessage(PREFIX.append(MESSAGE_COLOR.append(Component.text(String.format("You only had enough resources to trim %d out of %d armor pieces.", trimmedCount, inventoryItems.size())))));
            }

        } else {
            player.sendMessage(PREFIX.append(ERROR_COLOR.append(Component.text("Invalid mode. Use 'wearing' or 'inventory'."))));
            return false;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("wearing");
            completions.add("inventory");
            return completions.stream().filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
        }

        if (args.length == 2) {
            return Bukkit.getRegistry(TrimPattern.class).stream()
                    .map(p -> p.key().value())
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            return Bukkit.getRegistry(TrimMaterial.class).stream()
                    .map(m -> m.key().value())
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
        }

        return completions;
    }

    private boolean hasResources(Player player, Key patternKey, Key materialKey, int requiredCount) {
        Material patternItem = getTemplateMaterial(patternKey);
        Material materialItem = getMaterialItem(materialKey);

        if (patternItem == null || materialItem == null) {
            return false;
        }

        return countItems(player, patternItem) >= requiredCount && countItems(player, materialItem) >= requiredCount;
    }

    private void consumeResources(Player player, Key patternKey, Key materialKey, int count) {
        Material patternItem = getTemplateMaterial(patternKey);
        Material materialItem = getMaterialItem(materialKey);

        if (patternItem != null) {
            player.getInventory().removeItem(new ItemStack(patternItem, count));
        }
        if (materialItem != null) {
            player.getInventory().removeItem(new ItemStack(materialItem, count));
        }
    }

    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private Material getTemplateMaterial(Key key) {
        String templateName = key.value().toUpperCase() + "_ARMOR_TRIM_SMITHING_TEMPLATE";
        return Material.getMaterial(templateName);
    }

    private Material getMaterialItem(Key key) {
        return Material.getMaterial(key.value().toUpperCase());
    }
}