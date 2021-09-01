package fliens.autocraft;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

// Inventory Util methods written by avixk
public class Util {
    public static boolean canInventoryHold(ItemStack[] contents, ItemStack targetItem){
        int count = targetItem.getAmount();
        for(ItemStack item : contents.clone()){
            if(item == null) return true;
            if(isSimilar(item,targetItem.clone())){
                count = count - (item.getMaxStackSize() - item.getAmount());
            }
        }
        return count < 1;
    }
    public static boolean canInventoryHold(ItemStack[] contents, List<ItemStack> targetItems){
        List<ItemStack> pool = new ArrayList<>(targetItems);
        for(ItemStack item : contents.clone()){
            for(ItemStack poolItem : new ArrayList<>(pool)){
                if(item == null){
                    pool.remove(poolItem);
                    break;
                }else if(isSimilar(item,poolItem)){
                    int newAmount = poolItem.getAmount() - (item.getMaxStackSize() - item.getAmount());
                    if(newAmount < 1){
                        pool.remove(poolItem);
                        break;
                    } else
                        poolItem.setAmount(newAmount);
                }
            }
        }
        return pool.size() == 0;
    }
    public static boolean isInventoryFull(ItemStack[] contents){
        for(ItemStack item : contents.clone()){
            if(item == null || item.getAmount() < item.getMaxStackSize()) return false;
        }
        return true;
    }
    public static boolean isInventoryEmpty(ItemStack[] contents){
        for(ItemStack item : contents.clone()){
            if(item != null && !item.getType().isAir()) return false;
        }
        return true;
    }

    public static boolean isSimilar(ItemStack item1, ItemStack item2){
        return item1.isSimilar(item2);
    }
}
