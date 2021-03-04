package dev.tr7zw.entityculling.occlusionculling;

import java.util.Arrays;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.chunk.Chunk;

public class OcclusionCullingInstance {
	
	public boolean isAABBVisible(Vector3d aabbBlock, AxisAlignedBB aabb, Vector3d playerLoc, boolean entity) {
		try {
			if (entity) {
				aabb.maxx -= aabbBlock.x;
				aabb.minx -= aabbBlock.x;
				aabb.maxy -= aabbBlock.y;
				aabb.miny -= aabbBlock.y;
				aabb.maxz -= aabbBlock.z;
				aabb.minz -= aabbBlock.z;
			}
			aabbBlock = aabbBlock.subtract(((int)playerLoc.x), ((int)playerLoc.y), ((int)playerLoc.z));
			int maxX = (int) Math.ceil(aabbBlock.x + aabb.maxx + 0.25);
			int maxY = (int) Math.ceil(aabbBlock.y + aabb.maxy + 0.25);
			int maxZ = (int) Math.ceil(aabbBlock.z + aabb.maxz + 0.25);
			int minX = (int) Math.floor(aabbBlock.x + aabb.minx - 0.25);
			int minY = (int) Math.floor(aabbBlock.y + aabb.miny - 0.25);
			int minZ = (int) Math.floor(aabbBlock.z + aabb.minz - 0.25);

			for (int x = minX; x < maxX; x++) {
				if (!(x == minX || x == maxX - 1))
					continue;
				for (int y = minY; y < maxY; y++) {
					if (!(y == minY || y == maxY - 1))
						continue;
					for (int z = minZ; z < maxZ; z++) {
						if (!(z == minZ || z == maxZ - 1))
							continue;
						if (isVoxelVisible(playerLoc, new Vector3d(x, y, z))) {
							return true;
						}
					}
				}
			}

			return false;

		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return true;
	}

	private boolean isVoxelVisible(Vector3d playerLoc, Vector3d position) {
		Vector3d[] targets = new Vector3d[8];
		targets[0] = position;
		targets[1] = position.add(0.95, 0, 0);
		targets[2] = position.add(0, 0.95, 0);
		targets[3] = position.add(0.95, 0.95, 0);
		targets[4] = position.add(0, 0, 0.95);
		targets[5] = position.add(0.95, 0, 0.95);
		targets[6] = position.add(0, 0.95, 0.95);
		targets[7] = position.add(0.95, 0.95, 0.95);
		return isVisible(playerLoc, targets);
	}
	
	private final int reach = 64;
	private final byte[] cache = new byte[((reach*2)*(reach*2)*(reach*2))/4];
	
	public void resetCache() {
		Arrays.fill(cache, (byte)0);
	}
	
	/**
	 * returns the grid cells that intersect with this Vector3d<br>
	 * <a href=
	 * "http://playtechs.blogspot.de/2007/03/raytracing-on-grid.html">http://playtechs.blogspot.de/2007/03/raytracing-on-grid.html</a>
	 * 
	 * Caching assumes that all Vector3d's are inside the same block
	 */
	private boolean isVisible(Vector3d start, Vector3d[] targets) {
		int maxX = 0;
		int maxY = 0;
		int maxZ = 0;
		for(int i = 0; i < targets.length; i++) {
			maxX = Math.max(maxX, (int)Math.abs(targets[i].x));
			maxY = Math.max(maxY, (int)Math.abs(targets[i].y));
			maxZ = Math.max(maxZ, (int)Math.abs(targets[i].z));
		}
		if(maxX > reach - 2 || maxY > reach - 2 || maxZ > reach - 2)return false;
		
		for(int v = 0; v < targets.length; v++) {//check if target is already known
			Vector3d target = targets[v];
			int cx = (int) Math.floor(target.x + reach);
			int cy = (int) Math.floor(target.y + reach);
			int cz = (int) Math.floor(target.z + reach);
			int keyPos = cx + cy * (reach*2) + cz*(reach*2)*(reach*2);
			int entry = keyPos/4;
			int offset = (keyPos%4)*2;
			int cVal = cache[entry] >> offset & 3;
			if(cVal == 2) {
				return false;
			}else if(cVal == 1) {
				return true;
			}
		}
		
		for(int v = 0; v < targets.length; v++) {
			Vector3d target = targets[v];
			// coordinates of start and target point
			double x0 = start.getX();
			double y0 = start.getY();
			double z0 = start.getZ();
			double x1 = x0 + target.getX();
			double y1 = y0 + target.getY();
			double z1 = z0 + target.getZ();
	
			// horizontal and vertical cell amount spanned
			double dx = Math.abs(x1 - x0);
			double dy = Math.abs(y1 - y0);
			double dz = Math.abs(z1 - z0);
	
			// start cell coordinate
			int x = (int) Math.floor(x0);
			int y = (int) Math.floor(y0);
			int z = (int) Math.floor(z0);
	
			// distance between horizontal intersection points with cell border as a
			// fraction of the total Vector3d length
			double dt_dx = 1f / dx;
			// distance between vertical intersection points with cell border as a fraction
			// of the total Vector3d length
			double dt_dy = 1f / dy;
			double dt_dz = 1f / dz;
	
			// total amount of intersected cells
			int n = 1;
	
			// 1, 0 or -1
			// determines the direction of the next cell (horizontally / vertically)
			int x_inc, y_inc, z_inc;
			// the distance to the next horizontal / vertical intersection point with a cell
			// border as a fraction of the total Vector3d length
			double t_next_y, t_next_x, t_next_z;
	
			if (dx == 0f) {
				x_inc = 0;
				t_next_x = dt_dx; // don't increment horizontally because the Vector3d is perfectly vertical
			} else if (x1 > x0) {
				x_inc = 1; // target point is horizontally greater than starting point so increment every
							// step by 1
				n += (int) Math.floor(x1) - x; // increment total amount of intersecting cells
				t_next_x = (float) ((Math.floor(x0) + 1 - x0) * dt_dx); // calculate the next horizontal intersection point based on the position inside
																		// the first cell
			} else {
				x_inc = -1; // target point is horizontally smaller than starting point so reduce every step
							// by 1
				n += x - (int) Math.floor(x1); // increment total amount of intersecting cells
				t_next_x = (float) ((x0 - Math.floor(x0)) * dt_dx); // calculate the next horizontal intersection point based on the position inside
																	// the first cell
			}
	
			if (dy == 0f) {
				y_inc = 0;
				t_next_y = dt_dy; // don't increment vertically because the Vector3d is perfectly horizontal
			} else if (y1 > y0) {
				y_inc = 1; // target point is vertically greater than starting point so increment every
							// step by 1
				n += (int) Math.floor(y1) - y; // increment total amount of intersecting cells
				t_next_y = (float) ((Math.floor(y0) + 1 - y0) * dt_dy); // calculate the next vertical intersection point based on the position inside
																		// the first cell
			} else {
				y_inc = -1; // target point is vertically smaller than starting point so reduce every step
							// by 1
				n += y - (int) Math.floor(y1); // increment total amount of intersecting cells
				t_next_y = (float) ((y0 - Math.floor(y0)) * dt_dy); // calculate the next vertical intersection point based on the position inside
																	// the first cell
			}
	
			if (dz == 0f) {
				z_inc = 0;
				t_next_z = dt_dz; // don't increment vertically because the Vector3d is perfectly horizontal
			} else if (z1 > z0) {
				z_inc = 1; // target point is vertically greater than starting point so increment every
							// step by 1
				n += (int) Math.floor(z1) - z; // increment total amount of intersecting cells
				t_next_z = (float) ((Math.floor(z0) + 1 - z0) * dt_dz); // calculate the next vertical intersection point based on the position inside
																		// the first cell
			} else {
				z_inc = -1; // target point is vertically smaller than starting point so reduce every step
							// by 1
				n += z - (int) Math.floor(z1); // increment total amount of intersecting cells
				t_next_z = (float) ((z0 - Math.floor(z0)) * dt_dz); // calculate the next vertical intersection point based on the position inside
																	// the first cell
			}
	
			boolean finished =stepRay(start, x0, y0, z0, x, y, z, dt_dx, dt_dy, dt_dz, n, x_inc, y_inc, z_inc, t_next_y, t_next_x,
					t_next_z);
			if(finished) {
				cacheResult(targets[0], true);
				return true;
			}
		}
		cacheResult(targets[0], false);
		return false;
	}
	
	private void cacheResult(Vector3d vector, boolean result) {
		int cx = (int) Math.floor(vector.x + reach);
		int cy = (int) Math.floor(vector.y + reach);
		int cz = (int) Math.floor(vector.z + reach);
		int keyPos = cx + cy * (reach*2) + cz*(reach*2)*(reach*2);
		int entry = keyPos/4;
		int offset = (keyPos%4)*2;
		if(result) {
			cache[entry] |= 1 << offset;
		} else {
			cache[entry] |= 1 << offset + 1;
		}
	}
	
	private boolean stepRay(Vector3d start, double x0, double y0, double z0, int x, int y,
			int z, double dt_dx, double dt_dy, double dt_dz, int n, int x_inc, int y_inc, int z_inc, double t_next_y,
			double t_next_x, double t_next_z) {
		int chunkX = 0;
		int chunkZ = 0;
		Chunk snapshot = null;
		@SuppressWarnings("resource")
		ClientWorld world =  Minecraft.getInstance().world;
		
		// iterate through all intersecting cells (n times)
		for (; n > 1; n--) { //n-1 times because we don't want to check the last block
			int cx = (int) Math.floor((x0 - x) + reach);
			int cy = (int) Math.floor((y0 - y) + reach);
			int cz = (int) Math.floor((z0 - z) + reach);

			int keyPos = cx + cy*(reach*2) + cz*(reach*2)*(reach*2);
			int entry = keyPos/4;
			int offset = (keyPos%4)*2;
			int cVal = cache[entry] >> offset & 3;
			if(cVal == 2) {
				return false;
			}
			if(cVal == 0) {
				// save current cell
				//Vector3d cp = new Vector3d(x, y, z);
				int tchunkX = (int) Math.floor(x / 16d);
				int tchunkZ = (int) Math.floor(z / 16d);
				if(snapshot == null || chunkX != tchunkX || chunkZ != tchunkZ) {
					chunkX = tchunkX;
					chunkZ = tchunkZ;
					snapshot = world.getChunk(chunkX, chunkZ);//CullingPlugin.instance.blockChangeListener.cachedChunkSnapshots.get(cc);
					if(snapshot == null) {
						//cache[cx][cy][cz] = 2;
						return false;
					}
				}
				
				int relativeX = x % 16;
				if (relativeX < 0) {
					relativeX = 16 + relativeX;
				}
				int relativeZ = z % 16;
				if (relativeZ < 0) {
					relativeZ = 16 + relativeZ;
				}
				if (relativeX < 0) {
					cache[entry] |= 1 << offset + 1;
					return false;
				}
				if (relativeZ < 0) {
					cache[entry] |= 1 << offset + 1;
					return false;
				}
				if (y < 0 || y > 255) {
					cache[entry] |= 1 << offset + 1;
					return false;
				}
				BlockPos pos = new BlockPos(x, y, z);
				BlockState state = snapshot.getBlockState(pos);
				if(state.isOpaqueCube(world, pos)) {
					cache[entry] |= 1 << offset + 1;
					return false;
				}
				cache[entry] |= 1 << offset;
			}

			if (t_next_y < t_next_x && t_next_y < t_next_z) { // next cell is upwards/downwards because the distance to the next vertical
				// intersection point is smaller than to the next horizontal intersection point
				y += y_inc; // move up/down
				t_next_y += dt_dy; // update next vertical intersection point
			} else if (t_next_x < t_next_y && t_next_x < t_next_z) { // next cell is right/left
				x += x_inc; // move right/left
				t_next_x += dt_dx; // update next horizontal intersection point
			} else {
				z += z_inc; // move right/left
				t_next_z += dt_dz; // update next horizontal intersection point
			}

		}
		return true;
	}

}
