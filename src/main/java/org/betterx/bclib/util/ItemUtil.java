package org.betterx.bclib.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ItemUtil {
    public static <T> Codec<T> codecItemStackWithNBT(
            Function<T, ItemStack> getter,
            Function<ItemStack, T> factory
    ) {
        return RecordCodecBuilder.create((instance) -> instance.group(
                BuiltInRegistries.ITEM.holderByNameCodec()
                                      .fieldOf("item")
                                      .forGetter(o -> getter.apply(o).getItemHolder()),
                Codec.INT.optionalFieldOf("count", 1)
                         .forGetter(o -> getter.apply(o).getCount()),
                ExtraCodecs.strictOptionalField(TagParser.AS_CODEC, "nbt")
                           .forGetter(o -> Optional.ofNullable(getter.apply(o).getTag()))
        ).apply(instance, (item, count, nbt) -> factory.apply(new ItemStack(item, count, nbt))));
    }

    public static Codec<ItemStack> CODEC_ITEM_STACK_WITH_NBT = codecItemStackWithNBT(
            Function.identity(),
            Function.identity()
    );

    public static Codec<Ingredient.ItemValue> CODEC_NBT_ITEM_VALUE = codecItemStackWithNBT(
            (itemValue) -> itemValue.item(),
            (stack) -> new Ingredient.ItemValue(stack)
    );

    private static final Codec<Ingredient.Value> VALUE_CODEC = ExtraCodecs
            .xor(CODEC_NBT_ITEM_VALUE, Ingredient.TagValue.CODEC)
            .xmap(
                    (either) -> either.map((itemValue) -> itemValue, (tagValue) -> tagValue),
                    (value) -> {
                        if (value instanceof Ingredient.TagValue tagValue) {
                            return Either.right(tagValue);
                        } else if (value instanceof Ingredient.ItemValue itemValue) {
                            return Either.left(itemValue);
                        } else {
                            throw new UnsupportedOperationException(
                                    "This is neither an nbt-item value nor a tag value.");
                        }
                    }
            );

    private static Codec<Ingredient> ingredientCodec(boolean allowEmpty) {
        Codec<Ingredient.Value[]> LIST_CODEC = Codec.list(VALUE_CODEC).comapFlatMap((list) ->
                        !allowEmpty && list.isEmpty()
                                ? DataResult.error(() -> "Item array cannot be empty, at least one item must be defined")
                                : DataResult.success(list.toArray(new Ingredient.Value[0]))
                , List::of);

        return ExtraCodecs.either(LIST_CODEC, VALUE_CODEC).flatComapMap(
                (either) -> either.map(Ingredient::new, (value) -> new Ingredient(new Ingredient.Value[]{value})),
                (ingredient) -> {
                    if (ingredient.values.length == 1) {
                        return DataResult.success(Either.right(ingredient.values[0]));
                    } else {
                        return ingredient.values.length == 0 && !allowEmpty
                                ? DataResult.error(() -> "Item array cannot be empty, at least one item must be defined")
                                : DataResult.success(Either.left(ingredient.values));
                    }
                }
        );
    }

    public static Codec<Ingredient> CODEC_INGREDIENT_WITH_NBT = ingredientCodec(true);
    public static Codec<Ingredient> CODEC_INGREDIENT_WITH_NBT_NOT_EMPTY = ingredientCodec(false);
}
