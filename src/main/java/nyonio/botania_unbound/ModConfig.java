package nyonio.botania_unbound;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = BotaniaUnbound.MODID)
public class ModConfig {

    // ==================== Entropinnyum (热爆花) ====================
    @Config.Name("Entropinnyum")
    @Config.Comment("Entropinnyum (热爆花) settings")
    public static EntropinnyumCategory entropinnyum = new EntropinnyumCategory();

    public static class EntropinnyumCategory {
        @Config.Name("Allow Liquid TNT")
        @Config.Comment("Allow Entropinnyum to detect TNT exploding in liquids")
        public boolean allowLiquidTNT = true;
    }

    // ==================== Narslimmus (粘球草) ====================
    @Config.Name("Narslimmus")
    @Config.Comment("Narslimmus (粘球草) settings")
    public static NarslimmusCategory narslimmus = new NarslimmusCategory();

    public static class NarslimmusCategory {
        @Config.Name("Ignore Chunk Limit")
        @Config.Comment("Allow Narslimmus to work outside slime chunks")
        public boolean ignoreChunkLimit = true;

        @Config.Name("Accept All Slimes")
        @Config.Comment("Allow Narslimmus to accept all slimes (including from spawners, spawn eggs, splits, magma cubes, Tinkers slimes, etc.)")
        public boolean acceptAllSlimes = true;
    }

    // ==================== Endoflame (火红莲) ====================
    @Config.Name("Endoflame")
    @Config.Comment("Endoflame (火红莲) settings")
    public static EndoflameCategory endoflame = new EndoflameCategory();

    public static class EndoflameCategory {
        @Config.Name("Remove Tick Limit & Auto Expand Capacity")
        @Config.Comment({
            "Remove the 32000 tick burn time limit",
            "Auto expand capacity when burning high-energy fuels (only needed when exceeding 32000 tick)"
        })
        public boolean removeTickLimit = true;

        @Config.Name("Skip Burn Process")
        @Config.Comment("Skip burn process (generate mana instantly)")
        public boolean skipBurnProcess = true;
    }

    // ==================== Thermalily (炽玫瑰) ====================
    @Config.Name("Thermalily")
    @Config.Comment("Thermalily (炽玫瑰) settings")
    public static ThermalilyCategory thermalily = new ThermalilyCategory();

    public static class ThermalilyCategory {
        @Config.Name("No Lava Consumption During Cooldown")
        @Config.Comment("During cooldown, don't consume lava (extend cooldown), change to normal idle state")
        public boolean noLavaDuringCooldown = true;

        @Config.Name("Skip Burn Time")
        @Config.Comment("Skip burn time, generate mana instantly after consuming lava")
        public boolean skipBurnTime = true;

        @Config.Name("Ignore Cooldown")
        @Config.Comment("Ignore cooldown time, can consume lava immediately after burn ends")
        public boolean ignoreCooldown = true;
    }

    // ==================== Mana Enchanter (魔力附魔台) ====================
    @Config.Name("Enchanter")
    @Config.Comment("Mana Enchanter (魔力附魔台) settings")
    public static EnchanterCategory enchanter = new EnchanterCategory();

    public static class EnchanterCategory {
        @Config.Name("Allow Book Enchanting")
        @Config.Comment("Allow enchanting books (not just items)")
        public boolean allowBookEnchanting = true;

        @Config.Name("All Enchantments From Book")
        @Config.Comment("Receive all enchantments from enchanted book (vanilla only receives the first one)")
        public boolean allEnchantmentsFromBook = true;

        @Config.Name("Allow Conflicting Enchantments")
        @Config.Comment("Allow conflicting enchantments (e.g., Infinity + Mending)")
        public boolean allowConflictingEnchantments = true;

        @Config.Name("Read Enchantments From Enchanted Items")
        @Config.Comment("Read enchantments from enchanted items as if they were enchanted books")
        public boolean readFromEnchantedItems = true;

