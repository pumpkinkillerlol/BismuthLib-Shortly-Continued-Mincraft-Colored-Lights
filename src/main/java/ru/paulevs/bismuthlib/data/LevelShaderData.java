package ru.paulevs.bismuthlib.data;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import ru.paulevs.bismuthlib.LightPropagator;
import ru.paulevs.bismuthlib.gui.CFOptions;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Environment(EnvType.CLIENT)
public class LevelShaderData {
	private final Set<BlockPos> updateSections = new HashSet<>();
	private final Set<BlockPos> delayedSections = new HashSet<>();
	private final MutableBlockPos lastCenter = new MutableBlockPos();
	private final ShaderSectionData[][][] data;
	private final DynamicTexture texture;
	private final int textureSide;
	private final int dataHeight;
	private final int halfHeight;
	private final int dataWidth;
	private final int halfWidth;
	private final int dataSide;
	private Level level;
	
	private final ArrayBlockingQueue<BlockPos>[] innerUpdates;
	private final Thread[] threads;
	private final AtomicInteger pendingUpdates = new AtomicInteger(0);
	private final Set<Long> dirtyRegions = new HashSet<>();
	private byte updateTicks = 0;
	private byte mapUpdate = 0;
	private boolean upload;
	private boolean run;
	
	public LevelShaderData(int dataWidth, int dataHeight, int threadCount) {
		this.data = new ShaderSectionData[dataWidth][dataHeight][dataWidth];
		this.halfHeight = dataHeight >> 1;
		this.halfWidth = dataWidth >> 1;
		
		for (int x = 0; x < dataWidth; x++) {
			for (int y = 0; y < dataHeight; y++) {
				for (int z = 0; z < dataWidth; z++) {
					data[x][y][z] = new ShaderSectionData();
				}
			}
		}
		
		this.dataSide = (int) Math.ceil(Mth.sqrt(dataWidth * dataWidth * dataHeight));
		textureSide = getClosestPowerOfTwo(dataSide << 6);
		texture = new DynamicTexture(textureSide, textureSide, false);
		this.dataHeight = dataHeight;
		this.dataWidth = dataWidth;
		
		Thread main = Thread.currentThread();
		
		run = true;
		innerUpdates = new ArrayBlockingQueue[threadCount];
		LightPropagator[] propagators = new LightPropagator[threadCount];
		threads = new Thread[threadCount];
		int queueCapacity = dataWidth * dataWidth * dataHeight;
		for (byte i = 0; i < threadCount; i++) {
			final ArrayBlockingQueue<BlockPos> updates = new ArrayBlockingQueue<>(queueCapacity);
			final LightPropagator propagator = new LightPropagator();
			innerUpdates[i] = updates;
			propagators[i] = propagator;
			threads[i] = new Thread(() -> {
				while (run && main.isAlive()) {
					try {
						BlockPos pos = updates.poll(10, TimeUnit.MILLISECONDS);
						if (pos != null) {
							try {
								updateSection(level, pos.getX(), pos.getY(), pos.getZ(), true, propagator);
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								pendingUpdates.decrementAndGet();
							}
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			});
			threads[i].setName("colored_lights_" + i);
			threads[i].start();
		}
	}
	
	public void dispose() {
		run = false;
		for (Thread t: threads) {
			t.interrupt();
		}
		for (Thread t: threads) {
			while (t.isAlive());
		}
		texture.close();
	}
	
	public void resetAll() {
		if (level == null) return;
		short hmin = (short) level.getMinSectionY();
		short hmax = (short) level.getMaxSectionY();
		for (int i = 0; i < dataWidth; i++) {
			int px = lastCenter.getX() - halfWidth + i;
			for (int j = 0; j < dataWidth; j++) {
				int pz = lastCenter.getZ() - halfWidth + j;
				for (int k = 0; k < dataHeight; k++) {
					int py = lastCenter.getY() - halfHeight + k;
					if (py < hmin || py > hmax) continue;
					markToUpdate(px, py, pz);
				}
			}
		}
	}
	
	public DynamicTexture getTexture() {
		return texture;
	}
	
	public int getDataWidth() {
		return dataWidth;
	}
	
	public int getDataHeight() {
		return dataHeight;
	}
	
	public int getDataSide() {
		return dataSide;
	}
	
	public void markToUpdate(int x, int y, int z) {
		updateSections.add(new BlockPos(x, y, z));
	}
	
	private boolean updateSection(Level level, int x, int y, int z, boolean force, LightPropagator propagator) {
		if (y < level.getMinSectionY() || y > level.getMaxSectionY()) return false;
		int indexX = Math.abs(x - lastCenter.getX());
		int indexY = Math.abs(y - lastCenter.getY());
		int indexZ = Math.abs(z - lastCenter.getZ());
		if (indexX > halfWidth || indexZ > halfWidth || indexY > halfHeight) return false;
		indexX = wrap(x, dataWidth);
		indexY = wrap(y, dataHeight);
		indexZ = wrap(z, dataWidth);
		if (indexX < 0 || indexX >= dataWidth || indexY < 0 || indexY >= dataHeight || indexZ < 0 || indexZ >= dataWidth) {
			System.err.println("[BM] WRAP OOB: dw=" + dataWidth + " dh=" + dataHeight + " x=" + x + " y=" + y + " z=" + z + " iX=" + indexX + " iY=" + indexY + " iZ=" + indexZ);
			return false;
		}
		ShaderSectionData section;
		try {
			section = data[indexX][indexY][indexZ];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("[BM] DATA OOB: dw=" + dataWidth + " dh=" + dataHeight + " iX=" + indexX + " iY=" + indexY + " iZ=" + indexZ);
			throw e;
		}
		if (force || !section.hasCorrectPosition(x, y, z)) {
			section.setPosition(x, y, z);
			int[] sectionData = section.getData();
			try {
				if (CFOptions.isFastLight()) {
					propagator.fastLight(level, new BlockPos(x, y, z), sectionData);
				}
				else {
					propagator.advancedLight(level, new BlockPos(x, y, z), sectionData);
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				System.err.println("[BM] PROP OOB: sec=" + x + "," + y + "," + z + " lv.min=" + level.getMinSectionY() + " lv.max=" + level.getMaxSectionY());
				throw e;
			}
			int index = ((indexX * dataHeight) + indexY) * dataWidth + indexZ;
			int textureX = (index % dataSide) << 6;
			int textureY = (index / dataSide) << 6;
			short dataIndex = 0;
			synchronized (texture) {
				NativeImage image = texture.getPixels();
				try {
					for (byte j = 0; j < 64; j++) {
						for (byte i = 0; i < 64; i++) {
							int color = sectionData[dataIndex++];
							int a = (color >> 24) & 255;
							int r = (color >> 16) & 255;
							int g = (color >> 8) & 255;
							int b = color & 255;
							image.setPixel(textureX | i, textureY | j, (a << 24) | (b << 16) | (g << 8) | r);
						}
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					System.err.println("[BM] PIX OOB: texX=" + textureX + " texY=" + textureY + " side=" + textureSide + " ds=" + dataSide);
					throw e;
				}
				dirtyRegions.add(((long) textureX << 32) | (textureY & 0xFFFFFFFFL));
			}
			upload = true;
			return true;
		}
		return false;
	}
	
	public void update(Level level, int cx, int cy, int cz) {
		boolean force = level != this.level;
		this.level = level;
		if (lastCenter.getX() != cx || lastCenter.getY() != cy || lastCenter.getZ() != cz) {
			lastCenter.set(cx, cy, cz);
			short hmin = (short) level.getMinSectionY();
			short hmax = (short) level.getMaxSectionY();
			for (int i = 0; i < dataWidth; i++) {
				int px = cx - halfWidth + i;
				for (int j = 0; j < dataWidth; j++) {
					int pz = cz - halfWidth + j;
					for (int k = 0; k < dataHeight; k++) {
						int py = cy - halfHeight + k;
						if (py < hmin || py > hmax) continue;
						if (force) {
							markToUpdate(px, py, pz);
							continue;
						}
						int indexX = wrap(px, dataWidth);
						int indexY = wrap(py, dataHeight);
						int indexZ = wrap(pz, dataWidth);
						ShaderSectionData section = data[indexX][indexY][indexZ];
						if (!section.hasCorrectPosition(px, py, pz)) {
							markToUpdate(px, py, pz);
						}
					}
				}
			}
		}
		
		if (updateTicks++ > 4) {
			synchronized (texture) {
				if (!dirtyRegions.isEmpty()) {
					texture.bind();
					NativeImage image = texture.getPixels();
					for (long packed : dirtyRegions) {
						int texX = (int) (packed >> 32);
						int texY = (int) (packed & 0xFFFFFFFFL);
						image.upload(0, texX, texY, texX, texY, 64, 64, false);
					}
					dirtyRegions.clear();
				}
			}
			
			updateTicks = 0;
			int dispatched = 0;
			for (BlockPos pos: updateSections) {
				int index = getMultiIndex(pos);
				if (innerUpdates[index].remainingCapacity() > 0) {
					innerUpdates[index].add(pos);
					dispatched++;
				}
				else {
					delayedSections.add(pos);
				}
			}
			if (dispatched > 0) pendingUpdates.addAndGet(dispatched);
			updateSections.clear();
			updateSections.addAll(delayedSections);
			delayedSections.clear();
		}
	}
	
	private int getMultiIndex(BlockPos pos) {
		if (threads.length == 1) return 0;
		return Math.floorMod(pos.getX() + pos.getY() + pos.getZ(), threads.length);
	}
	
	public BlockPos getCenter() {
		return lastCenter;
	}
	
	public int getThreadCount() {
		return threads.length;
	}
	
	private int getClosestPowerOfTwo(int value) {
		if (value <= 0) return 0;
		int highest = Integer.highestOneBit(value);
		return highest == value ? highest : highest << 1;
	}
	
	private int wrap(int value, int side) {
		return Math.floorMod(value, side);
	}
}
