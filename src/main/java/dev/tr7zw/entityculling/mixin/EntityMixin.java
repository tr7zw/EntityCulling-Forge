package dev.tr7zw.entityculling.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import dev.tr7zw.entityculling.access.DataTrackerAccessor;
import dev.tr7zw.entityculling.access.EntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;

@Mixin(Entity.class)
public class EntityMixin implements EntityAccessor {

	@Shadow
	private boolean glowing;
	@Shadow
	private World world;
	@Shadow
	private EntityDataManager dataManager;
	@Shadow
	private static DataParameter<Byte> FLAGS;
	
	@Override
	public boolean isUnsafeGlowing() {
		return this.glowing || this.world.isRemote && this.getUnsafeFlag(6);
	}
	
	private boolean getUnsafeFlag(int index) {
		return ((Byte) ((DataTrackerAccessor)dataManager).getUnsafe(FLAGS) & 1 << index) != 0;
	}

}
