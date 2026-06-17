package nyonio.botania_unbound;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.SidedProxy;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = BotaniaUnbound.MODID,
    name = BotaniaUnbound.NAME,
    version = BotaniaUnbound.VERSION,
    dependencies = "required:botania;required-after:mixinbooter@[10.7,)"
)
public class BotaniaUnbound
{
    public static final String MODID = "botania_unbound";
    public static final String NAME = "Botania Unbound";
    public static final String VERSION = "1.0";

    public static Logger logger;

    @SidedProxy(
        clientSide = "nyonio.botania_unbound.ClientProxy",
        serverSide = "nyonio.botania_unbound.CommonProxy"
    )
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init();
        MinecraftForge.EVENT_BUS.register(new BubbellEventHandler());
        MinecraftForge.EVENT_BUS.register(new DandelifeonEventHandler());
        MinecraftForge.EVENT_BUS.register(new ManaBombEventHandler());
    }
}
