package fliens.autocraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.bukkit.configuration.file.FileConfiguration;
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

	boolean particles = true;
	boolean ignoresRedstone = false;

	@Override
	public void onEnable() {
		System.out.println("Loaded AutoCraft by Fliens");
		super.onEnable();

		FileConfiguration config = this.getConfig();
		config.addDefault("particles", "true");
		config.addDefault("ignoresRedstone", "false");
		config.options().copyDefaults(true);
		saveConfig();	
		particles = config.getBoolean("particles");
		ignoresRedstone = config.getBoolean("ignoresRedstone");

		new EventListener(this);
		BukkitScheduler scheduler = getServer().getScheduler();
		scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
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
				for (Block autocrafter : autoCrafters) {
					if(!ignoresRedstone) {
					if (!autocrafter.isBlockPowered()) // make sure no redstone signal
						handleAutoCrafter(autocrafter);
					}else {
						handleAutoCrafter(autocrafter);
					}
				}
			}
		}, 0L, 20L);
	}

	@Override
	public void onDisable() {
		super.onDisable();
	}

	private boolean handleAutoCrafter(Block autocrafter) {
		Dispenser dispenser = (Dispenser) autocrafter.getState();
		BlockFace targetFace = ((org.bukkit.material.Dispenser) autocrafter.getState().getData()).getFacing();
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
			if (!crafterItems.stream().anyMatch(i -> i != null)) // test if crafter is empty
				return false;
			List<ItemStack> itemstmp = new ArrayList<>();
			for (ItemStack item : crafterItems) {
				if (item != null) {
					if (!itemstmp.stream().anyMatch(i -> i.isSimilar(item))) {
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
					return false;
			ItemStack result = getCraftResult(crafterItems);
			if (result == null)
				return false;
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
				return false;
			destinationInv.addItem(result);
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
							new Particle.DustOptions(Color.GREEN, 0.2F));
		}
		return true;
	}

	private Map<List<ItemStack>, ItemStack> cache = new HashMap<List<ItemStack>, ItemStack>();
	private List<Recipe> recipes;

	private ItemStack getCraftResult(List<ItemStack> items) {
		if (cache.containsKey(items))
			return cache.get(items);
		recipes = new ArrayList<Recipe>();
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
			}
		}
		if (!notNull) {
			return null;
		}

		ItemStack result = null;
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
		return result;
	}

	private boolean matchesShapeless(List<RecipeChoice> choice, List<ItemStack> items) {
		items = new ArrayList<ItemStack>(items);
		for (RecipeChoice c : choice) {
			boolean match = false;
			for (int i = 0; i < items.size(); i++) {
				ItemStack item = items.get(i);
				if (item == null || item.getType() == Material.AIR)
					continue;
				if (c.test(item)) {
					match = true;
					items.remove(items.indexOf(item));
					break;
				}
			}
			if (!match)
				return false;
		}
		items.removeAll(Arrays.asList(null, new ItemStack(Material.AIR)));
		return (items.size() == 0 && !items.contains(Material.AIR) && !items.contains(null));
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
				itemsArray[i][j] = ItemStack.class.cast(tmpArray[i][j]);
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
		ArrayList<Pos> positionen = new ArrayList<Pos>();
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
		List<Location> result = new ArrayList<Location>();
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

class Pos {
	int x = 0;
	int y = 0;

	public Pos(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public String toString() {
		return "[" + x + "|" + y + "]";
	}
}
