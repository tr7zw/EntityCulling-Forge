package dev.tr7zw.entityculling;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import dev.tr7zw.entityculling.access.Cullable;
import dev.tr7zw.entityculling.occlusionculling.AxisAlignedBB;
import dev.tr7zw.entityculling.occlusionculling.OcclusionCullingInstance;
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
	private final AxisAlignedBB blockAABB = new AxisAlignedBB(0d, 0d, 0d, 1d, 1d, 1d);
	private final int sleepDelay = 10;
	private final Set<TileEntityType<?>> unCullable;
	private Vector3d lastPos = new Vector3d(0, 0, 0);
	private long lastTime = 0;

	public CullTask(OcclusionCullingInstance culling, Set<TileEntityType<?>> unCullable) {
		this.culling = culling;
		this.unCullable = unCullable;
	}
	
	@Override
	public void run() {
		while (client.isRunning()) {
			try {
				Thread.sleep(sleepDelay);

				if (client.world != null && client.player != null && client.player.ticksExisted > 10) {
					Vector3d camera = EntityCullingMod.instance.debug
							? client.player.getEyePosition(client.getRenderPartialTicks())
							: client.gameRenderer.getActiveRenderInfo().getProjectedView();
					if (requestCull || !lastPos.equals(camera)) {
						long start = System.currentTimeMillis();
						requestCull = false;
						lastPos = camera;
						culling.resetCache();
						boolean spectator = client.player.isSpectator();
						for (int x = -3; x <= 3; x++) {
							for (int z = -3; z <= 3; z++) {
								Chunk chunk = client.world.getChunk(client.player.chunkCoordX + x,
										client.player.chunkCoordZ + z);
								for (Entry<BlockPos, TileEntity> entry : chunk.getTileEntityMap().entrySet()) {
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
										boolean visible = culling.isAABBVisible(
												new Vector3d(pos.getX(), pos.getY(), pos.getZ()), blockAABB, camera,
												false);
										cullable.setCulled(!visible);
									}
								}

							}
						}
						Entity entity = null;
						Iterator<Entity> iterable = client.world.getAllEntities().iterator();
						while (iterable.hasNext()) {
							try {
								entity = iterable.next();
							} catch (NullPointerException npe) {
								break; // We are not synced to the main thread, so NPE's are allowed here and way less
										// overhead probably than trying to sync stuff up for no really good reason
							}
							Cullable cullable = (Cullable) entity;
							if (!cullable.isForcedVisible()) {
								if (spectator || entity.isGlowing()) {
									cullable.setCulled(false);
								} else {
									net.minecraft.util.math.AxisAlignedBB boundingBox = entity.getRenderBoundingBox();
									boolean visible = culling.isAABBVisible(
											new Vector3d(entity.getPosX(), entity.getPosY(),
													entity.getPosZ()),
											new AxisAlignedBB(boundingBox.minX - 0.05, boundingBox.minY,
													boundingBox.minZ - 0.05, boundingBox.maxX + 0.05, boundingBox.maxY,
													boundingBox.maxZ + 0.05),
											camera, true);
									cullable.setCulled(!visible);
								}
							}
						}
						lastTime = (System.currentTimeMillis()-start);
					}
					if(!client.debug.contains("CullTime"))
						client.debug += " CullTime: " + lastTime + "ms"; // Bit hacky, but works for now :shrug:
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
