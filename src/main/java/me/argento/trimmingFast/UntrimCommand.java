package me.argento.trimmingFast;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UntrimCommand implements CommandExecutor, TabCompleter {

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
        if (args.length < 1) {
            player.sendMessage(PREFIX.append(ERROR_COLOR.append(Component.text("Usage: /untrim <wearing/inventory>"))));
            return false;
        }

        String mode = args[0].toLowerCase();
        PlayerInventory inv = player.getInventory();
        int untrimmedCount = 0;

        if (mode.equals("wearing")) {
            ItemStack[] armor = {inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()};

            for (ItemStack item : armor) {
                if (item != null && item.getItemMeta() instanceof ArmorMeta) {
                    ArmorMeta meta = (ArmorMeta) item.getItemMeta();
                    if (meta.hasTrim()) {
                        meta.setTrim(null);
                        item.setItemMeta(meta);
                        untrimmedCount++;
                    }
                }
            }
        } else if (mode.equals("inventory")) {
            for (ItemStack item : inv.getStorageContents()) {
                if (item != null && item.getItemMeta() instanceof ArmorMeta) {
                    ArmorMeta meta = (ArmorMeta) item.getItemMeta();
                    if (meta.hasTrim()) {
                        meta.setTrim(null);
                        item.setItemMeta(meta);
                        untrimmedCount++;
                    }
                }
            }
        } else {
            player.sendMessage(PREFIX.append(ERROR_COLOR.append(Component.text("Invalid mode. Use 'wearing' or 'inventory'."))));
            return false;
        }

        if (untrimmedCount > 0) {
            player.sendMessage(PREFIX.append(MESSAGE_COLOR.append(Component.text("Successfully removed trims from " + untrimmedCount + " armor pieces!"))));
        } else {
            player.sendMessage(PREFIX.append(MESSAGE_COLOR.append(Component.text("No trimmed armor pieces were found."))));
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
        return completions;
    }
}