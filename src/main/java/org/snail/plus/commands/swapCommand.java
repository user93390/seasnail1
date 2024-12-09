package org.snail.plus.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

public class swapCommand extends Command {

    public swapCommand() {
        super("swap", "Swap to an item in your inventory.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("item_name", StringArgumentType.string())
                .executes(context -> {
                    String itemName = context.getArgument("item_name", String.class);
                    if (mc.player != null && mc.player.getInventory() != null) {
                        List<Item> items = getInventoryItems();
                        for (Item item : items) {
                            if (item.getName().getString().equalsIgnoreCase(itemName)) {
                                FindItemResult hotbarSlot = InvUtils.findInHotbar(item);
                                if (isValidSlot(hotbarSlot.slot())) {
                                    mc.player.getInventory().selectedSlot = hotbarSlot.slot();
                                    return 1;
                                }
                            }
                        }
                        error("Item not found in inventory");
                    }
                    return 0;
                }));
    }

    public List<Item> getInventoryItems() {
        List<Item> inventoryItems = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (!inventoryItems.contains(item)) {
                inventoryItems.add(item);
            }
        }
        return inventoryItems;
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0;
    }
}