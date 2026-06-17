package nyonio.botania_unbound.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.entity.monster.IMob;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.subtile.generating.SubTileShulkMeNot;

import java.util.List;

@Mixin(value = SubTileShulkMeNot.class, remap = false)
public abstract class MixinSubTileShulkMeNot extends SubTileGenerating {

    private static final int RADIUS = 8;

    /**
     * Overwrite onUpdate to only kill the target monster, not the shulker.
     *
     * @author Nyonio
     * @reason Shulk Me Not should not kill shulkers, only their targets
     */
    @Overwrite
    public void onUpdate() {
        super.onUpdate();

        int generate = getMaxMana();
        net.minecraft.world.World world = supertile.getWorld();
        net.minecraft.util.math.BlockPos pos = supertile.getPos();
        AxisAlignedBB aabb = new AxisAlignedBB(pos.add(-RADIUS, -RADIUS, -RADIUS), pos.add(RADIUS + 1, RADIUS + 1, RADIUS + 1));
        List<EntityShulker> shulkers = world.getEntitiesWithinAABB(EntityShulker.class, aabb);

        for (EntityShulker shulker : shulkers) {
            if (getMaxMana() - this.mana < generate) break;
            if (shulker.isDead || shulker.getDistanceSq(pos) > RADIUS * RADIUS) continue;

            EntityLivingBase target = shulker.getAttackTarget();
            if (target == null || !(target instanceof IMob) || target.isDead || target.getDistanceSq(pos) > RADIUS * RADIUS) continue;
            if (target.getActivePotionEffect(MobEffects.LEVITATION) == null) continue;

            target.setDead();
            // Do NOT kill the shulker - only kill the target

            for (int i = 0; i < 10; i++) {
                world.playSound(null, pos, SoundEvents.ENTITY_SHULKER_DEATH, SoundCategory.BLOCKS, 10F, (1F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, false, target.posX, target.posY, target.posZ, 100, 0.0, 0.0, 0.0, 0.0, 0);
                ws.spawnParticle(EnumParticleTypes.PORTAL, false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 40, 0.0, 0.0, 0.0, 1.0, 0);
            }

            this.mana += generate;
            sync();
        }
    }
}
