package io.mcmaster.someonesperfectend.infuser;

import io.mcmaster.someonesperfectend.block.ModBlocks;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The beacon menu plus one infusion slot. Slot indices: 0 payment (the beacon's), 1-36 player inventory,
 * {@link #INFUSION_SLOT} infusion. The infusion slot takes items only in the End.
 */
public class InfuserMenu extends BeaconMenu {
    public static final int PAYMENT_SLOT = 0;
    public static final int INV_SLOT_START = 1;
    public static final int USE_ROW_SLOT_END = 37;
    public static final int INFUSION_SLOT = 37;
    /** Infusion slot position: inside the secondary-power panel, below its buttons. */
    public static final int INFUSION_SLOT_X = 161;
    public static final int INFUSION_SLOT_Y = 76;

    private final ContainerLevelAccess access;
    private final boolean infusionEnabled;
    private final Container infusion = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            InfuserMenu.this.slotsChanged(this);
        }
    };
    private final Slot infusionSlot;

    /** Client-side constructor (from {@link MenuType}). */
    public InfuserMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainerData(3), ContainerLevelAccess.NULL);
    }

    public InfuserMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(containerId, inventory, data, access);
        this.access = access;
        // The client has no level access, but its player is always in the Infuser's level.
        Level level = access.evaluate((accessLevel, pos) -> accessLevel).orElse(inventory.player.level());
        this.infusionEnabled = isInfusionEnabled(level);
        this.infusionSlot = addSlot(new InfusionSlot(infusion, INFUSION_SLOT_X, INFUSION_SLOT_Y));
    }

    /** Infusion only works in the End. */
    public static boolean isInfusionEnabled(@Nullable Level level) {
        return level != null && level.dimension() == Level.END;
    }

    public boolean isInfusionEnabled() {
        return infusionEnabled;
    }

    public Slot getInfusionSlot() {
        return infusionSlot;
    }

    public boolean hasInfusionItem() {
        return infusionSlot.hasItem();
    }

    @Override
    public MenuType<?> getType() {
        return Infusers.MENU.get();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.INFUSER.get());
    }

    /** Effects the infusion item may take, ignoring tier (empty without an item). */
    public List<InfusionEffect> infusionEffects() {
        return InfusionRules.allowedEffects(Infusion.kindsOf(infusionSlot.getItem()));
    }

    /** Whether the screen offers this effect: valid for the item, the current tier, and any existing infusion. */
    public boolean canSelectInfusion(InfusionEffect effect) {
        ItemStack stack = infusionSlot.getItem();
        return infusionEnabled && !stack.isEmpty() && InfusionRules.canSelect(Infusion.kindsOf(stack),
                Infusion.currentOf(stack), effect, getLevels());
    }

    /** What confirming would write to the infusion item, ignoring the payment; empty if it can't be charged. */
    public Optional<InfusionRules.Charge> previewInfusion(InfusionEffect effect) {
        ItemStack stack = infusionSlot.getItem();
        if (!infusionEnabled || stack.isEmpty()) {
            return Optional.empty();
        }
        return InfusionRules.charge(Infusion.kindsOf(stack), Infusion.currentOf(stack), effect, getLevels());
    }

    /** Whether confirming with this effect would infuse: a payment item is present and the item can be charged. */
    public boolean canInfuse(@Nullable InfusionEffect effect) {
        return effect != null && super.hasPayment() && previewInfusion(effect).isPresent();
    }

    /**
     * Consumes one payment item and charges the infusion item with the effect. Never touches the beacon powers.
     *
     * @return whether the item was infused
     */
    public boolean infuse(@Nullable InfusionEffect effect) {
        if (!canInfuse(effect)) {
            return false;
        }
        InfusionRules.Charge charge = previewInfusion(effect).orElseThrow();
        infusionSlot.getItem().set(Infusers.INFUSION.get(), Infusion.of(charge));
        slots.get(PAYMENT_SLOT).remove(1);
        infusionSlot.setChanged();
        return true;
    }

    /**
     * With the infusion slot empty, confirm sets the beacon powers exactly like a beacon. With an item in it,
     * confirm infuses the item with the primary effect instead (the screen sends the chosen infusion effect as the
     * beacon packet's primary effect).
     */
    @Override
    public void updateEffects(Optional<Holder<MobEffect>> primary, Optional<Holder<MobEffect>> secondary) {
        if (hasInfusionItem()) {
            infuse(InfusionEffect.of(primary.orElse(null)));
            return;
        }
        super.updateEffects(primary, secondary);
    }

    /** Enables the beacon's own confirm button, which only sets beacon powers; see {@link #updateEffects}. */
    @Override
    public boolean hasPayment() {
        return super.hasPayment() && !hasInfusionItem();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            clearContainer(player, infusion);
        }
    }

    /** The beacon's shift-click rules, plus moving infusable items into the infusion slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int invEnd = INV_SLOT_START + 27;
        if (index == PAYMENT_SLOT || index == INFUSION_SLOT) {
            if (!moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (!infusionSlot.hasItem() && infusionSlot.mayPlace(stack)) {
            if (!moveItemStackTo(stack, INFUSION_SLOT, INFUSION_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!slots.get(PAYMENT_SLOT).hasItem() && slots.get(PAYMENT_SLOT).mayPlace(stack)
                && stack.getCount() == 1) {
            if (!moveItemStackTo(stack, PAYMENT_SLOT, PAYMENT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < invEnd) {
            if (!moveItemStackTo(stack, invEnd, USE_ROW_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INV_SLOT_START, invEnd, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    private class InfusionSlot extends Slot {
        InfusionSlot(Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return infusionEnabled && stack.is(Infusers.INFUSABLE);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
