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

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class AutoCraft extends JavaPlugin {

	boolean particles;
	String redstoneMode;
	long craftCooldown;

	@Override
	public void onEnable() {
		super.onEnable();
		getLogger().info("AutoCraft plugin started"); // Using logger instead of System.out

		saveDefaultConfig(); // using default config file instead of hard-coding defaults
		craftCooldown = getConfig().getLong("craftCooldown"); // added more config options
		particles = getConfig().getBoolean("particles");
		redstoneMode = getConfig().getString("redstoneMode");

		new EventListener(this);
		BukkitScheduler scheduler = getServer().getScheduler();
		scheduler.scheduleSyncRepeatingTask(this, () -> {
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
			} // redstone powering type check
			for (final Block autocrafter : autoCrafters) {
				if (!redstoneMode.equalsIgnoreCase("disabled")) {
					if ((redstoneMode.equalsIgnoreCase("indirect") && autocrafter.isBlockIndirectlyPowered()) || autocrafter.isBlockPowered()) {
						continue;
					}
				}
				handleAutoCrafter(autocrafter);
			}
		}, 0L, craftCooldown);// configurable cooldown
	}

	@Override
	public void onDisable() {
		super.onDisable();
		getLogger().info("AutoCraft plugin stopped");
	}

	private void handleAutoCrafter(Block autocrafter) { // changed to void because not using the returned value
		Dispenser dispenser = (Dispenser) autocrafter.getState();
		BlockFace targetFace = ((org.bukkit.block.data.type.Dispenser) autocrafter.getBlockData()).getFacing(); // removed deprecated material.Dispenser
		BlockState source = new Location(autocrafter.getWorld(),
				autocrafter.getX() + targetFace.getOppositeFace().getModX(),
				autocrafter.getY() + targetFace.getOppositeFace().getModY(),
				autocrafter.getZ() + targetFace.getOppositeFace().getModZ()).getBlock().getState();
		BlockState destination = new Location(autocrafter.getWorld(), autocrafter.getX() + targetFace.getModX(),
				autocrafter.getY() + targetFace.getModY(), autocrafter.getZ() + targetFace.getModZ()).getBlock()
						.getState();
		if (source instanceof InventoryHolder && destination instanceof InventoryHolder) {
			Inventory sourceInv = ((InventoryHolder) source).getInventory();
			Inventory destinationInv = ((InventoryHolder) destination).getInventory();
			List<ItemStack> crafterItems = new ArrayList<>(Arrays.asList(dispenser.getInventory().getContents()));
			if (crafterItems.stream().noneMatch(Objects::nonNull)) // test if crafter is empty
				return;
			List<ItemStack> itemstmp = new ArrayList<>();
			for (ItemStack item : crafterItems) {
				if (item != null) {
					if (itemstmp.stream().noneMatch(i -> i.isSimilar(item))) {
						int count = 0;
						for (ItemStack jtem : crafterItems) {
							if (jtem != null) {
								if (jtem.isSimilar(item)) {
									count += 1;
								}
							}
						}
						ItemStack tmpitem = new ItemStack(item);
						tmpitem.setAmount(count);
						itemstmp.add(tmpitem);
					}
				}
			}
			for (ItemStack i : itemstmp)
				if (!sourceInv.containsAtLeast(i, i.getAmount()))
					return;
			ItemStack result = getCraftResult(crafterItems);
			if (result == null)
				return;
			boolean destHasSpace = false;
			for (ItemStack destItem : destinationInv.getContents()) {
				if (destItem == null) {
					destHasSpace = true;
					break;
				} else if (destItem.isSimilar(result)) {
					if (destItem.getAmount() + result.getAmount() <= destItem.getMaxStackSize()) {
						destHasSpace = true;
						break;
					}
				}
			}
			if (!destHasSpace)// success
				return;
			destinationInv.addItem(result.clone()); // Fix for "Paper" implementation of addItem which rewrites the 'result'
			for (ItemStack item : itemstmp) {
				for (int i = 0; i < item.getAmount(); i++) {
					for (ItemStack sourceItem : sourceInv.getContents()) {
						if (sourceItem != null) {
							if (sourceItem.isSimilar(item)) {
								sourceItem.setAmount(sourceItem.getAmount() - 1);
								break;
							}
						}
					}
				}
			}
			if (particles)
				for (Location loc : getHollowCube(autocrafter.getLocation(), 0.05))
					loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0,
							new Particle.DustOptions(Color.LIME, 0.2F)); // changed the color to be more vibrant
		}
	}

	private final Map<List<ItemStack>, ItemStack> cache = new HashMap<>();

	private ItemStack getCraftResult(List<ItemStack> items) {
		if (cache.containsKey(items))
			return cache.get(items);
		List<Recipe> recipes = new ArrayList<>(); // moved to local from private global
		Iterator<Recipe> it = getServer().recipeIterator();
		while (it.hasNext()) {
			Recipe rec = it.next();
			recipes.add(rec);
		}

		if (items.size() != 9) { // list correct?
			return null;
		}
		boolean notNull = false;
		for (ItemStack itemstack : items) {
			if (itemstack != null) {
				notNull = true;
				break; // added fast break when found an item
			}
		}
		if (!notNull) {
			return null;
		}

		ItemStack result;
		for (Recipe recipe : recipes) {
			if (recipe instanceof ShapelessRecipe) { // shapeless recipe
				result = matchesShapeless(((ShapelessRecipe) recipe).getChoiceList(), items) ? recipe.getResult()
						: null;
				if (result != null) {
					cache.put(items, result);
					return result;
				}
			} else if (recipe instanceof ShapedRecipe) { // shaped recipe
				result = matchesShaped((ShapedRecipe) recipe, items) ? recipe.getResult() : null;
				if (result != null) {
					cache.put(items, result);
					return result;
				}
			}
		}
		return null;
	}

	private boolean matchesShapeless(List<RecipeChoice> choice, List<ItemStack> items) {
		items = new ArrayList<>(items);
		for (RecipeChoice c : choice) {
			boolean match = false;
			for (int i = 0; i < items.size(); i++) {
				ItemStack item = items.get(i);
				if (item == null || item.getType() == Material.AIR)
					continue;
				if (c.test(item)) {
					match = true;
					items.remove(item); // removing by the object
					break;
				}
			}
			if (!match)
				return false;
		}
		items.removeAll(Arrays.asList(null, new ItemStack(Material.AIR)));
		return items.size() == 0; // removed always true statements
	}

	private boolean matchesShaped(ShapedRecipe recipe, List<ItemStack> items) {
		RecipeChoice[][] recipeArray = new RecipeChoice[recipe.getShape().length][recipe.getShape()[0].length()];
		for (int i = 0; i < recipe.getShape().length; i++) {
			for (int j = 0; j < recipe.getShape()[i].length(); j++) {
				recipeArray[i][j] = recipe.getChoiceMap().get(recipe.getShape()[i].toCharArray()[j]);
			}
		}

		int counter = 0;
		ItemStack[][] itemsArray = new ItemStack[3][3];
		for (int i = 0; i < itemsArray.length; i++) {
			for (int j = 0; j < itemsArray[i].length; j++) {
				itemsArray[i][j] = items.get(counter);
				counter++;
			}
		}

		// itemsArray manipulation
		Object[][] tmpArray = reduceArray(itemsArray);
		itemsArray = new ItemStack[tmpArray.length][tmpArray[0].length];
		for (int i = 0; i < tmpArray.length; i++) {
			for (int j = 0; j < tmpArray[i].length; j++) {
				itemsArray[i][j] = (ItemStack) tmpArray[i][j]; // native Java casting
			}
		}
		ItemStack[][] itemsArrayGespiegelt = new ItemStack[itemsArray.length][itemsArray[0].length];
		for (int i = 0; i < itemsArray.length; i++) {
			int jPos = 0;
			for (int j = itemsArray[i].length - 1; j >= 0; j--) {
				itemsArrayGespiegelt[i][jPos] = itemsArray[i][j];
				jPos++;
			}
		}
		return match(itemsArray, recipeArray) || match(itemsArrayGespiegelt, recipeArray);
	}

	private boolean match(ItemStack[][] itemsArray, RecipeChoice[][] recipeArray) {
		boolean match = true;
		if (itemsArray.length == recipeArray.length && itemsArray[0].length == recipeArray[0].length) {
			for (int i = 0; i < recipeArray.length; i++) {
				for (int j = 0; j < recipeArray[0].length; j++) {
					if (recipeArray[i][j] != null && itemsArray[i][j] != null) {
						if (!recipeArray[i][j].test(itemsArray[i][j])) {
							match = false;
							break;
						}
					} else if ((recipeArray[i][j] == null && itemsArray[i][j] != null)
							|| (recipeArray[i][j] != null && itemsArray[i][j] == null)) {
						match = false;
						break;
					}
				}
			}
			return match;
		}
		return false;
	}

	private static Object[][] reduceArray(Object[][] array) {
		ArrayList<Pos> positionen = new ArrayList<>();
		for (int y = 0; y < array.length; y++)
			for (int x = 0; x < array[y].length; x++) {
				if (array[y][x] != null)
					positionen.add(new Pos(x, y));
			}

		Pos upperLeft = new Pos(array.length - 1, array[0].length - 1);
		Pos lowerRight = new Pos(0, 0);
		for (Pos pos : positionen) {
			if (pos.y < upperLeft.y)
				upperLeft.y = pos.y;
			if (pos.x < upperLeft.x)
				upperLeft.x = pos.x;
			if (pos.y > lowerRight.y)
				lowerRight.y = pos.y;
			if (pos.x > lowerRight.x)
				lowerRight.x = pos.x;
		}
		Object[][] clean = new Object[(lowerRight.y - upperLeft.y) + 1][(lowerRight.x - upperLeft.x) + 1];
		int cleanyY = 0;
		for (int y = upperLeft.y; y < lowerRight.y + 1; y++) {
			int cleanxX = 0;
			for (int x = upperLeft.x; x < lowerRight.x + 1; x++) {
				clean[cleanyY][cleanxX] = array[y][x];
				cleanxX++;
			}
			cleanyY++;
		}
		return clean;
	}

	public List<Location> getHollowCube(Location loc, double particleDistance) {
		List<Location> result = new ArrayList<>();
		World world = loc.getWorld();
		double minX = loc.getBlockX();
		double minY = loc.getBlockY();
		double minZ = loc.getBlockZ();
		double maxX = loc.getBlockX() + 1;
		double maxY = loc.getBlockY() + 1;
		double maxZ = loc.getBlockZ() + 1;

		for (double x = minX; x <= maxX; x = Math.round((x + particleDistance) * 1e2) / 1e2) {
			for (double y = minY; y <= maxY; y = Math.round((y + particleDistance) * 1e2) / 1e2) {
				for (double z = minZ; z <= maxZ; z = Math.round((z + particleDistance) * 1e2) / 1e2) {
					int components = 0;
					if (x == minX || x == maxX)
						components++;
					if (y == minY || y == maxY)
						components++;
					if (z == minZ || z == maxZ)
						components++;
					if (components >= 2) {
						result.add(new Location(world, x, y, z));
					}
				}
			}
		}
		return result;
	}
}