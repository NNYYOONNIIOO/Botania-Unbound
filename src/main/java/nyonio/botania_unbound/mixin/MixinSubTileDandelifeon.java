package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.DandelifeonState;
import nyonio.botania_unbound.ICellNumberAccessor;
import nyonio.botania_unbound.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.tile.TileCell;
import vazkii.botania.common.block.subtile.generating.SubTileDandelifeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mixin(value = SubTileDandelifeon.class, remap = false)
public abstract class MixinSubTileDandelifeon extends SubTileGenerating {

    private static final int RANGE = 12;
    private static final int MANA_PER_GEN = 60;

    /**
     * Overwrite onUpdate to support configurable cycle speed, skip on full mana, and reform mechanism.
     */
    @Overwrite
    public void onUpdate() {
        super.onUpdate();

        if (supertile.getWorld().isRemote) return;
        if (redstoneSignal <= 0) return;

        int speed = ModConfig.dandelifeon.enableSpeedAdjust ? ModConfig.dandelifeon.cycleSpeed : 10;
        if (ticksExisted % speed != 0) return;

        // Skip on full mana
        if (ModConfig.dandelifeon.skipOnFullMana && mana >= getMaxMana()) return;

        if (ModConfig.dandelifeon.enableReform) {
            runReformSimulation();
        } else {
            // Use vanilla simulation but with configurable max generation
            runVanillaSimulation();
        }
    }

