package dev.tr7zw.entityculling.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import dev.tr7zw.entityculling.access.DataTrackerAccessor;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;

@Mixin(EntityDataManager.class)
public class DataTrackerMixin implements DataTrackerAccessor {

	@Shadow
	private Map<Integer, EntityDataManager.DataEntry<?>> entries;
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getUnsafe(DataParameter<T> trackedData) {
		return (T) this.entries.get(trackedData.getId()).getValue();
	}

}
