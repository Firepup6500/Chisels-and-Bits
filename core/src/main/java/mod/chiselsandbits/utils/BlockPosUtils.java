package mod.chiselsandbits.utils;

import mod.chiselsandbits.api.multistate.StateEntrySize;
import net.minecraft.core.BlockPos;

public class BlockPosUtils
{

    private BlockPosUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: BlockPosUtils. This is a utility class");
    }

    public static int getCollisionIndex(final BlockPos blockPos) {
        return getCollisionIndex(blockPos.getX(), blockPos.getY(), blockPos.getZ(), StateEntrySize.current());
    }

    public static int getCollisionIndex(final int x, final int y, final int z) {
        return getCollisionIndex(x, y, z, StateEntrySize.current());
    }

    public static int getCollisionIndex(final BlockPos blockPos, final StateEntrySize size) {
        return getCollisionIndex(blockPos.getX(), blockPos.getY(), blockPos.getZ(), size);
    }

    public static int getCollisionIndex(final int x, final int y, final int z, final StateEntrySize size) {
        return x * (size.getBitsPerLayer()) +
                 y * (size.getBitsPerBlockSide()) +
                 z;
    }

    public static int getCollisionIndex(final BlockPos blockPos,  final int ySize, final int zSize) {
        return getCollisionIndex(blockPos.getX(), blockPos.getY(), blockPos.getZ(), ySize, zSize);
    }

    public static int getCollisionIndex(final int x, final int y, final int z, final int ySize, final int zSize) {
        if (x < 0 || y < 0 || z < 0) {
            throw new IllegalArgumentException("x, y and z must be positive: " + x + ", " + y + ", " + z);
        }

        if (y >= ySize || z >= zSize) {
            throw new IllegalArgumentException("y and z must be smaller than the size: " + y + ", " + z + ", " + ySize + ", " + zSize);
        }

        return x * (ySize * zSize) +
                 y * (zSize) +
                 z;
    }
}
