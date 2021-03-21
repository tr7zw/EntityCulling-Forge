package dev.tr7zw.entityculling;

import java.util.HashSet;
import java.util.Set;

import com.logisticscraft.occlusionculling.OcclusionCullingInstance;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("entityculling")
public class EntityCullingMod {

    public static EntityCullingMod instance;
    public final OcclusionCullingInstance culling = new OcclusionCullingInstance(128, new ForgeProvider());
    public Set<TileEntityType<?>> unCullable = new HashSet<>();
    public boolean nametags = true;
    public boolean debug = false;
    public boolean debugHitboxes = false;
    public static boolean enabled = true; // public static to make it faster for the jvm
    private CullTask cullTask = new CullTask(culling, unCullable);
    private Thread cullThread;
    private KeyBinding keybind = new KeyBinding("key.entityculling.toggle", -1, "EntityCulling");
    private boolean pressed = false;

    public EntityCullingMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::doTick);
    }

    private void setup(final FMLCommonSetupEvent event) {
        instance = this;
        unCullable.add(TileEntityType.BEACON);// TODO: Move to config
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
        } else {
            pressed = false;
        }
        cullTask.requestCull = true;
    }

}
