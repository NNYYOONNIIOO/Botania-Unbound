package nyonio.botania_unbound;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import vazkii.botania.api.subtile.SubTileEntity;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.subtile.generating.SubTileShulkMeNot;
import vazkii.botania.common.block.tile.TileSpecialFlower;

import java.lang.reflect.Field;
import java.util.List;

public class BubbellEventHandler {

    private static Field supertileField;
    private static Field ticksExistedField;
    private static Field manaField;

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        if (!ModConfig.bubbell.swallowShulkerTargets && !ModConfig.bubbell.swallowShulkerBullets && !ModConfig.bubbell.shulkerRegeneration)
            return;

        try {
            initFields();
        } catch (Exception e) {
            return;
        }

        World world = event.world;

        for (TileEntity te : world.loadedTileEntityList) {
            if (te instanceof TileSpecialFlower) {
                TileSpecialFlower flower = (TileSpecialFlower) te;
                SubTileEntity subTile = flower.getSubTile();
                if (subTile instanceof SubTileShulkMeNot) {
                    try {
                        processShulkMeNot((SubTileShulkMeNot) subTile, world);
                    } catch (Exception e) {
                        // skip this flower
                    }
                }
            }
        }
    }

    private void initFields() throws Exception {
        if (supertileField == null) {
            supertileField = SubTileEntity.class.getDeclaredField("supertile");
            supertileField.setAccessible(true);
        }
        if (ticksExistedField == null) {
            ticksExistedField = SubTileEntity.class.getDeclaredField("ticksExisted");
            ticksExistedField.setAccessible(true);
        }
        if (manaField == null) {
            manaField = SubTileGenerating.class.getDeclaredField("mana");
            manaField.setAccessible(true);
        }
    }

    private void processShulkMeNot(SubTileShulkMeNot shulkMeNot, World world) throws Exception {
        TileEntity supertile = (TileEntity) supertileField.get(shulkMeNot);
        int ticksExisted = ticksExistedField.getInt(shulkMeNot);
        int mana = manaField.getInt(shulkMeNot);

        if (supertile == null || supertile.getWorld() == null) return;

        int searchRange = 8;

        AxisAlignedBB searchBox = new AxisAlignedBB(
            supertile.getPos().add(-searchRange, -searchRange, -searchRange),
            supertile.getPos().add(searchRange + 1, searchRange + 1, searchRange + 1)
        );

        // 1. Shulker Regeneration
        if (ModConfig.bubbell.shulkerRegeneration && ticksExisted % 200 == 0) {
            List<EntityShulker> shulkers = world.getEntitiesWithinAABB(EntityShulker.class, searchBox);
            for (EntityShulker shulker : shulkers) {
                shulker.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 220, 0));
            }
        }

        // 2. Swallow Shulker Bullets
        if (ModConfig.bubbell.swallowShulkerBullets) {
            List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, searchBox);
            for (Entity entity : entities) {
                if (entity.isDead) continue;
                if (entity instanceof EntityShulkerBullet) {
                    entity.setDead();
                    manaField.setInt(shulkMeNot, mana + 15000);
                    break;
                }
            }
        }

        // 3. Swallow Shulker target monsters (NOT the Shulker itself)
        if (ModConfig.bubbell.swallowShulkerTargets) {
            List<EntityShulker> shulkers = world.getEntitiesWithinAABB(EntityShulker.class, searchBox);
            for (EntityShulker shulker : shulkers) {
                EntityLivingBase target = shulker.getAttackTarget();
                if (target == null || target.isDead) continue;
                if (!(target instanceof IMob)) continue;
                if (target instanceof EntityShulker) continue;
                double distSq = target.getDistanceSq(supertile.getPos());
                if (distSq > (long) searchRange * searchRange) continue;
                target.setDead();
                manaField.setInt(shulkMeNot, mana + 75000);
                break;
            }
        }
    }
}
