package mod.chiselsandbits.api.multistate.accessor.sortable;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;

/**
 * Represents an object which can mutate the order of the coordinate members
 * before they are retrieved from storage.
 * <p>
 * The default loop order is XYZ.
 * However, by swapping for example the X and Y coordinate member of the
 * passed coordinates a YXZ loop order can be achieved without
 * the performance degradation of sorting.
 */
@FunctionalInterface
public interface IPositionMutator
{
    /**
     * Applies the mutation that this mutator performs on a given position.
     *
     * @param input The input position.
     * @param size The size of the area that is being iterated over.
     *
     * @return The mutated output.
     */
    Vec3i mutate(final Vec3i input, AABB size);

    /**
     * Creates a new mutator which chains the current and the next mutator into one.
     *
     * @param next The mutator that needs to be applied after the current mutator.
     *
     * @return The combined mutator.
     */
    default IPositionMutator then(final IPositionMutator next) {
        return (input, size) -> next.mutate(this.mutate(input, size), size);
    }

    /**
     * The identity operator.
     * Performs no change to the passed in position.
     *
     * @return An identity position mutator.
     */
    static IPositionMutator identity() {
        return (input, size) -> input;
    }

    /**
     * The xyz operator.
     * Generally also perceived as the identity mutator.
     *
     * @return The identity mutator.
     */
    static IPositionMutator xyz() {
        return (input, size) -> new Vec3i(input.getX(), input.getY(), input.getZ());
    }

    /**
     * The xzy operator.
     *
     * @return The mutator which switches the Y and Z coordinate members
     */
    static IPositionMutator xzy() {
        return (input, size) -> new Vec3i(input.getX(), input.getZ(), input.getY());
    }

    /**
     * The zyx operator.
     *
     * @return The mutator which switches the X and Z coordinate members
     */
    static IPositionMutator zyx() {
        return (input, size) -> new Vec3i(input.getZ(), input.getY(), input.getX());
    }

    /**
     * The yxz operator.
     *
     * @return The mutator which switches the X and Y coordinate members
     */
    static IPositionMutator yxz() {
        return (input, size) -> new Vec3i(input.getY(), input.getX(), input.getZ());
    }

    /**
     * The zxy operator.
     *
     * @return The mutator which switches the X with the Z and then the Y with the moved X coordinate members
     */
    static IPositionMutator zxy() {
        return (input, size) -> new Vec3i(input.getZ(), input.getX(), input.getY());
    }

    /**
     * The yzx operator
     * Is a combination of xzy and zyx.
     *
     * @return The mutator which first switches the Y and Z and then the X and Z coordinates.
     */
    static IPositionMutator yzx() {
        return zyx().then(yxz());
    }

    /**
     * Returns the mutator which first iterates over the given direction and then over the others.
     * The order of the other axi is not fixed.
     *
     * @param axis The axis to iterate over first.
     * @return The position mutator for the given axis.
     */
    static IPositionMutator fromAxis(final Direction.Axis axis) {
        return switch (axis)
                 {
                     case X -> IPositionMutator.xyz();
                     case Y -> IPositionMutator.yxz();
                     case Z -> IPositionMutator.zxy();
                 };
    }
}
