package mod.chiselsandbits.forge.data.tag;

import com.communi.suggestu.scena.core.registries.deferred.IRegistryObject;
import mod.chiselsandbits.api.util.constants.Constants;
import mod.chiselsandbits.registrars.ModBlocks;
import mod.chiselsandbits.registrars.ModTags;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class ModBlockTagGenerator extends BlockTagsProvider
{
    public ModBlockTagGenerator(
      final DataGenerator dataGenerator,
      @Nullable final ExistingFileHelper existingFileHelper)
    {
        super(dataGenerator, Constants.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags()
    {
        this.tag(ModTags.Blocks.BLOCKED_CHISELABLE);
        this.tag(ModTags.Blocks.FORCED_CHISELABLE)
          .addTag(BlockTags.LEAVES)
          .add(Blocks.GRASS_BLOCK)
          .add(Blocks.ICE)
          .add(Blocks.PACKED_ICE)
          .add(Blocks.PACKED_ICE);
        this.tag(ModTags.Blocks.CHISELED_BLOCK).add(
          ModBlocks.MATERIAL_TO_BLOCK_CONVERSIONS.values()
            .stream()
            .map(IRegistryObject::get)
            .toArray(Block[]::new)
        );

        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.CHISELED_PRINTER.get());
        this.tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.MODIFICATION_TABLE.get())
                .add(ModBlocks.BIT_STORAGE.get())
                .add(ModBlocks.PATTERN_SCANNER.get());

    }
}
