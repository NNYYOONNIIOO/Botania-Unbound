package nyonio.botania_unbound.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.common.entity.EntityDoppleganger;
import nyonio.botania_unbound.ModConfig;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

@Mixin(value = EntityDoppleganger.class, remap = false)
public abstract class MixinEntityDoppleganger extends EntityLiving {

    @Shadow(remap = false) private boolean aggro;
    @Shadow(remap = false) private int tpDelay;
    @Shadow(remap = false) private boolean spawnPixies;
    @Shadow(remap = false) private int mobSpawnTicks;
    @Shadow(remap = false) private List<UUID> playersWhoAttacked;
    @Shadow(remap = false) private net.minecraft.util.math.BlockPos source;

    protected MixinEntityDoppleganger(net.minecraft.world.World worldIn) {
        super(worldIn);
    }

    @Shadow(remap = false)
    public abstract int getInvulTime();

    @Shadow(remap = false)
    public abstract void setInvulTime(int time);

    /**
     * @reason Allow fake players when configured
     * @author nyonio
     */
    @Overwrite
    public static boolean isTruePlayer(Entity e) {
        if (ModConfig.gaiaGuardian.allowFakePlayers) {
            return e instanceof EntityPlayer;
        }

        if (!(e instanceof EntityPlayer))
            return false;

        EntityPlayer player = (EntityPlayer) e;
        String name = player.getName();
        return !(player instanceof net.minecraftforge.common.util.FakePlayer || name.matches("^(?:\\[.*\\])|(?:ComputerCraft)$"));
    }

    /**
     * @reason Allow fake players to be counted when configured
     * @author nyonio
     */
    @Overwrite
    public List<EntityPlayer> getPlayersAround() {
        float range = 15F;
        EntityDoppleganger self = (EntityDoppleganger) (Object) this;
        if (ModConfig.gaiaGuardian.allowFakePlayers) {
            return self.world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(source.getX() + 0.5 - range, source.getY() + 0.5 - range, source.getZ() + 0.5 - range, source.getX() + 0.5 + range, source.getY() + 0.5 + range, source.getZ() + 0.5 + range), player -> !player.isSpectator());
        }
        return self.world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(source.getX() + 0.5 - range, source.getY() + 0.5 - range, source.getZ() + 0.5 - range, source.getX() + 0.5 + range, source.getY() + 0.5 + range, source.getZ() + 0.5 + range), player -> EntityDoppleganger.isTruePlayer(player) && !player.isSpectator());
    }

    /**
     * Call spawnMobs via reflection since @Shadow doesn't work reliably for private methods
     */
    private void invokeSpawnMobs(List<EntityPlayer> players) {
        try {
            EntityDoppleganger self = (EntityDoppleganger) (Object) this;
            Method method = EntityDoppleganger.class.getDeclaredMethod("spawnMobs", List.class);
            method.setAccessible(true);
            method.invoke(self, players);
        } catch (Exception e) {
            System.out.println("[BotaniaUnbound] Failed to invoke spawnMobs: " + e.getMessage());
        }
    }

    /**
     * @reason Remove damage cap, allow fake players, make boss invulnerable during mob spawn phase,
     * and accelerate mob spawning on attack attempts
     * @author nyonio
     * Note: In runtime, 'attackEntityFrom' is obfuscated to 'func_70097_a'
     */
    @Overwrite
    public boolean func_70097_a(@Nonnull DamageSource source, float par2) {
        Entity e = source.getTrueSource();

        boolean isAllowedPlayer;
        if (ModConfig.gaiaGuardian.allowFakePlayers) {
            isAllowedPlayer = e instanceof EntityPlayer;
        } else {
            isAllowedPlayer = e instanceof EntityPlayer && EntityDoppleganger.isTruePlayer(e);
        }

        if (isAllowedPlayer) {
            EntityPlayer player = (EntityPlayer) e;

            if (!playersWhoAttacked.contains(player.getUniqueID()))
                playersWhoAttacked.add(player.getUniqueID());

            // Mob spawn phase: aggro==true (boss has been fought), health < 20% and mobSpawnTicks > 0
            // During initial spawn phase, aggro is false, so this won't trigger
            boolean isInMobSpawnPhase = aggro && mobSpawnTicks > 0 && getHealth() / getMaxHealth() < 0.2F;
            boolean isInvulnerable = getInvulTime() > 0 || isInMobSpawnPhase;
            System.out.println("[BotaniaUnbound] Gaia Guardian attacked: invulTime=" + getInvulTime() + " mobSpawnTicks=" + mobSpawnTicks + " health=" + getHealth() + "/" + getMaxHealth() + " healthRatio=" + (getHealth() / getMaxHealth()) + " isInMobSpawnPhase=" + isInMobSpawnPhase + " isInvulnerable=" + isInvulnerable);

            if (isInvulnerable) {
                if (ModConfig.gaiaGuardian.attackAcceleratesMobSpawn && isInMobSpawnPhase) {
                    // Reduce 4 seconds (80 ticks) of mob spawn time per attack
                    mobSpawnTicks = Math.max(0, mobSpawnTicks - 80);
                    // Also spawn an extra wave of mobs immediately
                    invokeSpawnMobs(getPlayersAround());
                }
                return false;
            }

            // Not in invulnerable phase - apply damage
            float damage;
            if (ModConfig.gaiaGuardian.removeDamageCap) {
                damage = par2;
            } else {
                damage = Math.min(25, par2);
            }

            return super.attackEntityFrom(source, damage);
        }

        return false;
    }

    /**
     * @reason Original damageEntity logic from EntityDoppleganger (knockback, aggro, tpDelay, spawnPixies)
     * @author nyonio
     * Note: In runtime, 'damageEntity' is obfuscated to 'func_70665_d'
     */
    @Overwrite
    protected void func_70665_d(@Nonnull DamageSource source, float par2) {
        super.damageEntity(source, par2);

        Entity attacker = source.getImmediateSource();
        if (attacker != null) {
            vazkii.botania.common.core.helper.Vector3 thisVector = vazkii.botania.common.core.helper.Vector3.fromEntityCenter(this);
            vazkii.botania.common.core.helper.Vector3 playerVector = vazkii.botania.common.core.helper.Vector3.fromEntityCenter(attacker);
            vazkii.botania.common.core.helper.Vector3 motionVector = thisVector.subtract(playerVector).normalize().multiply(0.75);

            if (getHealth() > 0) {
                motionX = -motionVector.x;
                motionY = 0.5;
                motionZ = -motionVector.z;
                tpDelay = 4;
                spawnPixies = aggro;
            }

            aggro = true;
        }
    }
}
