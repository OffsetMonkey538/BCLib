package org.betterx.bclib.recipes;

import org.betterx.bclib.BCLib;
import org.betterx.bclib.interfaces.UnknownReceipBookCategory;
import org.betterx.bclib.util.ItemUtil;
import org.betterx.wover.recipe.api.BaseRecipeBuilder;
import org.betterx.wover.recipe.api.BaseUnlockableRecipeBuilder;
import org.betterx.wover.state.api.WorldState;
import org.betterx.wover.tag.api.TagManager;
import org.betterx.wover.tag.api.predefined.CommonItemTags;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class AnvilRecipe implements Recipe<AnvilRecipeInput>, UnknownReceipBookCategory {
    public final static String GROUP = "smithing";
    public final static RecipeType<AnvilRecipe> TYPE = BCLRecipeManager.registerType(BCLib.MOD_ID, GROUP);
    public final static Serializer SERIALIZER = BCLRecipeManager.registerSerializer(
            BCLib.MOD_ID,
            GROUP,
            new Serializer()
    );
    public final static ResourceLocation ID = BCLib.makeID(GROUP);


    public static void register() {
        //we call this to make sure that TYPE is initialized
    }

    private final Ingredient input;
    private final ItemStack output;
    private final int damage;
    private final TagKey<Item> allowedTools;
    private final int anvilLevel;
    private final int inputCount;

    public AnvilRecipe(
            Ingredient input,
            ItemStack output,
            int inputCount,
            TagKey<Item> allowedTools,
            int anvilLevel,
            int damage
    ) {
        this.input = input;
        this.output = output;
        this.allowedTools = allowedTools;
        this.anvilLevel = anvilLevel;
        this.inputCount = inputCount;
        this.damage = damage;
    }

    static Builder create(ResourceLocation id, ItemLike output) {
        return new BuilderImpl(id, output);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return this.output;
    }

    @Override
    public boolean matches(AnvilRecipeInput craftingInventory, Level level) {
        return this.matches(craftingInventory);
    }

    @Override
    public ItemStack assemble(AnvilRecipeInput recipeInput, HolderLookup.Provider provider) {
        return this.output.copy();
    }

    public static Iterable<Holder<Item>> getAllHammers() {
        Registry<Item> registry = WorldState.allStageRegistryAccess()
                                            .registryOrThrow(CommonItemTags.HAMMERS.registry());
        return registry.getTagOrEmpty(CommonItemTags.HAMMERS);
    }

    public static int getHammerSlot(Container c) {
        ItemStack h = c.getItem(0);
        if (!h.isEmpty() && h.is(CommonItemTags.HAMMERS)) return 0;

        //this is the default slot
        return 1;
    }

    public static int getIngredientSlot(Container c) {
        return Math.abs(getHammerSlot(c) - 1);
    }

    public ItemStack getHammer(AnvilRecipeInput c) {
        return c.hasHammer() ? c.getHammer() : null;
    }

    public ItemStack getIngredient(AnvilRecipeInput c) {
        return c.hasIngerdient() ? c.getIngredient() : null;
    }

    public ItemStack craft(AnvilRecipeInput craftingInventory, Player player) {
        if (!player.isCreative()) {
            if (!checkHammerDurability(craftingInventory, player)) return ItemStack.EMPTY;
            ItemStack hammer = getHammer(craftingInventory);
            if (hammer != null) {
                hammer.hurtAndBreak(this.damage, player, EquipmentSlot.OFFHAND);
                return ItemStack.EMPTY;
            }
        }
        return this.assemble(craftingInventory, Minecraft.getInstance().level.registryAccess());
    }

    public boolean checkHammerDurability(AnvilRecipeInput craftingInventory, Player player) {
        if (player.isCreative()) return true;
        ItemStack hammer = getHammer(craftingInventory);
        if (hammer != null) {
            int damage = hammer.getDamageValue() + this.damage;
            return damage < hammer.getMaxDamage();
        }
        return true;
    }

    public boolean matches(AnvilRecipeInput craftingInventory) {
        ItemStack hammer = getHammer(craftingInventory);
        if (hammer == null) {
            return false;
        }
        ItemStack material = getIngredient(craftingInventory);
        int materialCount = material == null ? 0 : material.getCount();

        return this.input.test(material) && materialCount >= this.inputCount && hammer.is(allowedTools);
    }

    public int getDamage() {
        return this.damage;
    }

    public int getInputCount() {
        return this.inputCount;
    }

    public TagKey<Item> getAllowedTools() {
        return this.allowedTools;
    }

    public Ingredient getMainIngredient() {
        return this.input;
    }

    public int getAnvilLevel() {
        return this.anvilLevel;
    }

    public boolean canUse(Item tool) {
        if (tool instanceof TieredItem ti) {
            return ti.builtInRegistryHolder().is(allowedTools);
        }
        return false;
    }

    public static boolean isHammer(Item tool) {
        if (tool == null) return false;
        return tool.getDefaultInstance().is(CommonItemTags.HAMMERS);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> defaultedList = NonNullList.create();
        defaultedList.add(Ingredient.of(BuiltInRegistries.ITEM.stream()
                                                              .filter(AnvilRecipe::isHammer)
                                                              .filter(this::canUse)
                                                              .map(ItemStack::new))
        );
        defaultedList.add(input);
        return defaultedList;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnvilRecipe that = (AnvilRecipe) o;
        return damage == that.damage &&
                ((allowedTools != null && allowedTools.equals(that.allowedTools)) || (allowedTools == null && that.allowedTools == null)) &&
                input.equals(that.input) &&
                output.equals(that.output);
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, output, damage, allowedTools);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("AnvilRecipe{");
        sb.append("input=").append(input);
        sb.append(", output=").append(output);
        sb.append(", damage=").append(damage);
        sb.append(", allowedTools=").append(allowedTools);
        sb.append(", anvilLevel=").append(anvilLevel);
        sb.append(", inputCount=").append(inputCount);
        sb.append('}');
        return sb.toString();
    }

    public interface Builder extends BaseRecipeBuilder<AnvilRecipe.Builder>, BaseUnlockableRecipeBuilder<AnvilRecipe.Builder> {
        Builder setInputCount(int ct);
        Builder setAllowedTools(TagKey<Item> items);

        Builder setAnvilLevel(int level);
        Builder setDamage(int damage);

        Builder setPrimaryInput(ItemLike... inputs);
        Builder setPrimaryInput(TagKey<Item> input);
        Builder setPrimaryInputAndUnlock(TagKey<Item> input);
        Builder setPrimaryInputAndUnlock(ItemLike... inputs);
    }

    public static class BuilderImpl extends BCLBaseRecipeBuilder<Builder, AnvilRecipe> implements Builder {
        private TagKey<Item> allowedTools;
        private int anvilLevel;
        private int damage;
        private int inputCount;

        protected BuilderImpl(ResourceLocation id, ItemLike output) {
            super(id, output, false);

            this.allowedTools = null;
            this.anvilLevel = 1;
            this.damage = 1;
            this.inputCount = 1;
        }

        public Builder setInputCount(int ct) {
            this.inputCount = ct;
            return this;
        }


        public Builder setAllowedTools(TagKey<Item> items) {
            this.allowedTools = items;
            return this;
        }

        public Builder setAnvilLevel(int level) {
            this.anvilLevel = level;
            return this;
        }

        public Builder setDamage(int damage) {
            this.damage = damage;
            return this;
        }

        @Override
        protected void validate() {
            super.validate();
            if (inputCount <= 0) {
                throwIllegalStateException(
                        "Number of input items for Recipe must be positive. Recipe {} will be ignored!"
                );
            }
        }

        @Override
        protected AnvilRecipe createRecipe(ResourceLocation id) {
            return new AnvilRecipe(primaryInput, output, inputCount, this.allowedTools, anvilLevel, damage);
        }
    }

    public static class Serializer implements RecipeSerializer<AnvilRecipe> {
        public static MapCodec<AnvilRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(recipe -> recipe.input),
                ItemUtil.CODEC_ITEM_STACK_WITH_NBT.fieldOf("result").forGetter(recipe -> recipe.output),
                Codec.INT.optionalFieldOf("inputCount", 1).forGetter(recipe -> recipe.inputCount),
                TagKey
                        .codec(Registries.ITEM)
                        .optionalFieldOf("allowedTools", null)
                        .forGetter(recipe -> recipe.allowedTools),
                Codec.INT.optionalFieldOf("anvilLevel", 1).forGetter(recipe -> recipe.anvilLevel),
                Codec.INT.optionalFieldOf("damage", 1).forGetter(recipe -> recipe.damage)
        ).apply(instance, AnvilRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, AnvilRecipe> STREAM_CODEC = StreamCodec.of(AnvilRecipe.Serializer::toNetwork, AnvilRecipe.Serializer::fromNetwork);
        public static final StreamCodec<RegistryFriendlyByteBuf, TagKey<Item>> ITEM_TAG_STREAM_CODEC = TagManager.streamCodec(Registries.ITEM);

        @Override
        public MapCodec<AnvilRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AnvilRecipe> streamCodec() {
            return STREAM_CODEC;
        }


        public static AnvilRecipe fromNetwork(RegistryFriendlyByteBuf packetBuffer) {
            Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(packetBuffer);
            ItemStack output = ItemStack.STREAM_CODEC.decode(packetBuffer);
            int inputCount = packetBuffer.readVarInt();
            TagKey<Item> allowedTools = ITEM_TAG_STREAM_CODEC.decode(packetBuffer);
            int anvilLevel = packetBuffer.readVarInt();
            int damage = packetBuffer.readVarInt();

            return new AnvilRecipe(input, output, inputCount, allowedTools, anvilLevel, damage);
        }

        public static void toNetwork(RegistryFriendlyByteBuf packetBuffer, AnvilRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(packetBuffer, recipe.input);
            ItemStack.STREAM_CODEC.encode(packetBuffer, recipe.output);
            packetBuffer.writeVarInt(recipe.inputCount);
            ITEM_TAG_STREAM_CODEC.encode(packetBuffer, recipe.allowedTools);
            packetBuffer.writeVarInt(recipe.anvilLevel);
            packetBuffer.writeVarInt(recipe.damage);
        }
    }
}
