package mod.chiselsandbits.api.item.wireframe;

import com.mojang.math.Vector3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Represents an item which can provide a wireframe for different purposes,
 * including rendering a preview.
 */
public interface IWireframeProvidingItem
{
    /**
     * Provides access to the wire frame of the item.
     *
     * @param stack The stack to get the wire frame from.
     * @param player The player to get the wire frame for.
     * @param rayTraceResult The ray trace result of the current context.
     * @return The VoxelShape for the wire frame.
     */
    VoxelShape getWireFrame(
      final ItemStack stack,
      final Player player,
      final BlockHitResult rayTraceResult
    );

    /**
     * The color to render the wireframe in.
     *
     * @param heldStack The stack to get the wire frame color for.
     * @param playerEntity The entity that is rendering the wire frame color.
     * @param blockRayTraceResult The block ray trace result for the current context.
     * @return An RGB (XYZ) Vector with the color.
     */
    Vec3 getWireFrameColor(
      ItemStack heldStack,
      Player playerEntity,
      BlockHitResult blockRayTraceResult
    );

    /**
     * Returns the position the wire frame should be rendered at.
     *
     * @param heldStack The stack to get the position for.
     * @param playerEntity The entity that is rendering the wire frame.
     * @param blockRayTraceResult The block ray trace result for the current context.
     * @return The position to render the wire frame.
     */
    Vec3 getTargetedBlockPos(
      ItemStack heldStack,
      Player playerEntity,
      BlockHitResult blockRayTraceResult
    );
}
