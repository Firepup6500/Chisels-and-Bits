package mod.chiselsandbits.block.entities.storage;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import mod.chiselsandbits.api.blockinformation.IBlockInformation;
import mod.chiselsandbits.blockinformation.BlockInformation;
import mod.chiselsandbits.api.util.INBTSerializable;
import mod.chiselsandbits.api.util.IPacketBufferSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

public class SimpleStateEntryPalette implements IPacketBufferSerializable, INBTSerializable<ListTag>
{

    private final List<Entry>                    paletteEntries = Lists.newCopyOnWriteArrayList();
    private final Map<IBlockInformation, Entry> paletteMap     = Maps.newConcurrentMap();
    private final IntConsumer                    onNewSizeAddedConsumer;
    private final Consumer<Map<Integer, Integer>> onPaletteIndexChanged;

    public SimpleStateEntryPalette(final IntConsumer onNewSizeAddedConsumer, final Consumer<Map<Integer, Integer>> onPaletteIndexChanged) {
        this.onNewSizeAddedConsumer = onNewSizeAddedConsumer;
        this.onPaletteIndexChanged = onPaletteIndexChanged;
        clear(); //Reset to initial state
    }

    public SimpleStateEntryPalette(final IntConsumer onPaletteResize, final Consumer<Map<Integer, Integer>> onPaletteIndexChanged, final SimpleStateEntryPalette palette)
    {
        this.onNewSizeAddedConsumer = onPaletteResize;
        this.onPaletteIndexChanged = onPaletteIndexChanged;
        this.paletteEntries.addAll(palette.paletteEntries);
        this.paletteMap.putAll(palette.paletteMap);
    }

    @Override
    public ListTag serializeNBT()
    {
        return paletteEntries.stream().map(INBTSerializable::serializeNBT).collect(Collectors.toCollection(ListTag::new));
    }

    @Override
    public void deserializeNBT(final ListTag nbt)
    {
        final int currentSize = this.paletteEntries.size();
        this.paletteMap.clear();
        this.paletteEntries.clear();

        nbt.stream()
                .filter(CompoundTag.class::isInstance)
                .map(CompoundTag.class::cast)
                .map(Entry::new)
                .forEach(this.paletteEntries::add);

        this.paletteEntries.forEach(entry -> this.paletteMap.put(entry.get(), entry));

        if (paletteEntries.isEmpty()) {
            clear();
        }

        if (currentSize != this.paletteEntries.size()) {
            this.onNewSizeAddedConsumer.accept(this.paletteEntries.size());
        }
    }

    @Override
    public void serializeInto(final @NotNull FriendlyByteBuf packetBuffer)
    {
        packetBuffer.writeVarInt(this.paletteEntries.size());
        this.paletteEntries.forEach(entry -> entry.serializeInto(packetBuffer));
    }

    @Override
    public void deserializeFrom(final @NotNull FriendlyByteBuf packetBuffer)
    {
        final int currentSize = this.paletteEntries.size();

        this.paletteEntries.clear();
        this.paletteMap.clear();

        final int newCount = packetBuffer.readVarInt();
        for (int i = 0; i < newCount; i++)
        {
            this.paletteEntries.add(new Entry(packetBuffer));
        }

        this.paletteEntries.forEach(entry -> this.paletteMap.put(entry.get(), entry));

        if (paletteEntries.isEmpty()) {
            clear();
        }

        if (currentSize != this.paletteEntries.size()) {
            this.onNewSizeAddedConsumer.accept(this.paletteEntries.size());
        }
    }

    public int getIndex(final IBlockInformation state) {
        if (this.paletteMap.containsKey(state)) {
            final Entry entry = this.paletteMap.get(state);
            return this.paletteEntries.indexOf(entry);
        }

        final Entry newEntry = new Entry(state);
        this.paletteMap.put(state, newEntry);

        this.paletteEntries.add(newEntry);
        this.onNewSizeAddedConsumer.accept(this.paletteEntries.size());

        return this.paletteEntries.size() - 1;
    }

    public IBlockInformation getBlockState(final int blockStateId)
    {
        if (this.paletteEntries.isEmpty())
            return BlockInformation.AIR;

        if (blockStateId < 0 || blockStateId >= this.paletteEntries.size())
            return getBlockState(0);

        return this.paletteEntries.get(blockStateId).get();
    }

    public void sanitize(final Collection<IBlockInformation> toRemove) {
        final List<Entry> toRemoveList = toRemove.stream().map(this.paletteMap::get).toList();

        final Map<Entry, Integer> remainingPreRemoveIndexMap = this.paletteEntries.stream()
                .filter(entry -> !toRemoveList.contains(entry))
                .collect(Collectors.toMap(Function.identity(), this.paletteEntries::indexOf));

        this.paletteEntries.removeAll(toRemoveList);
        toRemove.forEach(this.paletteMap::remove);

        final Map<Entry, Integer> remainingPostRemoveIndexMap = remainingPreRemoveIndexMap.keySet()
                .stream()
                .collect(Collectors.toMap(Function.identity(), this.paletteEntries::indexOf));

        final Map<Integer, Integer> indexAlterationMap = remainingPreRemoveIndexMap.keySet()
                .stream()
                .filter(e -> !Objects.equals(remainingPreRemoveIndexMap.get(e), remainingPostRemoveIndexMap.get(e)))
                .collect(Collectors.toMap(remainingPreRemoveIndexMap::get, remainingPostRemoveIndexMap::get));

        this.onPaletteIndexChanged.accept(indexAlterationMap);

        this.onNewSizeAddedConsumer.accept(this.paletteEntries.size());
    }

    public void clear() {
        this.paletteEntries.clear();
        this.paletteMap.clear();
        this.getIndex(BlockInformation.AIR);
    }

    public List<IBlockInformation> getStates()
    {
        return this.paletteMap.keySet().stream().toList();
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof final SimpleStateEntryPalette that))
        {
            return false;
        }

        return paletteEntries.equals(that.paletteEntries);
    }

    @Override
    public int hashCode()
    {
        return paletteEntries.hashCode();
    }

    @Override
    public String toString()
    {
        return "SimpleStateEntryPalette{" +
                 "paletteEntries=" + paletteEntries +
                 '}';
    }

    private static final class Entry implements IPacketBufferSerializable, INBTSerializable<CompoundTag>
    {
        private IBlockInformation outwardFacingState;
        private CompoundTag rawSpec;

        private Entry(final IBlockInformation newState) {
            this.outwardFacingState = newState;
            this.rawSpec = newState.serializeNBT();
        }

        private Entry(final CompoundTag tag) {
            deserializeNBT(tag);
        }

        private Entry(final FriendlyByteBuf buffer) {
            deserializeFrom(buffer);
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return rawSpec.copy();
        }

        @Override
        public void deserializeNBT(final CompoundTag nbt)
        {
            this.rawSpec = nbt;
            this.outwardFacingState = new BlockInformation(nbt);
        }

        @Override
        public void serializeInto(final @NotNull FriendlyByteBuf packetBuffer)
        {
            this.outwardFacingState.serializeInto(packetBuffer);
        }

        @Override
        public void deserializeFrom(final @NotNull FriendlyByteBuf packetBuffer)
        {
            this.outwardFacingState = new BlockInformation(packetBuffer);
            this.rawSpec = this.outwardFacingState.serializeNBT();
        }

        public IBlockInformation get()
        {
            return outwardFacingState;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return Objects.equals(outwardFacingState, entry.outwardFacingState);
        }

        @Override
        public int hashCode() {
            return Objects.hash(outwardFacingState);
        }

    }
}