        @Config.Name("Drop Enchantments On Break")
        @Config.Comment("When multiblock structure is broken after enchanting, drop enchantments as enchanted books")
        public boolean dropEnchantmentsOnBreak = true;
    }

    // ==================== Terra Blade (泰拉之刃) ====================
    @Config.Name("Terra Blade")
    @Config.Comment("Terra Blade (泰拉之刃) settings")
    public static TerraBladeCategory terraBlade = new TerraBladeCategory();

    public static class TerraBladeCategory {
        @Config.Name("Beam Inherits Player Attributes")
        @Config.Comment("Beam damage inherits player's ATTACK_DAMAGE attribute (includes weapon base, enchantments, strength potion, equipment bonuses, etc.) and Fire Aspect ignites targets")
        public boolean beamInheritsEnchantments = true;
    }

    // ==================== Thunder Sword (召雷者) ====================
    @Config.Name("Thunder Sword")
    @Config.Comment("Thunder Sword (召雷者) settings")
    public static ThunderSwordCategory thunderSword = new ThunderSwordCategory();

    public static class ThunderSwordCategory {
        @Config.Name("Lightning Inherits Player Attributes")
        @Config.Comment("Chain lightning damage inherits player's ATTACK_DAMAGE attribute (includes weapon base, enchantments, strength potion, equipment bonuses, etc.)")
        public boolean lightningInheritsEnchantments = true;

        @Config.Name("Infinite Chain Lightning")
        @Config.Comment("Chain lightning hops infinitely between enemies until no more targets are found or damage drops to 0")
        public boolean infiniteChainLightning = true;
    }

    // ==================== Star Sword (召星者) ====================
    @Config.Name("Star Sword")
    @Config.Comment("Star Sword (召星者) settings")
    public static StarSwordCategory starSword = new StarSwordCategory();

    public static class StarSwordCategory {
        @Config.Name("Falling Star Inherits Player Attributes")
        @Config.Comment("Falling star damage inherits player's ATTACK_DAMAGE attribute (includes weapon base, enchantments, strength potion, equipment bonuses, etc.)")
        public boolean starInheritsEnchantments = true;

        @Config.Name("Star Only Damages Living Entities")
        @Config.Comment("Prevent falling stars from damaging non-living entities (e.g., item frames, paintings)")
        public boolean starOnlyDamagesLiving = true;
    }

    // ==================== Gaia Guardian (盖亚守护者) ====================
    @Config.Name("Gaia Guardian")
    @Config.Comment("Gaia Guardian (盖亚守护者) settings")
    public static GaiaGuardianCategory gaiaGuardian = new GaiaGuardianCategory();

    public static class GaiaGuardianCategory {
        @Config.Name("Remove Damage Cap")
        @Config.Comment("Remove the 25 damage per hit cap on Gaia Guardian")
        public boolean removeDamageCap = true;

        @Config.Name("Attack Accelerates Mob Spawn Phase")
        @Config.Comment("Attacking Gaia Guardian during mob spawn phase shortens that phase (reduces mobSpawnTicks by damage dealt)")
        public boolean attackAcceleratesMobSpawn = true;

        @Config.Name("Allow Fake Players")
        @Config.Comment("Allow fake players (e.g., from dispensers, ComputerCraft turtles) to damage Gaia Guardian and trigger the ritual")
        public boolean allowFakePlayers = true;
    }

    // ==================== Gourmaryllis (彼方兰) ====================
    @Config.Name("Gourmaryllis")
    @Config.Comment("Gourmaryllis (彼方兰) settings")
    public static GourmaryllisCategory gourmaryllis = new GourmaryllisCategory();

    public static class GourmaryllisCategory {
        @Config.Name("No Diminishing Returns")
        @Config.Comment("No mana reduction when eating the same food repeatedly")
        public boolean noDiminishingReturns = true;

        @Config.Name("No Chewing Time")
        @Config.Comment("Skip chewing time, produce mana immediately after eating food")
        public boolean noChewingTime = true;

