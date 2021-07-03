package fliens.autocraft;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;

public class EventListener implements Listener{
	
	public EventListener(AutoCraft plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onItemMove(BlockDispenseEvent e) {
		if(e.getBlock().getType().equals(Material.DISPENSER)) {
			List<Block> autoCrafters = new ArrayList<Block>();

			for (World world : Bukkit.getWorlds()) {
				for (Chunk chunk : world.getLoadedChunks()) {
					for (Entity entity : chunk.getEntities()) {
						if (entity.getType().equals(EntityType.ITEM_FRAME)) {
							if (((ItemFrame) entity).getItem().equals(new ItemStack(Material.CRAFTING_TABLE))) {
								Block autoCrafter = entity.getLocation().getBlock()
										.getRelative(((ItemFrame) entity).getAttachedFace());
								if (!autoCrafters.contains(autoCrafter)
										&& autoCrafter.getType().equals(Material.DISPENSER)) {
									autoCrafters.add(autoCrafter);
								}
							}
						}
					}
				}
			}
			if(autoCrafters.contains(e.getBlock())) {
				e.setCancelled(true);
			}
		}
	}
	
}
