package org.betterx.bclib.complexmaterials.set.wood;

import org.betterx.bclib.complexmaterials.ComplexMaterial;
import org.betterx.bclib.complexmaterials.WoodenComplexMaterial;
import org.betterx.bclib.complexmaterials.entry.BlockEntry;
import org.betterx.bclib.complexmaterials.entry.SimpleMaterialSlot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractSaplingSlot extends SimpleMaterialSlot<WoodenComplexMaterial> {
    protected static final String SAPLING_SUFFIX = "sapling";

    protected AbstractSaplingSlot() {
        super(SAPLING_SUFFIX);
    }

    @Override
    protected void modifyBlockEntry(WoodenComplexMaterial parentMaterial, @NotNull BlockEntry entry) {
        entry
                .setBlockTags(BlockTags.SAPLINGS)
                .setItemTags(ItemTags.SAPLINGS);
    }

    @Override
    protected @Nullable void makeRecipe(ComplexMaterial parentMaterial, ResourceLocation id) {

    }

    public static AbstractSaplingSlot create(BiFunction<ComplexMaterial, BlockBehaviour.Properties, Block> maker) {
        return new AbstractSaplingSlot() {
            @Override
            protected @NotNull Block createBlock(
                    WoodenComplexMaterial parentMaterial,
                    BlockBehaviour.Properties settings
            ) {
                return maker.apply(parentMaterial, settings);
            }
        };
    }

    public static AbstractSaplingSlot create(Supplier<Block> maker) {
        return new AbstractSaplingSlot() {
            @Override
            protected @NotNull Block createBlock(
                    WoodenComplexMaterial parentMaterial,
                    BlockBehaviour.Properties settings
            ) {
                return maker.get();
            }
        };
    }
}
