/*  AutoCraft plugin
 *
 *  Copyright (C) 2021 Fliens
 *  Copyright (C) 2021 MrTransistor
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
import org.bukkit.plugin.Plugin;

public class EventListener implements Listener{
	
	public EventListener(Plugin plugin) { // changed to generic Plugin
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onItemMove(BlockDispenseEvent e) {
		if(e.getBlock().getType().equals(Material.DISPENSER)) {
			List<Block> autoCrafters = new ArrayList<>();

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
