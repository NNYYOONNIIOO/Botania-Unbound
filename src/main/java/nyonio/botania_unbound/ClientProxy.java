package nyonio.botania_unbound;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import nyonio.botania_unbound.client.RenderTileCell;
import vazkii.botania.common.block.tile.TileCell;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileCell.class, new RenderTileCell());
    }
}
