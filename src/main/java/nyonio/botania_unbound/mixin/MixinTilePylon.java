package nyonio.botania_unbound.mixin;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nyonio.botania_unbound.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.api.state.BotaniaStateProps;
import vazkii.botania.api.state.enums.PylonVariant;
import vazkii.botania.api.wand.IWandBindable;
import vazkii.botania.common.Botania;
import vazkii.botania.common.block.tile.TilePylon;

import javax.annotation.Nullable;

@Mixin(value = TilePylon.class, remap = false)
public abstract class MixinTilePylon implements IWandBindable {

    @Shadow private boolean activated;
    @Shadow private BlockPos centerPos;
    @Shadow private int ticks;

    private BlockPos bindPos = null;
    private BlockPos cachedSourcePos = null;
    private int sourceCacheTimer = 0;
    private boolean nbtLoaded = false;

    // ==================== Per-variant config helper ====================

    private ModConfig.PylonVariantConfig getVariantConfig() {
        TilePylon self = (TilePylon) (Object) this;
        if (self.getWorld() == null) return ModConfig.pylonPump.mana;
        PylonVariant variant = self.getWorld().getBlockState(self.getPos()).getValue(BotaniaStateProps.PYLON_VARIANT);
        switch (variant) {
            case NATURA: return ModConfig.pylonPump.natura;
            case GAIA:   return ModConfig.pylonPump.gaia;
            default:     return ModConfig.pylonPump.mana;
        }
    }

    // ==================== IWandBindable ====================

    @Override
    public boolean canSelect(EntityPlayer player, ItemStack wand, BlockPos pos, EnumFacing side) {
        return getVariantConfig().enabled;
    }

    @Override
    public boolean bindTo(EntityPlayer player, ItemStack wand, BlockPos pos, EnumFacing side) {
        ModConfig.PylonVariantConfig config = getVariantConfig();
        if (!config.enabled) return false;
        TilePylon self = (TilePylon) (Object) this;
        // Don't bind to the block directly below (that's the source)
        if (pos.equals(self.getPos().down())) {
            bindPos = null;
            clearBindNBT(self);
            self.markDirty();
            syncToClient(self);
            return false;
        }
        // Validate binding target
        if (!isValidBindingPos(pos, config)) {
            bindPos = null;
            clearBindNBT(self);
            self.markDirty();
            syncToClient(self);
            return false;
        }
        bindPos = pos;
        saveBindNBT(self);
        self.markDirty();
        syncToClient(self);
        return true;
    }

    @Nullable
    @Override
    public BlockPos getBinding() {
        if (!getVariantConfig().enabled) return null;
        return bindPos;
    }

    // ==================== NBT Persistence ====================

    private void saveBindNBT(TilePylon self) {
        NBTTagCompound data = self.getTileData();
        if (bindPos != null) {
            data.setInteger("botania_unbound:bindX", bindPos.getX());
            data.setInteger("botania_unbound:bindY", bindPos.getY());
            data.setInteger("botania_unbound:bindZ", bindPos.getZ());
        } else {
            data.removeTag("botania_unbound:bindX");
            data.removeTag("botania_unbound:bindY");
            data.removeTag("botania_unbound:bindZ");
        }
    }

    private void clearBindNBT(TilePylon self) {
        NBTTagCompound data = self.getTileData();
        data.removeTag("botania_unbound:bindX");
        data.removeTag("botania_unbound:bindY");
        data.removeTag("botania_unbound:bindZ");
    }

    private void syncToClient(TilePylon self) {
        World world = self.getWorld();
        if (!world.isRemote) {
            IBlockState state = world.getBlockState(self.getPos());
            world.notifyBlockUpdate(self.getPos(), state, state, 3);
        }
    }

    // ==================== Mana Transfer Logic ====================

    @Inject(method = "func_73660_a", at = @At("RETURN"))
    private void onTickEnd(CallbackInfo ci) {
        TilePylon self = (TilePylon) (Object) this;
        World world = self.getWorld();
        ModConfig.PylonVariantConfig config = getVariantConfig();
        if (!config.enabled) return;

        // Load NBT data on first tick (both sides for client highlight/HUD)
        if (!nbtLoaded) {
            nbtLoaded = true;
            NBTTagCompound persistedNbt = self.getTileData();
            if (persistedNbt.hasKey("botania_unbound:bindX")) {
                bindPos = new BlockPos(
                    persistedNbt.getInteger("botania_unbound:bindX"),
                    persistedNbt.getInteger("botania_unbound:bindY"),
                    persistedNbt.getInteger("botania_unbound:bindZ")
                );
            }
        }

        if (bindPos == null) return;

        if (!world.isRemote) {
            // Server side: transfer mana
            IManaReceiver source = getSource(config);
            if (source == null || source.getCurrentMana() <= 0) return;

            IManaReceiver target = getBoundTarget();
            if (target == null) return;
            if (target.isFull()) return;

            int extractAmount = Math.min(config.transferRate, source.getCurrentMana());
            int reachedAmount = (int) Math.round(extractAmount * (1.0 - config.lossRatio));
            if (reachedAmount <= 0) return;

            // Give mana to target first, then check how much was actually received
            int oldTargetMana = target.getCurrentMana();
            target.recieveMana(reachedAmount);
            int actuallyReceived = target.getCurrentMana() - oldTargetMana;

            if (actuallyReceived <= 0) {
                // Target rejected the mana, undo nothing (target didn't change)
                // and don't deduct from source
                return;
            }

            // Deduct from source proportionally to actual received amount
            if (actuallyReceived < reachedAmount) {
                extractAmount = (int) Math.ceil((float) extractAmount * actuallyReceived / reachedAmount);
            }
            source.recieveMana(-extractAmount);
        } else {
            // Client side: spawn particles only if target is valid and not full
            if (config.particles) {
                IManaReceiver source = getSource(config);
                IManaReceiver target = getBoundTarget();
                if (source != null && source.getCurrentMana() > 0 && target != null && !target.isFull()) {
                    spawnTransferParticles();
                }
            }
        }
    }

