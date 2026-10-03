package io.mcmaster.someonesperfectend.enchanting;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The End-chanter (US-035): an enchanting table with a dragon head at a bookshelf position can re-enchant already
 * enchanted items. Hooked into {@code EnchantmentMenu} by {@code EnchantmentMenuMixin}.
 */
public final class EndChanter {
    private EndChanter() {}

    /**
     * Whether a dragon head sits at any bookshelf position of the table at {@code table}, with the block in between in
     * {@code #minecraft:enchantment_power_transmitter} (the same rule as {@link EnchantingTableBlock#isValidBookShelf}).
     */
    public static boolean isActive(Level level, BlockPos table) {
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            BlockState state = level.getBlockState(table.offset(offset));
            if ((state.is(Blocks.DRAGON_HEAD) || state.is(Blocks.DRAGON_WALL_HEAD))
                    && level.getBlockState(table.offset(offset.getX() / 2, offset.getY(), offset.getZ() / 2))
                            .is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The table's enchanting power as the number of valid bookshelves around it (vanilla's rule; on NeoForge,
     * blocks with any enchanting power bonus).
     */
    public static int bookshelfPower(Level level, BlockPos table) {
        int power = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, table, offset)) {
                power++;
            }
        }
        return power;
    }

    /** Whether the lapis slot holds Void Cleansing fuel (dragon's breath, US-036). */
    public static boolean isVoidCleansingFuel(ItemStack stack) {
        return stack.is(Items.DRAGON_BREATH);
    }

    /** Whether the stack is an already-enchanted item (not a book) that the End-chanter would re-enchant. */
    public static boolean isReEnchantable(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(Items.BOOK) && !stack.is(Items.ENCHANTED_BOOK) && stack.isEnchanted();
    }

    /** Whether an active End-chanter accepts the stack: enchanted, enchantable, and repair cost below 40. */
    public static boolean accepts(ItemStack stack) {
        return isReEnchantable(stack)
                && stack.getItem().isEnchantable(stack)
                && EndChanterRules.acceptsRepairCost(repairCost(stack));
    }

    public static int repairCost(ItemStack stack) {
        return stack.getOrDefault(DataComponents.REPAIR_COST, 0);
    }

    /** Filters a vanilla offer against the stack's enchantments ({@link EndChanterRules#filterOffer}). */
    public static List<EnchantmentInstance> filterOffer(ItemStack stack, List<EnchantmentInstance> offered) {
        Map<Holder<Enchantment>, Integer> existing = new HashMap<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry :
                stack.getEnchantments().entrySet()) {
            existing.put(entry.getKey(), entry.getIntValue());
        }
        return EndChanterRules.filterOffer(
                offered, offer -> offer.enchantment, offer -> offer.level, existing, Enchantment::areCompatible);
    }
}