        @Config.Name("No Nutrition Cap")
        @Config.Comment("Remove the 12 nutrition cap, auto-expand mana capacity for high-nutrition foods")
        public boolean noNutritionCap = true;

        @Config.Name("Don't Eat During Chewing")
        @Config.Comment("Don't consume (destroy) food items while chewing, preventing food waste")
        public boolean dontEatDuringChewing = true;
    }

    // ==================== Bubbell (勿落草) ====================
    @Config.Name("Bubbell")
    @Config.Comment("Bubbell (勿落草) settings")
    public static BubbellCategory bubbell = new BubbellCategory();

    public static class BubbellCategory {
        @Config.Name("Swallow Shulker Targets")
        @Config.Comment("Bubbell swallows monsters targeted by Shulkers, producing 75000 mana without consuming the Shulker")
        public boolean swallowShulkerTargets = true;

        @Config.Name("Swallow Shulker Bullets")
        @Config.Comment("Bubbell can swallow Shulker Bullets, producing 15000 mana")
        public boolean swallowShulkerBullets = true;

        @Config.Name("Shulker Regeneration")
        @Config.Comment("Shulkers within Bubbell's range get Regeneration I effect (every 10s, lasts 11s)")
        public boolean shulkerRegeneration = true;
    }

    // ==================== Dandelifeon (启命英) ====================
    @Config.Name("Dandelifeon")
    @Config.Comment("Dandelifeon (启命英) settings")
    public static DandelifeonCategory dandelifeon = new DandelifeonCategory();

    public static class DandelifeonCategory {
        @Config.Name("Enable Reform")
        @Config.Comment("Enable Dandelifeon reform mechanism (2048-style cell merging, redstone direction push, cell number display)")
        public boolean enableReform = true;

        @Config.Name("Initial Cell Number")
        @Config.Comment("Initial number value when a cell block is created or placed")
        @Config.RangeInt(min = 1)
        public int initialCellNumber = 1;

        @Config.Name("Max Generation")
        @Config.Comment("Maximum cell generation (age) limit. Vanilla is 100")
        @Config.RangeInt(min = 1)
        public int maxGeneration = 16384;

        @Config.Name("Expand Mana Buffer")
        @Config.Comment("Temporarily expand mana buffer when single cycle production exceeds max mana")
        public boolean expandManaBuffer = true;

        @Config.Name("Skip On Full Mana")
        @Config.Comment("Skip cycle when mana is full (instead of still running and wasting cells)")
        public boolean skipOnFullMana = true;

        @Config.Name("Enable Speed Adjust")
        @Config.Comment("Enable Dandelifeon cycle speed adjustment. If disabled, vanilla speed (10 ticks) is used")
        public boolean enableSpeedAdjust = true;

        @Config.Name("Cycle Speed")
        @Config.Comment("Number of ticks per Dandelifeon cycle (vanilla is 10)")
        @Config.RangeInt(min = 1)
        public int cycleSpeed = 5;
    }

    // ==================== Mana Storm (魔力风暴) ====================
    @Config.Name("Mana Storm")
    @Config.Comment("Mana Storm (魔力风暴) settings")
    public static ManaStormCategory manaStorm = new ManaStormCategory();

    public static class ManaStormCategory {
        @Config.Name("Disable Ray Explosion")
        @Config.Comment("Disable explosions from mana storm burst rays hitting blocks")
        public boolean disableRayExplosion = false;

        @Config.Name("Disable Self Explosion")
        @Config.Comment("Disable the final explosion when mana storm ends")
        public boolean disableSelfExplosion = false;

        @Config.Name("Duration")
        @Config.Comment("Duration of mana storm in ticks (vanilla is approximately 1350)")
        @Config.RangeInt(min = 1)
        public int duration = 1350;

        @Config.Name("Total Bursts")
        @Config.Comment("Total number of bursts the mana storm fires over its duration (vanilla is 250)")
        @Config.RangeInt(min = 1)
        public int totalBursts = 250;

