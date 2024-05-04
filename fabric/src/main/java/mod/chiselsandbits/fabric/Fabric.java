package mod.chiselsandbits.fabric;

import com.communi.suggestu.scena.core.init.PlatformInitializationHandler;
import mod.chiselsandbits.ChiselsAndBits;
import mod.chiselsandbits.block.ChiseledBlock;
import mod.chiselsandbits.fabric.plugin.FabricPluginManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class Fabric implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("chisels-and-bits-fabric");

    private ChiselsAndBits chiselsAndBits;

    public Fabric()
    {
        LOGGER.info("Initialized chisels-and-bits for Fabric");
        PlatformInitializationHandler.getInstance().onInit(platform -> setChiselsAndBits(new ChiselsAndBits(
                Objects::isNull,
                target -> Optional.empty(),
                FabricPluginManager.getInstance(),
                ChiseledBlock::new
        )));
    }

    @Override
    public void onInitialize()
    {
        if (chiselsAndBits != null)
        {
            chiselsAndBits.onInitialize();
            return;
        }

        PlatformInitializationHandler.getInstance().onInit(platform -> {
            if (chiselsAndBits == null)
            {
                throw new IllegalStateException("Chisels and bits was not initialized properly.");
            }

            chiselsAndBits.onInitialize();
        });
    }

    public void setChiselsAndBits(final ChiselsAndBits chiselsAndBits)
    {
        this.chiselsAndBits = chiselsAndBits;
    }
}
