package nyonio.botania_unbound.mixin;

import nyonio.botania_unbound.ModConfig;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vazkii.botania.api.subtile.RadiusDescriptor;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.block.subtile.generating.SubTileArcaneRose;
import vazkii.botania.common.core.helper.ExperienceHelper;

import java.util.List;

@Mixin(value = SubTileArcaneRose.class, remap = false)
public abstract class MixinSubTileArcaneRose extends SubTileGenerating {

    private static final int RANGE = 1;

    /**
     * Overwrite onUpdate to implement Arcane Rose adjustments:
     * 1. Max speed player XP extraction - drain all available XP per tick
     * 2. Max speed XP orb absorption - absorb all orbs in range per tick
     *
     * @author Nyonio
     * @reason Arcane Rose max speed adjustments
     */
    @Overwrite
    public void onUpdate() {
        super.onUpdate();

        if (mana >= getMaxMana())
            return;

        List<EntityPlayer> players = supertile.getWorld().getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(supertile.getPos().add(-RANGE, -RANGE, -RANGE), supertile.getPos().add(RANGE + 1, RANGE + 1, RANGE + 1)));

        if (ModConfig.arcaneRose.maxSpeedPlayerXP) {
            // Drain all available XP from all players in range
            for (EntityPlayer player : players) {
                if (ExperienceHelper.getPlayerXP(player) >= 1 && player.onGround) {
                    int available = ExperienceHelper.getPlayerXP(player);
                    int space = getMaxMana() - mana;
                    // Each XP = 50 mana, so max XP we can use = space / 50
                    int maxXP = space / 50;
                    if (maxXP <= 0) continue;
                    int toDrain = Math.min(available, maxXP);
                    ExperienceHelper.drainPlayerXP(player, toDrain);
                    mana += toDrain * 50;
                    if (mana >= getMaxMana()) return;
                }
            }
        } else {
            // Original: drain 1 XP from first available player
            for (EntityPlayer player : players) {
                if (ExperienceHelper.getPlayerXP(player) >= 1 && player.onGround) {
                    ExperienceHelper.drainPlayerXP(player, 1);
                    mana += 50;
                    return;
                }
            }
        }

        List<EntityXPOrb> orbs = supertile.getWorld().getEntitiesWithinAABB(EntityXPOrb.class, new AxisAlignedBB(supertile.getPos().add(-RANGE, -RANGE, -RANGE), supertile.getPos().add(RANGE + 1, RANGE + 1, RANGE + 1)));

        if (ModConfig.arcaneRose.maxSpeedXPOrbs) {
            // Absorb all XP orbs in range
            for (EntityXPOrb orb : orbs) {
                if (orb.isDead) continue;
                int orbMana = orb.getXpValue() * 35;
                mana += orbMana;
                orb.setDead();
                if (mana >= getMaxMana()) return;
            }
        } else {
            // Original: absorb one orb per tick
            for (EntityXPOrb orb : orbs) {
                mana += orb.getXpValue() * 35;
                orb.setDead();
                return;
            }
        }
    }
}
