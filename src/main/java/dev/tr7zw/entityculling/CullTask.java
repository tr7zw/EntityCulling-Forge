package dev.tr7zw.entityculling;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.util.Vec3d;

import dev.tr7zw.entityculling.access.Cullable;
import dev.tr7zw.entityculling.access.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.chunk.Chunk;

public class CullTask implements Runnable {

	public boolean requestCull = false;

	private final OcclusionCullingInstance culling;
	private final Minecraft client = Minecraft.getInstance();
	private final int sleepDelay = EntityCullingMod.instance.config.sleepDelay;
	private final int hitboxLimit = EntityCullingMod.instance.config.hitboxLimit;
	private final Set<TileEntityType<?>> unCullable;
	public long lastTime = 0;

	// reused preallocated vars
    private Vec3d lastPos = new Vec3d(0, 0, 0);
    private Vec3d aabbMin = new Vec3d(0, 0, 0);
    private Vec3d aabbMax = new Vec3d(0, 0, 0);

	
	public CullTask(OcclusionCullingInstance culling, Set<TileEntityType<?>> unCullable) {
		this.culling = culling;
		this.unCullable = unCullable;
	}
	
	@Override
	public void run() {
		while (client.isRunning()) {
			try {
				Thread.sleep(sleepDelay);

				if (EntityCullingMod.enabled && client.world != null && client.player != null && client.player.ticksExisted > 10) {
				    Vector3d cameraMC = EntityCullingMod.instance.config.debugMode
                            ? client.player.getEyePosition(client.getRenderPartialTicks())
                            : client.gameRenderer.getActiveRenderInfo().getProjectedView();
					if (requestCull || !(cameraMC.x == lastPos.x && cameraMC.y == lastPos.y && cameraMC.z == lastPos.z)) {
						long start = System.currentTimeMillis();
						requestCull = false;
						lastPos.set(cameraMC.x, cameraMC.y, cameraMC.z);
                        Vec3d camera = lastPos;
						culling.resetCache();
						boolean spectator = client.player.isSpectator();
						for (int x = -8; x <= 8; x++) {
							for (int z = -8; z <= 8; z++) {
							    Chunk chunk = client.world.getChunk(client.player.chunkCoordX + x,
                                        client.player.chunkCoordZ + z);
								Iterator<Entry<BlockPos, TileEntity>> iterator = chunk.getTileEntityMap().entrySet().iterator();
								Entry<BlockPos, TileEntity> entry;
								while(iterator.hasNext()) {
									try {
										entry = iterator.next();
									}catch(NullPointerException | ConcurrentModificationException ex) {
										break; // We are not synced to the main thread, so NPE's/CME are allowed here and way less
										// overhead probably than trying to sync stuff up for no really good reason
									}
									if(unCullable.contains(entry.getValue().getType())) {
										continue;
									}
									Cullable cullable = (Cullable) entry.getValue();
									if (!cullable.isForcedVisible()) {
										if (spectator) {
											cullable.setCulled(false);
											continue;
										}
										BlockPos pos = entry.getKey();
										if(pos.withinDistance(cameraMC, 64)) { // 64 is the fixed max tile view distance
										    aabbMin.set(pos.getX(), pos.getY(), pos.getZ());
                                            aabbMax.set(pos.getX()+1d, pos.getY()+1d, pos.getZ()+1d);
                                            boolean visible = culling.isAABBVisible(aabbMin, aabbMax, camera);
    										cullable.setCulled(!visible);
										}
									}
								}

							}
						}
						Entity entity = null;
						Iterator<Entity> iterable = client.world.getAllEntities().iterator();
						while (iterable.hasNext()) {
							try {
								entity = iterable.next();
							} catch (NullPointerException | ConcurrentModificationException ex) {
								break; // We are not synced to the main thread, so NPE's/CME are allowed here and way less
										// overhead probably than trying to sync stuff up for no really good reason
							}
							Cullable cullable = (Cullable) entity;
							if (!cullable.isForcedVisible()) {
								if (spectator || ((EntityAccessor)entity).isUnsafeGlowing()) {
									cullable.setCulled(false);
								} else {
								    if(entity.getPositionVec().isWithinDistanceOf(cameraMC, 128)) { // Max supported range currently for this mod
								        net.minecraft.util.math.AxisAlignedBB boundingBox = entity.getRenderBoundingBox();
								        if(boundingBox.getXSize() > hitboxLimit || boundingBox.getYSize() > hitboxLimit || boundingBox.getZSize() > hitboxLimit) {
                                            cullable.setCulled(false); // To big to bother to cull
                                        } else {
                                            aabbMin.set(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
                                            aabbMax.set(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
                                            boolean visible = culling.isAABBVisible(aabbMin, aabbMax, camera);
        									cullable.setCulled(!visible);
                                        }
								    } else {
								        cullable.setCulled(false); // If your entity view distance is larger than 128 blocks just render it
								    }
								}
							}
						}
						lastTime = (System.currentTimeMillis()-start);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Shutting down culling task!");
	}
}