    /**
     * Vanilla simulation with configurable max generation.
     */
    private void runVanillaSimulation() {
        int maxGen = ModConfig.dandelifeon.maxGeneration;
        int[][] table = getCellTable();
        List<int[]> changes = new ArrayList<>();
        boolean wipe = false;

        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < table[0].length; j++) {
                int gen = table[i][j];
                int adj = getAdjCells(table, i, j);
                int newVal = gen;

                if (adj < 2 || adj > 3) {
                    newVal = -1;
                } else {
                    if (adj == 3 && gen == -1) {
                        newVal = getSpawnCellGeneration(table, i, j);
                    } else if (gen > -1) {
                        newVal = gen + 1;
                    }
                }

                int xdist = Math.abs(i - RANGE);
                int zdist = Math.abs(j - RANGE);
                int allowDist = 1;
                if (xdist <= allowDist && zdist <= allowDist && newVal > -1) {
                    gen = newVal;
                    newVal = gen == 1 ? -1 : -2;
                }

                if (newVal != gen) {
                    changes.add(new int[]{i, j, newVal, gen});
                    if (newVal == -2) wipe = true;
                }
            }
        }

        BlockPos pos = supertile.getPos();
        for (int[] change : changes) {
            BlockPos pos_ = pos.add(-RANGE + change[0], 0, -RANGE + change[1]);
            int val = change[2];
            if (val != -2 && wipe) val = -1;
            int old = change[3];
            setBlockForGeneration(pos_, val, old, maxGen);
        }
    }

    /**
     * Reform simulation: 2048-style cell merging with redstone direction push.
     */
    private void runReformSimulation() {
        World world = supertile.getWorld();
        BlockPos flowerPos = supertile.getPos();

        // Detect redstone signal direction (only N/S/E/W, not up/down)
        EnumFacing pushDir = detectRedstoneDirection(world, flowerPos);
        if (pushDir == null) return; // No valid redstone direction, don't run

        // Get cell number table (25x25)
        int diam = RANGE * 2 + 1;
        int[][] numTable = new int[diam][diam]; // 0 = no cell, >0 = cell number
        boolean[][] validCell = new boolean[diam][diam]; // true = belongs to this flower

        for (int i = 0; i < diam; i++) {
            for (int j = 0; j < diam; j++) {
                BlockPos cellPos = flowerPos.add(-RANGE + i, 0, -RANGE + j);
                TileEntity te = world.getTileEntity(cellPos);
                if (te instanceof TileCell) {
                    TileCell cell = (TileCell) te;
                    // Include ALL cell blocks in range, not just those belonging to this flower
                    int cellNum = ((ICellNumberAccessor) cell).botania_unbound$getNumber();
                    if (cellNum <= 0) cellNum = ModConfig.dandelifeon.initialCellNumber;
                    numTable[i][j] = cellNum;
                    validCell[i][j] = true;
                }
            }
        }

        // Check for external interference (pushed, broken, non-Dandelifeon removal)
        // If any previously valid cell is now invalid, wipe all cells
        // (This is handled by TileCell's isSameFlower check - cells moved by pistons self-destruct)

        // Push cells in the redstone direction and merge (2048 logic)
        int di = 0, dj = 0;
        switch (pushDir) {
            case NORTH: dj = -1; break; // -Z
            case SOUTH: dj = 1; break;  // +Z
            case EAST: di = 1; break;   // +X
            case WEST: di = -1; break;  // -X
            default: return;
        }

        // Determine iteration order based on push direction
        // We need to process cells from the edge they're being pushed toward
        int totalMana = 0;
        int maxGen = ModConfig.dandelifeon.maxGeneration;
        boolean[][] merged = new boolean[diam][diam]; // Track which cells have been merged into

        // Process rows/columns in the direction of push
        if (di != 0) {
            // Pushing along X axis
            for (int j = 0; j < diam; j++) {
                // Collect non-zero cells in this column
                List<int[]> cells = new ArrayList<>(); // [i, number]
                if (di > 0) {
                    // Push East: collect from right (edge) to left
                    for (int i = diam - 1; i >= 0; i--) {
                        if (numTable[i][j] > 0) cells.add(new int[]{i, numTable[i][j]});
                    }
                } else {
                    // Push West: collect from left (edge) to right
                    for (int i = 0; i < diam; i++) {
                        if (numTable[i][j] > 0) cells.add(new int[]{i, numTable[i][j]});
                    }
                }

                // Clear all cells in this row
                for (int i = 0; i < diam; i++) {
                    numTable[i][j] = 0;
                }

                // Place cells at the edge and merge
                int startPos = di > 0 ? diam - 1 : 0;
                int pos = startPos;
                int prevNum = 0;
                int prevPos = -1;

                for (int[] cell : cells) {
                    int num = cell[1];
                    if (prevNum == num && prevPos >= 0 && !merged[prevPos][j]) {
                        // Merge: same number, combine
                        int mergedNum = num + num;
                        numTable[prevPos][j] = mergedNum;
                        merged[prevPos][j] = true;
                        // Produce mana: equivalent to吞噬 mergedNum 周期细胞
                        int manaVal = Math.min(maxGen, mergedNum) * MANA_PER_GEN;
                        totalMana += manaVal;
                        prevNum = 0;
                        prevPos = -1;
                    } else {
                        numTable[pos][j] = num;
                        prevNum = num;
                        prevPos = pos;
                        pos -= di;
                    }
                }
            }
        } else {
            // Pushing along Z axis
            for (int i = 0; i < diam; i++) {
                List<int[]> cells = new ArrayList<>();
                if (dj > 0) {
                    // Push South: collect from bottom (edge) to top
                    for (int j = diam - 1; j >= 0; j--) {
                        if (numTable[i][j] > 0) cells.add(new int[]{j, numTable[i][j]});
                    }
                } else {
                    // Push North: collect from top (edge) to bottom
                    for (int j = 0; j < diam; j++) {
                        if (numTable[i][j] > 0) cells.add(new int[]{j, numTable[i][j]});
                    }
                }

                for (int j = 0; j < diam; j++) {
                    numTable[i][j] = 0;
                }

                int startPos = dj > 0 ? diam - 1 : 0;
                int pos = startPos;
                int prevNum = 0;
                int prevPos = -1;

                for (int[] cell : cells) {
                    int num = cell[1];
                    if (prevNum == num && prevPos >= 0 && !merged[i][prevPos]) {
                        int mergedNum = num + num;
                        numTable[i][prevPos] = mergedNum;
                        merged[i][prevPos] = true;
                        int manaVal = Math.min(maxGen, mergedNum) * MANA_PER_GEN;
                        totalMana += manaVal;
                        prevNum = 0;
                        prevPos = -1;
                    } else {
                        numTable[i][pos] = num;
                        prevNum = num;
                        prevPos = pos;
                        pos -= dj;
                    }
                }
            }
        }

        // Apply changes to the world
        // First, remove all existing cells in range
        DandelifeonState.isDandelifeonRemoving = true;
        for (int i = 0; i < diam; i++) {
            for (int j = 0; j < diam; j++) {
                BlockPos cellPos = flowerPos.add(-RANGE + i, 0, -RANGE + j);
                if (validCell[i][j]) {
                    world.setBlockToAir(cellPos);
                }
            }
        }
        DandelifeonState.isDandelifeonRemoving = false;

        // Then, place cells with new numbers
        for (int i = 0; i < diam; i++) {
            for (int j = 0; j < diam; j++) {
                if (numTable[i][j] > 0) {
                    BlockPos cellPos = flowerPos.add(-RANGE + i, 0, -RANGE + j);
                    IBlockState stateAt = world.getBlockState(cellPos);
                    if (stateAt.getBlock().isAir(stateAt, world, cellPos)) {
                        world.setBlockState(cellPos, ModBlocks.cellBlock.getDefaultState());
                        TileEntity te = world.getTileEntity(cellPos);
                        if (te instanceof TileCell) {
                            ((TileCell) te).setGeneration(supertile, numTable[i][j]);
                            ((ICellNumberAccessor) te).botania_unbound$setNumber(numTable[i][j]);
                        }
                    }
                }
            }
        }

        // Spawn a new cell at a random empty position
        spawnRandomCell(world, flowerPos, numTable, diam);

        // Add mana with expand buffer support
        if (totalMana > 0) {
            if (ModConfig.dandelifeon.expandManaBuffer && mana + totalMana > getMaxMana()) {
                mana += totalMana;
            } else {
                mana = Math.min(getMaxMana(), mana + totalMana);
            }
            sync();
        }
    }

    /**
     * Detect redstone signal direction. Only accepts N/S/E/W, not up/down.
     * Returns the direction from which the strongest redstone signal comes.
     */
    private EnumFacing detectRedstoneDirection(World world, BlockPos pos) {
        int maxPower = 0;
        EnumFacing bestDir = null;

        for (EnumFacing dir : EnumFacing.HORIZONTALS) {
            BlockPos neighbor = pos.offset(dir);
            int power = world.getRedstonePower(neighbor, dir);
            if (power > maxPower) {
                maxPower = power;
                bestDir = dir;
            }
        }

        return bestDir;
    }

    /**
     * Spawn a new cell at a random empty position within range.
     */
    private void spawnRandomCell(World world, BlockPos flowerPos, int[][] numTable, int diam) {
        List<int[]> emptyPositions = new ArrayList<>();
        for (int i = 0; i < diam; i++) {
            for (int j = 0; j < diam; j++) {
                if (numTable[i][j] == 0) {
                    BlockPos cellPos = flowerPos.add(-RANGE + i, 0, -RANGE + j);
                    IBlockState stateAt = world.getBlockState(cellPos);
                    if (stateAt.getBlock().isAir(stateAt, world, cellPos)) {
                        emptyPositions.add(new int[]{i, j});
                    }
                }
            }
        }

        if (!emptyPositions.isEmpty()) {
            Random rand = world.rand;
            int[] chosen = emptyPositions.get(rand.nextInt(emptyPositions.size()));
            BlockPos cellPos = flowerPos.add(-RANGE + chosen[0], 0, -RANGE + chosen[1]);
            world.setBlockState(cellPos, ModBlocks.cellBlock.getDefaultState());
            TileEntity te = world.getTileEntity(cellPos);
            if (te instanceof TileCell) {
                int initialNum = ModConfig.dandelifeon.initialCellNumber;
                ((TileCell) te).setGeneration(supertile, initialNum);
                ((ICellNumberAccessor) te).botania_unbound$setNumber(initialNum);
            }
        }
    }

    /**
     * Set block for generation with configurable max generation.
     */
    private void setBlockForGeneration(BlockPos pos, int gen, int prevGen, int maxGen) {
        World world = supertile.getWorld();
        IBlockState stateAt = world.getBlockState(pos);
        Block blockAt = stateAt.getBlock();
        TileEntity tile = world.getTileEntity(pos);

        if (gen == -2) {
            int val = Math.min(maxGen, prevGen) * MANA_PER_GEN;
            if (ModConfig.dandelifeon.expandManaBuffer && mana + val > getMaxMana()) {
                mana += val;
            } else {
                mana = Math.min(getMaxMana(), mana + val);
            }
        } else if (blockAt == ModBlocks.cellBlock) {
            if (gen < 0) {
                DandelifeonState.isDandelifeonRemoving = true;
                world.setBlockToAir(pos);
                DandelifeonState.isDandelifeonRemoving = false;
            } else {
                ((TileCell) tile).setGeneration(supertile, gen);
            }
        } else if (gen >= 0 && blockAt.isAir(stateAt, supertile.getWorld(), pos)) {
            world.setBlockState(pos, ModBlocks.cellBlock.getDefaultState());
            tile = world.getTileEntity(pos);
            ((TileCell) tile).setGeneration(supertile, gen);
        }
    }

    // ========== Helper methods (same as vanilla) ==========

    int[][] getCellTable() {
        int diam = RANGE * 2 + 1;
        int[][] table = new int[diam][diam];
        BlockPos pos = supertile.getPos();
        for (int i = 0; i < diam; i++)
            for (int j = 0; j < diam; j++) {
                BlockPos pos_ = pos.add(-RANGE + i, 0, -RANGE + j);
                table[i][j] = getCellGeneration(pos_);
            }
        return table;
    }

    int getCellGeneration(BlockPos pos) {
        TileEntity tile = supertile.getWorld().getTileEntity(pos);
        if (tile instanceof TileCell)
            return ((TileCell) tile).isSameFlower(supertile) ? ((TileCell) tile).getGeneration() : 0;
        return -1;
    }

    int getAdjCells(int[][] table, int x, int z) {
        int count = 0;
        int[][] adjacent = new int[][]{
            {-1, -1}, {-1, +0}, {-1, +1},
            {+0, +1}, {+1, +1}, {+1, +0},
            {+1, -1}, {+0, -1}
        };
        for (int[] shift : adjacent) {
            int xp = x + shift[0];
            int zp = z + shift[1];
            if (!isOffBounds(table, xp, zp) && table[xp][zp] >= 0)
                count++;
        }
        return count;
    }

    int getSpawnCellGeneration(int[][] table, int x, int z) {
        int max = -1;
        int[][] adjacent = new int[][]{
            {-1, -1}, {-1, +0}, {-1, +1},
            {+0, +1}, {+1, +1}, {+1, +0},
            {+1, -1}, {+0, -1}
        };
        for (int[] shift : adjacent) {
            int xp = x + shift[0];
            int zp = z + shift[1];
            if (!isOffBounds(table, xp, zp) && table[xp][zp] > max)
                max = table[xp][zp];
        }
        return max == -1 ? -1 : max + 1;
    }

    boolean isOffBounds(int[][] table, int x, int z) {
        return x < 0 || z < 0 || x >= table.length || z >= table[0].length;
    }

    /**
     * Overwrite getMaxMana to support expandable buffer.
     */
    @Overwrite
    public int getMaxMana() {
        return 50000;
    }
}
