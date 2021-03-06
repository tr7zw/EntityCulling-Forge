package dev.tr7zw.entityculling;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;

@Mod("entityculling")
public class EntityCullingMod {

    public static EntityCullingMod instance;
    public OcclusionCullingInstance culling;
    public Set<TileEntityType<?>> unCullable = new HashSet<>();
    public boolean debugHitboxes = false;
    public static boolean enabled = true; // public static to make it faster for the jvm
    public CullTask cullTask;
    private Thread cullThread;
    private KeyBinding keybind;
    private boolean pressed = false;

    public Config config;
    private final File settingsFile = new File("config", "entityculling.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private boolean onServer = false;

    //stats
    public int renderedBlockEntities = 0;
    public int skippedBlockEntities = 0;
    public int renderedEntities = 0;
    public int skippedEntities = 0;
    
    public EntityCullingMod() {
        try {
            Class clientClass = net.minecraft.client.MinecraftGame.class;
        }catch(Throwable ex) {
            System.out.println("EntityCulling Mod installed on a Server. Going to sleep.");
            onServer = true;
            return;
        }
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::doTick);
        keybind = new KeyBinding("key.entityculling.toggle", -1, "EntityCulling");
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
        () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, isServer) -> true));
    }

    private void setup(final FMLCommonSetupEvent event) {
        if(onServer)return;
        instance = this;
        if (settingsFile.exists()) {
            try {
                config = gson.fromJson(new String(Files.readAllBytes(settingsFile.toPath()), StandardCharsets.UTF_8),
                        Config.class);
            } catch (Exception ex) {
                System.out.println("Error while loading config! Creating a new one!");
                ex.printStackTrace();
            }
        }
        if (config == null) {
            config = new Config();
            writeConfig();
        } else {
            if(ConfigUpgrader.upgradeConfig(config)) {
                writeConfig(); // Config got modified
            }
        }
        for(String blockId : config.blockEntityWhitelist) {
            Optional<TileEntityType<?>> block = Registry.BLOCK_ENTITY_TYPE.getOptional(new ResourceLocation(blockId));
            block.ifPresent(b -> {
                unCullable.add(b);
            });
        }
        culling = new OcclusionCullingInstance(config.tracingDistance, new ForgeProvider());
        cullTask = new CullTask(culling, unCullable);
        cullThread = new Thread(cullTask, "CullThread");
        cullThread.setUncaughtExceptionHandler((thread, ex) -> {
            System.out.println("The CullingThread has crashed! Please report the following stacktrace!");
            ex.printStackTrace();
        });
        cullThread.start();
        ClientRegistry.registerKeyBinding(keybind);
    }

    private void doTick(ClientTickEvent event) {
        if (keybind.isPressed()) {
            if (pressed)
                return;
            pressed = true;
            enabled = !enabled;
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if(enabled) {
                if (player != null) {
                    player.sendMessage(new StringTextComponent("Culling on").mergeStyle(TextFormatting.GREEN),
                            Util.DUMMY_UUID);
                }
            } else {
                if (player != null) {
                    player.sendMessage(new StringTextComponent("Culling off").mergeStyle(TextFormatting.RED),
                            Util.DUMMY_UUID);
                }
            }
        } else {
            pressed = false;
        }
        cullTask.requestCull = true;
    }

    public void writeConfig() {
        if (settingsFile.exists())
            settingsFile.delete();
        try {
            Files.write(settingsFile.toPath(), gson.toJson(config).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
}
