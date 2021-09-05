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

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

public class AutoCraft extends JavaPlugin {

    boolean particles;
    String redstoneMode;
    long craftCooldown;

    ArrayList<Recipe> recipes = new ArrayList<>();

    @Override
    public void onEnable() {
        super.onEnable();
        getLogger().info("AutoCraft plugin started");

        saveDefaultConfig();
        craftCooldown = getConfig().getLong("craftCooldown");
        particles = getConfig().getBoolean("particles");
        redstoneMode = getConfig().getString("redstoneMode");

        recipes = collectRecipes();

        new EventListener(this);
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, () -> {
            ArrayList<Block> autoCrafters = collectAutoCrafters();

            for (final Block autocrafter : autoCrafters) {
                if (!redstoneMode.equalsIgnoreCase("disabled")) { // redstone powering type check
                    if ((redstoneMode.equalsIgnoreCase("indirect") && autocrafter.isBlockIndirectlyPowered()) || autocrafter.isBlockPowered()) {
                        continue;
                    }
                }
                handleAutoCrafter(autocrafter);
            }
        }, 0L, craftCooldown); // configurable cooldown
    }

    @Override
    public void onDisable() {
        super.onDisable();
        getLogger().info("AutoCraft plugin stopped");
    }

    /*
     * TODO: Switch to Event-based crafter collection during operation
     * Next method is too resource-heavy to execute every craft tick, so it eventually should be converted
     * to an Event-based system. We can check item frame manipulation events and add/remove crafters only when needed
     * thus removing the necessity to update crafter list every craft tick.
     */

    /**
     * This method returns an ArrayList of all autocrafters in the world
     *
     * @return ArrayList of autocrafters
     **/

    public static ArrayList<Block> collectAutoCrafters() {
        ArrayList<Block> autoCrafters = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) { // avixk`s fix
                if (entity.getType().equals(EntityType.ITEM_FRAME) || entity.getType().equals(EntityType.GLOW_ITEM_FRAME)) {
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
        return autoCrafters;
    }

    /**
     * This method returns an ArrayList of all available recipes (including extra)
     *
     * @return ArrayList of all recipes
     **/

    public ArrayList<Recipe> collectRecipes() {
        ArrayList<Recipe> recipes = new ArrayList<>();
        Iterator<Recipe> it = getServer().recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe.getResult().getType() == Material.FIREWORK_ROCKET)
                continue; // skipping duplicating recipe with no NBT
            recipes.add(recipe);
        }

        recipes.addAll(collectExtraRecipes());

        return recipes;
    }

    /**
     * This method returns an ArrayList of extra recipes
     *
     * @return ArrayList of extra recipes
     **/

    public ArrayList<Recipe> collectExtraRecipes() {
        ArrayList<Recipe> recipes = new ArrayList<>();

        ItemStack rocket_1 = new ItemStack(Material.FIREWORK_ROCKET, 3);
        FireworkMeta rocket_1_meta = (FireworkMeta) rocket_1.getItemMeta();
        rocket_1_meta.setPower(1);
        rocket_1.setItemMeta(rocket_1_meta);
        ShapelessRecipe rocket_1_recipe = new ShapelessRecipe(new NamespacedKey(this, "fliens.autocraft.firework_rocket_1"), rocket_1);
        rocket_1_recipe.addIngredient(Material.PAPER);
        rocket_1_recipe.addIngredient(1, Material.GUNPOWDER);

        ItemStack rocket_2 = new ItemStack(Material.FIREWORK_ROCKET, 3);
        FireworkMeta rocket_2_meta = (FireworkMeta) rocket_2.getItemMeta();
        rocket_2_meta.setPower(2);
        rocket_2.setItemMeta(rocket_2_meta);
        ShapelessRecipe rocket_2_recipe = new ShapelessRecipe(new NamespacedKey(this, "fliens.autocraft.firework_rocket_2"), rocket_2);
        rocket_2_recipe.addIngredient(Material.PAPER);
        rocket_2_recipe.addIngredient(2, Material.GUNPOWDER);

        ItemStack rocket_3 = new ItemStack(Material.FIREWORK_ROCKET, 3);
        FireworkMeta rocket_3_meta = (FireworkMeta) rocket_3.getItemMeta();
        rocket_3_meta.setPower(3);
        rocket_3.setItemMeta(rocket_3_meta);
        ShapelessRecipe rocket_3_recipe = new ShapelessRecipe(new NamespacedKey(this, "fliens.autocraft.firework_rocket_3"), rocket_3);
        rocket_3_recipe.addIngredient(Material.PAPER);
        rocket_3_recipe.addIngredient(3, Material.GUNPOWDER);

        recipes.add(rocket_1_recipe);
        recipes.add(rocket_2_recipe);
        recipes.add(rocket_3_recipe);

        return recipes;
    }

    /**
     * Main method that updates a specified autocrafter
     *
     * @param autocrafter specified autocrafter
     **/

    private void handleAutoCrafter(Block autocrafter) {
        Dispenser dispenser = (Dispenser) autocrafter.getState();
        BlockFace targetFace = ((org.bukkit.block.data.type.Dispenser) autocrafter.getBlockData()).getFacing();
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
            List<ItemStack> itemsrem = new ArrayList<>();
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

                        Material remMat = item.getType().getCraftingRemainingItem(); // fix for lost bottles/buckets by avixk
                        if (remMat != null && !remMat.isAir()) {
                            itemsrem.add(new ItemStack(remMat, count));
                        }
                    }
                }
            }
            for (ItemStack i : itemstmp)
                if (!sourceInv.containsAtLeast(i, i.getAmount()))
                    return;
            ItemStack result = getCraftResult(crafterItems);
            if (result == null)
                return;

            ArrayList<ItemStack> output = new ArrayList<>(); // Making a list of items to add to the output container
            output.add(result.clone());
            output.addAll(itemsrem);

            if (!addItemsIfCan(destinationInv, output)) // Trying to add
                return;

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
                            new Particle.DustOptions(Color.LIME, 0.2F));
        }
    }

    /**
     * This method adds items to an inventory
     *
     * @param inventory inventory to add items to
     * @param items     an ArrayList of items to add
     * @return true if items were added, false if items weren't added because some of them didn't fit
     **/

    private boolean addItemsIfCan(Inventory inventory, ArrayList<ItemStack> items) { // I think this needs explanation
        ItemStack[] mutableCont = inventory.getContents(); // We have to manually clone each ItemStack to prevent them from
        ItemStack[] contents = inventory.getContents(); // mutation when adding items. This is the backup inventory
        for (int i = 0; i < mutableCont.length; i++)
            contents[i] = mutableCont[i] == null ? null : mutableCont[i].clone();
        for (ItemStack item : items) {
            Map<Integer, ItemStack> left = inventory.addItem(item.clone()); // addItem() returns a map of items which didn`t fit
            if (!left.isEmpty()) {
                inventory.setStorageContents(contents);// if so, we rollback the inventory
                return false;
            }
        }
        return true;
    }

    /*
     * Warning! testIfCanFit() method should be only used if you don`t need to add items.
     * In cases when you have to add items please use addItemsIfCan()
     */

    /**
     * This method is similar to addItemsifCan() with only one difference - it doesn't add items
     *
     * @param inventory inventory to add items to
     * @param items     an ArrayList of items to add
     * @return true if items can fit into the inventory, false if not
     * @see AutoCraft#addItemsIfCan(Inventory inventory, ArrayList items)
     **/

    private boolean testIfCanFit(Inventory inventory, ArrayList<ItemStack> items) {
        ItemStack[] mutableCont = inventory.getContents();
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < mutableCont.length; i++)
            contents[i] = mutableCont[i] == null ? null : mutableCont[i].clone();
        for (ItemStack item : items) {
            Map<Integer, ItemStack> left = inventory.addItem(item.clone());
            if (!left.isEmpty()) {
                inventory.setStorageContents(contents);
                return false;
            }
        }
        inventory.setStorageContents(contents);
        return true;
    }

    private final Map<List<ItemStack>, ItemStack> cache = new HashMap<>();

    /**
     * This method returns a craft result for a list of input items
     *
     * @param items an ArrayList of input items
     * @return crafting result (ItemStack)
     **/

    private ItemStack getCraftResult(List<ItemStack> items) {
        if (cache.containsKey(items))
            return cache.get(items);

        if (items.size() != 9) { // list correct?
            return null;
        }
        boolean notNull = false;
        for (ItemStack itemstack : items) {
            if (itemstack != null) {
                notNull = true;
                break;
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

    /**
     * This method checks if a list of items matches a specified shapeless recipe
     *
     * @param choice specified recipe
     * @param items  a list of items to check
     * @return true if recipe matches, false if not
     **/

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
                    items.remove(item);
                    break;
                }
            }
            if (!match)
                return false;
        }
        items.removeAll(Arrays.asList(null, new ItemStack(Material.AIR)));
        return items.size() == 0;
    }

    /**
     * This method checks if a list of items matches a specified shaped recipe
     *
     * @param recipe specified recipe
     * @param items  a list of items to check
     * @return true if recipe matches, false if not
     **/

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
                itemsArray[i][j] = (ItemStack) tmpArray[i][j];
            }
        }
        ItemStack[][] itemsArrayMirrored = new ItemStack[itemsArray.length][itemsArray[0].length];
        for (int i = 0; i < itemsArray.length; i++) {
            int jPos = 0;
            for (int j = itemsArray[i].length - 1; j >= 0; j--) {
                itemsArrayMirrored[i][jPos] = itemsArray[i][j];
                jPos++;
            }
        }
        return match(itemsArray, recipeArray) || match(itemsArrayMirrored, recipeArray);
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
        ArrayList<Pos> positions = new ArrayList<>();
        for (int y = 0; y < array.length; y++)
            for (int x = 0; x < array[y].length; x++) {
                if (array[y][x] != null)
                    positions.add(new Pos(x, y));
            }

        Pos upperLeft = new Pos(array.length - 1, array[0].length - 1);
        Pos lowerRight = new Pos(0, 0);
        for (Pos pos : positions) {
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

    /**
     * This method returns a list of coordinates for particles around a block
     *
     * @param loc              block position
     * @param particleDistance distance between particles
     * @return a list of particle positions
     **/

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