    // ==================== Helper Methods ====================

    private boolean isValidBindingPos(BlockPos pos, ModConfig.PylonVariantConfig config) {
        TilePylon self = (TilePylon) (Object) this;
        World world = self.getWorld();
        if (!world.isBlockLoaded(pos)) return false;

        // Check distance
        double dist = self.getPos().distanceSq(pos);
        double maxDist = config.maxDistance;
        if (dist > maxDist * maxDist) return false;

        // Check if target is a mana receiver
        TileEntity te = world.getTileEntity(pos);
        return te instanceof IManaReceiver;
    }

    @Nullable
    private IManaReceiver getSource(ModConfig.PylonVariantConfig config) {
        TilePylon self = (TilePylon) (Object) this;
        World world = self.getWorld();

        // Refresh cache every 20 ticks
        sourceCacheTimer++;
        if (cachedSourcePos == null || sourceCacheTimer >= 20) {
            sourceCacheTimer = 0;
            cachedSourcePos = null;

            if (!config.verticalStacking) {
                // Simple: just check directly below
                TileEntity te = world.getTileEntity(self.getPos().down());
                if (te instanceof IManaReceiver) {
                    cachedSourcePos = self.getPos().down();
                }
            } else {
                // Vertical stacking: scan down through pylons to find a mana receiver
                BlockPos checkPos = self.getPos().down();
                while (checkPos.getY() >= 0) {
                    TileEntity te = world.getTileEntity(checkPos);
                    if (te instanceof IManaReceiver) {
                        cachedSourcePos = checkPos;
                        break;
                    } else if (te instanceof TilePylon) {
                        checkPos = checkPos.down();
                    } else {
                        break;
                    }
                }
            }
        }

        if (cachedSourcePos != null) {
            TileEntity te = world.getTileEntity(cachedSourcePos);
            if (te instanceof IManaReceiver) {
                return (IManaReceiver) te;
            }
        }
        return null;
    }

    @Nullable
    private IManaReceiver getBoundTarget() {
        if (bindPos == null) return null;
        TilePylon self = (TilePylon) (Object) this;
        World world = self.getWorld();
        if (!world.isBlockLoaded(bindPos)) return null;
        TileEntity te = world.getTileEntity(bindPos);
        if (te instanceof IManaReceiver) return (IManaReceiver) te;
        return null;
    }

    private void spawnTransferParticles() {
        TilePylon self = (TilePylon) (Object) this;
        if (bindPos == null) return;

        net.minecraft.block.state.IBlockState state = self.getWorld().getBlockState(self.getPos());
        PylonVariant variant = state.getValue(BotaniaStateProps.PYLON_VARIANT);
        ModConfig.PylonVariantConfig config = getVariantConfig();

        float r, g, b;
        switch (variant) {
            case MANA:  r = 0.2F; g = 1.0F; b = 0.2F; break;
            case NATURA: r = 0.5F; g = 1.0F; b = 0.5F; break;
            case GAIA:  r = 1.0F; g = 0.5F; b = 1.0F; break;
            default:    r = 0.2F; g = 1.0F; b = 0.2F; break;
        }

        double srcX = self.getPos().getX() + 0.5;
        double srcY = self.getPos().getY() + 0.5;
        double srcZ = self.getPos().getZ() + 0.5;
        double dstX = bindPos.getX() + 0.5;
        double dstY = bindPos.getY() + 0.5;
        double dstZ = bindPos.getZ() + 0.5;

        int strength = config.particleStrength;
        for (int i = 0; i < strength; i++) {
            double t = Math.random();
            double px = srcX + (dstX - srcX) * t + (Math.random() - 0.5) * 0.3;
            double py = srcY + (dstY - srcY) * t + (Math.random() - 0.5) * 0.3;
            double pz = srcZ + (dstZ - srcZ) * t + (Math.random() - 0.5) * 0.3;
            Botania.proxy.wispFX(px, py, pz, r, g, b, 0.15F + (float) Math.random() * 0.1F);
        }
    }
}