        @Config.Name("Death Time")
        @Config.Comment("Ticks after all bursts are fired before the storm ends (vanilla is 200)")
        @Config.RangeInt(min = 1)
        public int deathTime = 200;

        @Config.Name("Enable Redstone Activation")
        @Config.Comment("Allow mana bomb to be activated by redstone signal")
        public boolean enableRedstoneActivation = false;

        @Config.Name("Disable Mana Pulse Activation")
        @Config.Comment("Prevent mana bomb from being activated by mana pulse collision")
        public boolean disableManaPulseActivation = false;
    }

    // ==================== Kekimurus (贪食花) ====================
    @Config.Name("Kekimurus")
    @Config.Comment("Kekimurus (贪食花) settings")
    public static KekimurusCategory kekimurus = new KekimurusCategory();

    public static class KekimurusCategory {
        @Config.Name("Prioritize Higher Y")
        @Config.Comment("Scan for cakes from higher Y to lower Y instead of lower to higher")
        public boolean prioritizeHigherY = true;

        @Config.Name("Eat Cake Items")
        @Config.Comment("Allow Kekimurus to eat dropped cake items (produces mana equivalent to 7 bites)")
        public boolean eatCakeItems = true;

        @Config.RangeInt(min = 1, max = 7)
        @Config.Name("Bites Per Cycle")
        @Config.Comment("Number of cake bites to eat per work cycle (max 7, a full cake has 7 bites)")
        public int bitesPerCycle = 7;

        @Config.Name("Eat Alt Grass")
        @Config.Comment("Allow Kekimurus to eat Botania alt grass blocks (equivalent to 1 bite, turns to dirt)")
        public boolean eatAltGrass = true;
    }

    // ==================== Arcane Rose (阿卡纳蔷薇) ====================
    @Config.Name("Arcane Rose")
    @Config.Comment("Arcane Rose (阿卡纳蔷薇) settings")
    public static ArcaneRoseCategory arcaneRose = new ArcaneRoseCategory();

    public static class ArcaneRoseCategory {
        @Config.Name("Max Speed Player XP")
        @Config.Comment("Extract XP from players at maximum speed (drain all available XP per tick instead of 1 per tick)")
        public boolean maxSpeedPlayerXP = true;

        @Config.Name("Max Speed XP Orbs")
        @Config.Comment("Absorb all experience orbs in range per tick instead of one per tick")
        public boolean maxSpeedXPOrbs = true;
    }

    // ==================== Rafflowsia (噬草花) ====================
    @Config.Name("Rafflowsia")
    @Config.Comment("Rafflowsia (噬草花) settings")
    public static RafflowsiaCategory rafflowsia = new RafflowsiaCategory();

    public static class RafflowsiaCategory {
        @Config.Name("No Diminishing Returns")
        @Config.Comment("No mana reduction when eating the same flower type repeatedly")
        public boolean noDiminishingReturns = true;

        @Config.Name("Eat Flower Items")
        @Config.Comment("Allow Rafflowsia to consume Botania flower items (dropped items, not just placed blocks)")
        public boolean eatFlowerItems = true;

        @Config.Name("Auto Expand Capacity")
        @Config.Comment("Auto-expand mana buffer capacity when eating would cause overflow, instead of stopping")
        public boolean autoExpandCapacity = true;

        @Config.Name("New Mana Formula")
        @Config.Comment("Use new mana formula based on unique flower count: mana = round(-401.45 + 7.03436*x + 16.0932*x^2 + 7.64878*1.25226^x, 100). If 'No Diminishing Returns' is on, same flowers count as unique.")
        public boolean newManaFormula = true;
    }

    // ==================== Munchdew (咀叶花) ====================
    @Config.Name("Munchdew")
    @Config.Comment("Munchdew (咀叶花) settings")
    public static MunchdewCategory munchdew = new MunchdewCategory();

