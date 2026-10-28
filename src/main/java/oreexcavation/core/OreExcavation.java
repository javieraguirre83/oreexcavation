package oreexcavation.core;

import net.minecraft.resources.Identifier;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import oreexcavation.client.ExcavationClient;
import oreexcavation.handlers.ConfigHandler;
import oreexcavation.handlers.EventHandler;
import oreexcavation.network.ExcPacketHandler;
import oreexcavation.network.PacketExcavation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(OreExcavation.MODID)
public class OreExcavation {
    public static final String MODID = "oreexcavation";
    public static final String NAME = "OreExcavation";
    public static final String CHANNEL = "oe_chan";
    public static final int NET_PROTOCOL = 1;
    public static OreExcavation instance;
    public static Logger logger = LogManager.getLogger(MODID);

    public SimpleChannel network;

    public OreExcavation(FMLJavaModLoadingContext context) {
        instance = this;
        var modBus = context.getModBusGroup();

        context.registerConfig(ModConfig.Type.COMMON, ConfigHandler.commonSpec);
        context.registerConfig(ModConfig.Type.CLIENT, ConfigHandler.clientSpec);
        ModConfigEvent.Loading.getBus(modBus).addListener(ConfigHandler::onLoad);
        ModConfigEvent.Reloading.getBus(modBus).addListener(ConfigHandler::onReload);
        FMLCommonSetupEvent.getBus(modBus).addListener(this::setup);

        network = ChannelBuilder.named(Identifier.fromNamespaceAndPath(MODID, CHANNEL))
                .networkProtocolVersion(NET_PROTOCOL)
                .simpleChannel();
        network.messageBuilder(PacketExcavation.class)
                .encoder(PacketExcavation::toBytes)
                .decoder(PacketExcavation::new)
                .consumerMainThread(ExcPacketHandler::handle)
                .add();
        network.build();

        EventHandler.register();
        RegisterCommandsEvent.BUS.addListener(event -> CommandUndo.register(event.getDispatcher()));

        if (FMLEnvironment.dist.isClient()) {
            ExcavationClient.init(context);
        }
    }

    private void setup(FMLCommonSetupEvent event) {
        // GameStages does not exist for 26.2, so its integration stays off
        ExcavationSettings.gamestagesInstalled = false;
    }
}
