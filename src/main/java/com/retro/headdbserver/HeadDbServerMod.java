package com.retro.headdbserver;

import com.retro.headdbserver.command.HeadDbCommands;
import com.retro.headdbserver.config.HeadDbConfig;
import com.retro.headdbserver.data.HeadRepository;
import com.retro.headdbserver.util.PriceState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

@Mod(HeadDbServerMod.ModId)
public final class HeadDbServerMod {
    public static final String ModId = "headdbserver";
    public static final Logger Logger = LoggerFactory.getLogger(ModId);

    private static HeadDbServerMod _instance;

    private final HeadRepository _repository;
    private final PriceState _priceState;

    private Path _serverDirectory;

    public HeadDbServerMod(IEventBus modEventBus, ModContainer modContainer) {
        _instance = this;

        modContainer.registerConfig(ModConfig.Type.SERVER, HeadDbConfig.Settings, ModId + "-server.toml");

        _repository = new HeadRepository();
        _priceState = new PriceState();

        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);

        Logger.info("HeadDB Server mod constructed.");
    }

    public static HeadDbServerMod get() {
        return _instance;
    }

    public HeadRepository repository() {
        return _repository;
    }

    public PriceState priceState() {
        return _priceState;
    }

    public Path serverDirectory() {
        return _serverDirectory;
    }

    public void savePriceState() {
        if (_serverDirectory != null) {
            _priceState.save(_serverDirectory);
        }
    }

    public void reloadPriceState() {
        if (_serverDirectory != null) {
            _priceState.load(_serverDirectory);
        }
    }

    private void registerCommands(RegisterCommandsEvent event) {
        HeadDbCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    private void onServerStarted(ServerStartedEvent event) {
        _serverDirectory = event.getServer().getServerDirectory();
        _priceState.load(_serverDirectory);
        _repository.refreshAsync(event.getServer());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        savePriceState();
    }
}