    public static class MunchdewCategory {
        @Config.Name("No Cooldown")
        @Config.Comment("No cooldown when mana buffer is full or no leaves are found for 5 ticks")
        public boolean noCooldown = true;
    }

    // ==================== Spectrolus (斑斓花) ====================
    @Config.Name("Spectrolus")
    @Config.Comment("Spectrolus (斑斓花) settings")
    public static SpectrolusCategory spectrolus = new SpectrolusCategory();

    public static class SpectrolusCategory {
        @Config.Name("Accept Any Color")
        @Config.Comment("Accept any color wool/sheep instead of requiring a specific color sequence")
        public boolean acceptAnyColor = true;

        @Config.Name("Don't Eat Wrong Color")
        @Config.Comment("Don't consume (destroy) wool items with the wrong color")
        public boolean dontEatWrongColor = true;
    }

    // ==================== Pylon Pump (水晶泵) ====================
    @Config.Name("Pylon Pump")
    @Config.Comment("Pylon Pump (水晶泵) settings - Pylons can transfer mana from a pool below to a bound pool")
    public static PylonPumpCategory pylonPump = new PylonPumpCategory();

    public static class PylonVariantConfig {
        @Config.Name("Enabled")
        @Config.Comment("Enable Pylon Pump for this variant")
        public boolean enabled = true;

        @Config.Name("Transfer Rate")
        @Config.Comment("Mana transfer rate per tick (for reference: Spark pair transfers 1000/tick)")
        @Config.RangeInt(min = 1)
        public int transferRate = 10000;

        @Config.Name("Max Distance")
        @Config.Comment("Maximum distance (in blocks) between pylon and bound target")
        @Config.RangeInt(min = 1, max = 256)
        public int maxDistance = 64;

        @Config.Name("Loss Ratio")
        @Config.Comment("Mana loss ratio during transfer (0.0 = no loss, 0.1 = 10% loss)")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double lossRatio = 0.1;

        @Config.Name("Particles")
        @Config.Comment("Show particle effects when pylon is transferring mana")
        public boolean particles = true;

        @Config.Name("Particle Strength")
        @Config.Comment("Particle line strength (number of particles per tick)")
        @Config.RangeInt(min = 1, max = 10)
        public int particleStrength = 3;

        @Config.Name("Vertical Stacking")
        @Config.Comment("Allow vertical stacking: multiple pylons can drain from the same source pool")
        public boolean verticalStacking = true;
    }

    public static class PylonPumpCategory {
        @Config.Name("Mana Pylon (魔法水晶)")
        @Config.Comment("Mana Pylon (魔法水晶) settings")
        public PylonVariantConfig mana = new PylonVariantConfig();

        @Config.Name("Natura Pylon (自然水晶)")
        @Config.Comment("Natura Pylon (自然水晶) settings")
        public PylonVariantConfig natura = createNaturaConfig();

        @Config.Name("Gaia Pylon (盖亚水晶)")
        @Config.Comment("Gaia Pylon (盖亚水晶) settings")
        public PylonVariantConfig gaia = createGaiaConfig();
    }

    private static PylonVariantConfig createNaturaConfig() {
        PylonVariantConfig config = new PylonVariantConfig();
        config.transferRate = 10000;
        config.maxDistance = 64;
        config.lossRatio = 0.1;
        config.particleStrength = 3;
        return config;
    }

    private static PylonVariantConfig createGaiaConfig() {
        PylonVariantConfig config = new PylonVariantConfig();
        config.transferRate = 50000;
        config.maxDistance = 128;
        config.lossRatio = 0.0;
        config.particleStrength = 5;
        return config;
    }

    @Mod.EventBusSubscriber(modid = BotaniaUnbound.MODID)
    private static class ConfigHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(BotaniaUnbound.MODID)) {
                ConfigManager.sync(BotaniaUnbound.MODID, Config.Type.INSTANCE);
            }
        }
    }
}