package me.vacefronvevo.dogwhisperer;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class DogWhispererHelp implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player))
            return false;

        var bookItemStack = new ItemStack(Material.WRITTEN_BOOK);
        var bookItemData = (BookMeta)bookItemStack.getItemMeta();

        if (bookItemData == null)
            return false;

        bookItemData.setTitle("Dog whisperer help");
        bookItemData.setAuthor("Dog whisperer");
        bookItemData.addPage(ChatColor.translateAlternateColorCodes('&', "Thanks for using dog whisperer!\n\nTurn the page for instructions on how to use this plugin.\n\n\n\n\n\n\n\n &o- VAC Efron&r"));
        bookItemData.addPage(ChatColor.translateAlternateColorCodes('&', "&lRequirements:&r\n - Have a stick equipped"));
        bookItemData.addPage(ChatColor.translateAlternateColorCodes('&', "&lAdd/release dog:&r\n - Left click the dog\n\n&lMove the dog(s):&r\n - Left click to move to location\n - Right click to recall\n\n&lAttack mob:&r\n - Left click the mob"));
        bookItemData.addPage(ChatColor.translateAlternateColorCodes('&', "&lSummon dogs:&r\n - Use '/summondogs' to teleport the dogs in your party to your location"));
        bookItemStack.setItemMeta(bookItemData);

        player.openBook(bookItemStack);

        return true;
    }
}
