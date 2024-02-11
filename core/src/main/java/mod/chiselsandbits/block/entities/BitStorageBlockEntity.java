package mod.chiselsandbits.block.entities;

import com.communi.suggestu.scena.core.blockstate.ILevelBasedPropertyAccessor;
import com.communi.suggestu.scena.core.fluid.FluidInformation;
import com.communi.suggestu.scena.core.fluid.IFluidManager;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.blockinformation.BlockInformation;
import mod.chiselsandbits.api.chiseling.eligibility.IEligibilityManager;
import mod.chiselsandbits.api.inventory.bit.IBitInventory;
import mod.chiselsandbits.api.inventory.management.IBitInventoryManager;
import mod.chiselsandbits.api.item.bit.IBitItem;
import mod.chiselsandbits.api.item.bit.IBitItemManager;
import mod.chiselsandbits.api.multistate.StateEntrySize;
import mod.chiselsandbits.api.util.constants.NbtConstants;
import mod.chiselsandbits.api.variant.state.IStateVariantManager;
import mod.chiselsandbits.api.util.SingleBlockLevelReader;
import mod.chiselsandbits.block.BitStorageBlock;
import mod.chiselsandbits.registrars.ModBlockEntityTypes;
import mod.chiselsandbits.utils.BitInventoryUtils;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class BitStorageBlockEntity extends BlockEntity implements Container
{
    private IBlockInformation state = null;
    private int        bits  = 0;

    private int oldLV = -1;

    public BitStorageBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntityTypes.BIT_STORAGE.get(), pos, state);
    }

    @Override
    public void load(final @NotNull CompoundTag nbt)
    {
        super.load(nbt);

        final CompoundTag tag = nbt.getCompound(NbtConstants.BLOCK_INFORMATION);
        state = new BlockInformation(tag);
        bits = nbt.getInt(NbtConstants.BITS);
    }

    @Override
    public void saveAdditional(final @NotNull CompoundTag compound)
    {
        super.saveAdditional(compound);

        if (state != null) {
            compound.put(NbtConstants.BLOCK_INFORMATION, state.serializeNBT());
            compound.putInt(NbtConstants.BITS, bits);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag()
    {
        return saveWithFullMetadata();
    }

    public boolean addAllPossibleBits(
      final Player playerIn)
    {
        if (playerIn != null && playerIn.isShiftKeyDown() && state != null && !state.isAir())
        {
            final IBitInventory bitInventory = IBitInventoryManager.getInstance().create(playerIn);
            final int extractionAmount = Math.min(
              StateEntrySize.current().getBitsPerBlock() - bits,
              bitInventory.getMaxExtractAmount(state)
            );

            bitInventory.extract(state, extractionAmount);

            bits += extractionAmount;
            saveAndUpdate();
        }

        return false;
    }

    private void saveAndUpdate()
    {
        if (level == null || getLevel() == null)
        {
            return;
        }

        if (bits == 0)
        {
            this.state = null;
        }

        if (state == null)
        {
            this.bits = 0;
        }

        setChanged();
        getLevel().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);

        final int lv = getLightValue();
        if (oldLV != lv)
        {
            getLevel().getLightEngine().checkBlock(getBlockPos());
            oldLV = lv;
        }
    }

    public int getLightValue()
    {
        return ILevelBasedPropertyAccessor.getInstance().getLightEmission(
          new SingleBlockLevelReader(
            state == null ? BlockInformation.AIR : state,
            getBlockPos(),
            getLevel()
          ),
          getBlockPos()
        );
    }

    public boolean addHeldBits(
      final ItemStack current,
      final Player playerIn)
    {
        if (current.isEmpty())
        {
            return false;
        }

        if (playerIn.isShiftKeyDown() || this.bits == 0)
        {
            if (current.getItem() instanceof IBitItem bitItem)
            {
                if (bitItem.getBlockInformation(current) == state || state == null)
                {
                    state = bitItem.getBlockInformation(current);
                    final int maxToInsert = StateEntrySize.current().getBitsPerBlock() - bits;
                    final int toInsert = Math.min(maxToInsert, current.getCount());

                    bits += toInsert;

                    if (!playerIn.isCreative())
                    {
                        current.shrink(toInsert);
                        playerIn.getInventory().setItem(playerIn.getInventory().selected, current);
                        playerIn.getInventory().setChanged();
                    }
                    saveAndUpdate();
                    return true;
                }
            }
            else if (IEligibilityManager.getInstance().canBeChiseled(current))
            {
                final IBlockInformation stackState = ItemStackUtils.getStateFromItem(current);
                if (stackState.getBlockState().getBlock() != Blocks.AIR)
                {
                    if (this.state == null || state.isAir())
                    {
                        this.state = stackState;
                        this.bits = StateEntrySize.current().getBitsPerBlock();

                        if (!playerIn.isCreative())
                        {
                            current.shrink(1);
                            playerIn.getInventory().setItem(playerIn.getInventory().selected, current);
                            playerIn.getInventory().setChanged();
                        }
                        saveAndUpdate();
                        return true;
                    }
                }
            }


            final Optional<FluidInformation> containedFluid = IFluidManager.getInstance().get(current);
            if (containedFluid.isPresent() && containedFluid.get().amount() > 0)
            {
                final BlockState state = containedFluid.get().fluid().defaultFluidState().createLegacyBlock();
                final BlockInformation blockInformation = new BlockInformation(state, IStateVariantManager.getInstance().getStateVariant(containedFluid.get()));

                if (IEligibilityManager.getInstance().canBeChiseled(blockInformation))
                {
                    if (this.state == null || blockInformation.isAir())
                    {
                        final int maxToInsert = StateEntrySize.current().getBitsPerBlock() - bits;
                        final int toInsert = (int) Math.min(maxToInsert, getBitCountFrom(containedFluid.get()));

                        this.state = blockInformation;
                        this.bits += toInsert;

                        if (!playerIn.isCreative())
                        {
                            final ItemStack resultStack = IFluidManager.getInstance().extractFrom(current, toInsert);
                            playerIn.getInventory().setItem(playerIn.getInventory().selected, resultStack);
                            playerIn.getInventory().setChanged();
                        }
                        saveAndUpdate();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private float getBitCountFrom(final FluidInformation containedFluid)
    {
        return StateEntrySize.current()
          .getBitsPerBlock() * (containedFluid.amount() / (float) IFluidManager.getInstance().getBucketAmount());
    }

    public boolean extractBits(
      final Player playerIn
    )
    {
        if (!playerIn.isShiftKeyDown())
        {
            final ItemStack is = getItem(0);
            if (!is.isEmpty())
            {
                if (is.getItem() instanceof final IBitItem bitItem)
                {
                    final IBlockInformation blockState = bitItem.getBlockInformation(is);

                    BitInventoryUtils.insertIntoOrSpawn(
                      playerIn,
                      blockState,
                      is.getCount()
                    );

                    removeItem(0, is.getCount());
                }
            }
            return true;
        }

        return false;
    }

    public IBlockInformation getContainedBlockInformation()
    {
        return state;
    }

    public int getBits()
    {
        return bits;
    }

    public Direction getFacing()
    {
        return getLevel().getBlockState(getBlockPos()).getValue(BitStorageBlock.FACING);
    }

    @Override
    public int getContainerSize()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return state == null || bits == 0;
    }

    @Override
    public @NotNull ItemStack getItem(final int index)
    {
        if (index != 0)
        {
            return ItemStack.EMPTY;
        }

        if (state == null)
        {
            return ItemStack.EMPTY;
        }

        return IBitItemManager.getInstance().create(state, Math.min(64, bits));
    }

    @Override
    public @NotNull ItemStack removeItem(final int index, final int count)
    {
        if (index != 0)
        {
            return ItemStack.EMPTY;
        }

        final IBlockInformation currentState = state;
        final int toRemove = Math.min(count, bits);
        bits -= toRemove;

        saveAndUpdate();
        return IBitItemManager.getInstance().create(currentState, toRemove);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(final int index)
    {
        //Not supported
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(final int index, final @NotNull ItemStack itemStack)
    {
        if (index != 0 || !(itemStack.getItem() instanceof IBitItem) || ((IBitItem) itemStack.getItem()).getBlockInformation(itemStack) == state)
        {
            return;
        }

        saveAndUpdate();
        bits = Math.max(StateEntrySize.current().getBitsPerBlock(), bits + itemStack.getCount());
    }

    @Override
    public boolean stillValid(final @NotNull Player player)
    {
        if (this.level.getBlockEntity(this.worldPosition) != this)
        {
            return false;
        }
        else
        {
            return !(player.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D, (double) this.worldPosition.getZ() + 0.5D) > 64.0D);
        }
    }

    @Override
    public void clearContent()
    {
        this.state = null;
        this.bits = 0;
        saveAndUpdate();
    }

    public Optional<FluidInformation> getFluid()
    {
        if (!containsFluid())
        {
            return Optional.empty();
        }

        final long amount = (long) (bits / (StateEntrySize.current().getBitsPerBlock() / (float) IFluidManager.getInstance().getBucketAmount()));
        final Optional<FluidInformation> dynamicFluid = IStateVariantManager.getInstance()
                                                          .getFluidInformation(this.state, amount);

        if (dynamicFluid.isPresent())
            return dynamicFluid;

        return Optional.of(new FluidInformation(
          ((LiquidBlock) state.getBlockState().getBlock()).getFluidState(state.getBlockState()).getType(),
          amount,
          new CompoundTag()
        ));
    }

    public boolean containsFluid()
    {
        if (this.state == null)
        {
            return false;
        }

        final long amount = (long) (bits / (StateEntrySize.current().getBitsPerBlock() / (float) IFluidManager.getInstance().getBucketAmount()));
        final Optional<FluidInformation> dynamicFluid = IStateVariantManager.getInstance()
          .getFluidInformation(this.state, amount);

        if (dynamicFluid.isPresent())
            return true;

        return state.getBlockState().getBlock() instanceof LiquidBlock liquidBlock &&
                 !liquidBlock.getFluidState(state.getBlockState()).isEmpty();
    }

    public void extractBits(final int count)
    {
        this.bits = Math.max(0, this.bits - count);
        if (this.bits <= 0)
        {
            this.state = null;
        }
        saveAndUpdate();
    }

    public void insertBits(final int bitCountToInsert, final IBlockInformation blockInformation)
    {
        if (state == null || blockInformation.equals(state))
        {
            this.bits = Math.max(StateEntrySize.current().getBitsPerBlock(), bitCountToInsert + bits);
            this.state = blockInformation;
            saveAndUpdate();
        }
    }

    public void insertBitsFromFluid(final FluidInformation fluidInformation)
    {
        final BlockInformation fluidBlockInformation = new BlockInformation(
          fluidInformation.fluid().defaultFluidState().createLegacyBlock(),
          IStateVariantManager.getInstance().getStateVariant(fluidInformation)
        );

        if (state == null || state.equals(fluidBlockInformation))
        {
            this.bits = (int) Math.max(StateEntrySize.current().getBitsPerBlock(), getBitCountFrom(fluidInformation) + bits);
            this.state = fluidBlockInformation;
            saveAndUpdate();
        }
    }

    public void setContents(final BlockInformation blockInformation, final int count)
    {
        this.state = blockInformation;
        this.bits = count;
        saveAndUpdate();
    }
}
