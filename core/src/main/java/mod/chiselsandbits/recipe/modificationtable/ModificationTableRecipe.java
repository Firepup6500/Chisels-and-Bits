package mod.chiselsandbits.recipe.modificationtable;

import mod.chiselsandbits.api.item.multistate.IMultiStateItem;
import mod.chiselsandbits.api.item.multistate.IMultiStateItemStack;
import mod.chiselsandbits.api.item.pattern.IMultiUsePatternItem;
import mod.chiselsandbits.api.item.pattern.IPatternItem;
import mod.chiselsandbits.api.modification.operation.IModificationOperation;
import mod.chiselsandbits.api.multistate.snapshot.IMultiStateSnapshot;
import mod.chiselsandbits.multistate.snapshot.EmptySnapshot;
import mod.chiselsandbits.registrars.ModRecipeSerializers;
import mod.chiselsandbits.registrars.ModRecipeTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ModificationTableRecipe implements Recipe<Container>
{
    private final IModificationOperation operation;

    public ModificationTableRecipe(final IModificationOperation operation) {this.operation = operation;}

    public IModificationOperation getOperation()
    {
        return operation;
    }

    @Override
    public boolean matches(final Container inv, final @NotNull Level worldIn)
    {
        return inv.getItem(0).getItem() instanceof IPatternItem && !(inv.getItem(0).getItem() instanceof IMultiUsePatternItem);
    }

    @Override
    public @NotNull ItemStack assemble(final @NotNull Container inv, final @NotNull RegistryAccess access)
    {
        return getAppliedSnapshot(inv).toItemStack().toPatternStack();
    }

    public @NotNull ItemStack getCraftingBlockResult(final Container inv)
    {
        return getAppliedSnapshot(inv).toItemStack().toBlockStack();
    }

    public @NotNull IMultiStateSnapshot getAppliedSnapshot(final Container inv)
    {
        final ItemStack multiStateStack = inv.getItem(0);
        if (multiStateStack.isEmpty())
            return EmptySnapshot.INSTANCE;

        if (!(multiStateStack.getItem() instanceof final IMultiStateItem item))
            return EmptySnapshot.INSTANCE;

        final IMultiStateItemStack multiStateItemStack = item.createItemStack(multiStateStack);
        final IMultiStateSnapshot snapshot = multiStateItemStack.createSnapshot().clone();

        getOperation().apply(snapshot);

        return snapshot;
    }

    public Component getDisplayName() {
        return Component.translatable(
          Objects.requireNonNull(this.getOperation().getRegistryName()).getNamespace() + ".recipes.chisel.pattern.modification." + this.getOperation().getRegistryName().getPath());
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height)
    {
        return width * height > 0;
    }

    @Override
    public @NotNull ItemStack getResultItem(final @NotNull RegistryAccess registryAccess)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer()
    {
        return ModRecipeSerializers.MODIFICATION_TABLE.get();
    }

    @Override
    public @NotNull RecipeType<?> getType()
    {
        return ModRecipeTypes.MODIFICATION_TABLE.get();
    }
}
