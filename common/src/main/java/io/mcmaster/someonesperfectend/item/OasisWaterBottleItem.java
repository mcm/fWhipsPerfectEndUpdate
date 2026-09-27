package io.mcmaster.someonesperfectend.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;

/**
 * Bottle of Oasis Water. Its {@code potion_contents} is the water potion, so it drinks and turns dirt into
 * mud like a water bottle; brewing it gives the longer Oasis potions (see {@code OasisPotions}).
 */
public class OasisWaterBottleItem extends PotionItem {
    public OasisWaterBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        return new ItemStack(this);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return getDescriptionId();
    }
}
