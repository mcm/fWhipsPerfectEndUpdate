package io.mcmaster.fwhipsperfectendupdate.trinket;

import com.mojang.datafixers.util.Pair;
import io.mcmaster.fwhipsperfectendupdate.FwhipsPerfectEndUpdate;
import io.wispforest.accessories.api.AccessoriesAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The trinket slot added to the vanilla player {@link InventoryMenu} (see {@code InventoryMenuMixin}), directly
 * above the off-hand slot. It stores nothing itself: it reads and writes the Accessories container, so other
 * mods using Accessories see the equipped item.
 */
public class TrinketSlot extends Slot {
    /** Position in the survival inventory screen: directly above the off-hand slot (77, 62). */
    public static final int X = 77;

    public static final int Y = 44;
    /** Empty-slot icon (a gem outline) in the block atlas. */
    public static final ResourceLocation EMPTY_ICON = FwhipsPerfectEndUpdate.id("item/empty_trinket_slot");

    private final Player player;
    private final TrinketContainer trinkets;

    public TrinketSlot(Player player, int x, int y) {
        this(player, new TrinketContainer(player), x, y);
    }

    private TrinketSlot(Player player, TrinketContainer container, int x, int y) {
        super(container, 0, x, y);
        this.player = player;
        this.trinkets = container;
    }

    /** Index of the trinket slot in {@code menu}, or -1 if it has none. */
    public static int indexIn(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot instanceof TrinketSlot) {
                return slot.index;
            }
        }
        return -1;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.trinkets.isAvailable() && Trinkets.canEquip(this.player, stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        ItemStack stack = getItem();
        return stack.isEmpty() || AccessoriesAPI.canUnequip(stack, Trinkets.reference(this.player));
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Nullable
    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_ICON);
    }
}
