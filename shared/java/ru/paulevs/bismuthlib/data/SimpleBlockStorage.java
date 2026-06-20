package ru.paulevs.bismuthlib.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class SimpleBlockStorage implements BlockGetter {
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private MutableBlockPos pos = new MutableBlockPos();
	private BlockState[] storage = new BlockState[110592];
	
	public void fill(Level level, int x1, int y1, int z1) {
		int index = 0;
		for (byte dx = 0; dx < 48; dx++) {
			pos.setX(x1 + dx);
			for (byte dy = 0; dy < 48; dy++) {
				pos.setY(y1 + dy);
				for (byte dz = 0; dz < 48; dz++) {
					pos.setZ(z1 + dz);
					storage[index++] = level.getBlockState(pos);
				}
			}
		}
		pos.set(x1, y1, z1);
	}
	
	public void fillFromSections(Level level, int secX, int secY, int secZ) {
		LevelChunkSection[] chunkSections = null;
		int lastCx = Integer.MIN_VALUE;
		int lastCz = Integer.MIN_VALUE;
		for (int sx = -1; sx <= 1; sx++) {
			int cx = secX + sx;
			int storageXBase = (sx + 1) << 4;
			for (int sz = -1; sz <= 1; sz++) {
				int cz = secZ + sz;
				if (cx != lastCx || cz != lastCz) {
					LevelChunk chunk = level.getChunk(cx, cz);
					chunkSections = chunk == null ? null : chunk.getSections();
					lastCx = cx;
					lastCz = cz;
				}
				if (chunkSections == null) continue;
				int storageZBase = (sz + 1) << 4;
				for (int sy = -1; sy <= 1; sy++) {
					int cy = secY + sy;
					int sectionIndex = cy - level.getMinSection();
					if (sectionIndex < 0 || sectionIndex >= chunkSections.length) continue;
					LevelChunkSection section = chunkSections[sectionIndex];
					if (section == null) continue;
					int storageYBase = (sy + 1) << 4;
					for (int lx = 0; lx < 16; lx++) {
						int xIdx = (storageXBase + lx) * 2304;
						for (int ly = 0; ly < 16; ly++) {
							int xyIdx = xIdx + (storageYBase + ly) * 48;
							for (int lz = 0; lz < 16; lz++) {
								storage[xyIdx + storageZBase + lz] = section.getBlockState(lx, ly, lz);
							}
						}
					}
				}
			}
		}
		pos.set(secX << 4, secY << 4, secZ << 4);
		pos.move(-16, -16, -16);
	}
	
	private int getIndex(int x, int y, int z) {
		return  x * 2304 + y * 48 + z;
	}
	
	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos blockPos) {
		return null;
	}
	
	@Override
	public BlockState getBlockState(BlockPos blockPos) {
		int px = blockPos.getX() - pos.getX();
		int py = blockPos.getY() - pos.getY();
		int pz = blockPos.getZ() - pos.getZ();
		if (px < 0 || px > 47 || py < 0 || py > 47 || pz < 0 || pz > 47) return AIR;
		return storage[getIndex(px, py, pz)];
	}
	
	@Override
	public FluidState getFluidState(BlockPos blockPos) {
		return null;
	}
	
	@Override
	public int getHeight() {
		return 0;
	}
	
	@Override
	public int getMinBuildHeight() {
		return 0;
	}
}
