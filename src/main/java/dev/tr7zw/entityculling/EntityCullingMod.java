package dev.tr7zw.entityculling;

import java.util.HashSet;
import java.util.Set;

import dev.tr7zw.entityculling.occlusionculling.OcclusionCullingInstance;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("entityculling")
public class EntityCullingMod {

	public static EntityCullingMod instance;
	public final OcclusionCullingInstance culling = new OcclusionCullingInstance();
	public Set<TileEntityType<?>> unCullable = new HashSet<>();
	public boolean nametags = true;
	public boolean debug = false;
	private CullTask cullTask = new CullTask(culling, unCullable);

	public EntityCullingMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::doTick);
	}
	
    private void setup(final FMLCommonSetupEvent event)
    {
		instance = this;
		unCullable.add(TileEntityType.BEACON);// TODO: Move to config
		new Thread(cullTask, "CullThread").start();
    }
    
    private void doTick(ClientTickEvent event) {
    	culling.resetCache();
		cullTask.requestCull = true;
    }
	
}
