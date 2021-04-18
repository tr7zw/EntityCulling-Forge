package dev.tr7zw.entityculling.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import dev.tr7zw.entityculling.access.DataTrackerAccessor;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.datasync.EntityDataManager.DataEntry;

@Mixin(EntityDataManager.class)
public class DataTrackerMixin implements DataTrackerAccessor {

	@Shadow
	private Map<Integer, EntityDataManager.DataEntry<?>> entries;
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getUnsafe(DataParameter<T> trackedData) {
	    DataEntry<?> entry = this.entries.get(trackedData.getId());
		return  entry == null ? null : (T) entry.getValue();
	}

}
