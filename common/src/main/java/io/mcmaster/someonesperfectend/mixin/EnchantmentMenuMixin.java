package io.mcmaster.someonesperfectend.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.mcmaster.someonesperfectend.enchanting.EndChanter;
import io.mcmaster.someonesperfectend.enchanting.EndChanterFuelSlot;
import io.mcmaster.someonesperfectend.enchanting.EndChanterRules;
import io.mcmaster.someonesperfectend.enchanting.VoidCleansingRules;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Container;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The End-chanter (US-035): with a dragon head at a bookshelf position, the enchanting table also makes offers for
 * already-enchanted items (repair cost below 40). Offers come from the vanilla algorithm and are then filtered against
 * the item's enchantments; empty offers are removed and the clue shows the first remaining enchantment. Applying one
 * raises the repair cost like an anvil use.
 *
 * <p>Void Cleansing (US-036): while the End-chanter is active, the lapis slot also accepts dragon's breath. With dragon's
 * breath in it, vanilla offers are replaced by a single Void Cleansing offer in slot 3 (cost 30, clue
 * {@link VoidCleansingRules#CLUE}) for items with a repair cost above 0 at a table with full power; applying it
 * consumes 1 dragon's breath and 3 levels and lowers the repair cost to {@code (cost - 1) / 2}.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Shadow @Final private Container enchantSlots;
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final public int[] costs;
    @Shadow @Final public int[] enchantClue;
    @Shadow @Final public int[] levelClue;
    @Shadow @Final private DataSlot enchantmentSeed;

    /** The repair cost of the item being re-enchanted by the current click, or -1. */
    @Unique private int someonesperfectend$reEnchantRepairCost = -1;
    @Unique private ItemEnchantments someonesperfectend$enchantmentsBefore;

    @Shadow
    private List<EnchantmentInstance> getEnchantmentList(RegistryAccess registries, ItemStack stack, int slot,
            int cost) {
        throw new AssertionError();
    }

    /** Replaces the lapis slot with one that also takes dragon's breath while the End-chanter is active. */
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;"
            + "Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void someonesperfectend$voidCleansingFuelSlot(int containerId, Inventory inventory,
            ContainerLevelAccess access, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        Slot lapis = menu.slots.get(1);
        menu.slots.set(1, new EndChanterFuelSlot(lapis, () -> this.access.evaluate(EndChanter::isActive, true)));
    }

    /**
     * With dragon's breath in the lapis slot, vanilla offers are never made: the server shows only the Void Cleansing
     * offer (if any), and the client keeps the offers the server syncs.
     */
    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void someonesperfectend$voidCleansingOffer(Container container, CallbackInfo ci) {
        if (container != this.enchantSlots || !EndChanter.isVoidCleansingFuel(container.getItem(1))) {
            return;
        }
        ci.cancel();
        ItemStack stack = container.getItem(0);
        this.access.execute((level, pos) -> {
            boolean offered = !stack.isEmpty() && EndChanter.isActive(level, pos)
                    && VoidCleansingRules.offered(EndChanter.repairCost(stack), EndChanter.bookshelfPower(level, pos));
            for (int slot = 0; slot < this.costs.length; slot++) {
                boolean cleansing = offered && slot == VoidCleansingRules.OFFER_SLOT;
                this.costs[slot] = cleansing ? VoidCleansingRules.LEVEL_REQUIREMENT : 0;
                this.enchantClue[slot] = cleansing ? VoidCleansingRules.CLUE : -1;
                this.levelClue[slot] = -1;
            }
            ((AbstractContainerMenu) (Object) this).broadcastChanges();
        });
    }

    /** Applies Void Cleansing; with dragon's breath in the lapis slot no vanilla offer can be applied. */
    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void someonesperfectend$applyVoidCleansing(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        ItemStack fuel = this.enchantSlots.getItem(1);
        if (!EndChanter.isVoidCleansingFuel(fuel)) {
            return;
        }
        ItemStack stack = this.enchantSlots.getItem(0);
        int repairCost = EndChanter.repairCost(stack);
        if (id != VoidCleansingRules.OFFER_SLOT || this.costs[id] <= 0 || this.enchantClue[id] != VoidCleansingRules.CLUE
                || repairCost <= 0 || fuel.getCount() < VoidCleansingRules.BREATH_COST
                || !VoidCleansingRules.canApply(player.experienceLevel, player.getAbilities().instabuild)) {
            cir.setReturnValue(false);
            return;
        }
        this.access.execute((level, pos) -> {
            player.onEnchantmentPerformed(stack, VoidCleansingRules.LEVEL_COST);
            stack.set(DataComponents.REPAIR_COST, VoidCleansingRules.cleansedRepairCost(repairCost));
            fuel.consume(VoidCleansingRules.BREATH_COST, player);
            if (fuel.isEmpty()) {
                this.enchantSlots.setItem(1, ItemStack.EMPTY);
            }
            this.enchantSlots.setChanged();
            this.enchantmentSeed.set(player.getEnchantmentSeed());
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F,
                    level.random.nextFloat() * 0.1F + 0.9F);
        });
        cir.setReturnValue(true);
    }

    /** Shift-clicking dragon's breath moves it into the lapis slot (vanilla would put it into the item slot). */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void someonesperfectend$quickMoveBreath(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        Slot slot = menu.slots.get(index);
        Slot fuelSlot = menu.slots.get(1);
        ItemStack stack = slot.getItem();
        if (index < 2 || !EndChanter.isVoidCleansingFuel(stack) || !fuelSlot.mayPlace(stack)) {
            return;
        }
        ItemStack before = stack.copy();
        ItemStack inSlot = fuelSlot.getItem();
        if (inSlot.isEmpty()) {
            fuelSlot.setByPlayer(stack.split(Math.min(stack.getCount(), fuelSlot.getMaxStackSize(stack))));
        } else if (ItemStack.isSameItemSameComponents(inSlot, stack)) {
            int moved = Math.min(stack.getCount(), fuelSlot.getMaxStackSize(stack) - inSlot.getCount());
            if (moved > 0) {
                inSlot.grow(moved);
                stack.shrink(moved);
                fuelSlot.setChanged();
            }
        }
        if (stack.getCount() == before.getCount()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        cir.setReturnValue(before);
    }

    /**
     * Lets an active End-chanter make offers for enchanted items. On the client ({@code NULL} access) accepted items
     * count as enchantable so the client keeps the offers the server syncs instead of clearing them.
     */
    @WrapOperation(method = "slotsChanged", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isEnchantable()Z"))
    private boolean someonesperfectend$acceptEnchanted(ItemStack stack, Operation<Boolean> original) {
        return original.call(stack) || EndChanter.accepts(stack) && this.access.evaluate(EndChanter::isActive, true);
    }

    @ModifyReturnValue(method = "getEnchantmentList", at = @At("RETURN"))
    private List<EnchantmentInstance> someonesperfectend$filterOffer(List<EnchantmentInstance> offered,
            RegistryAccess registries, ItemStack stack, int slot, int cost) {
        return EndChanter.isReEnchantable(stack) ? EndChanter.filterOffer(stack, offered) : offered;
    }

    /** Removes offers left empty by the filter and points each clue at the first remaining enchantment. */
    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void someonesperfectend$updateClues(Container container, CallbackInfo ci) {
        ItemStack stack = container.getItem(0);
        if (container != this.enchantSlots || !EndChanter.isReEnchantable(stack)) {
            return;
        }
        this.access.execute((level, pos) -> {
            IdMap<Holder<Enchantment>> ids = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .asHolderIdMap();
            for (int slot = 0; slot < this.costs.length; slot++) {
                if (this.costs[slot] <= 0) {
                    continue;
                }
                List<EnchantmentInstance> offer = this.getEnchantmentList(level.registryAccess(), stack, slot,
                        this.costs[slot]);
                if (offer.isEmpty()) {
                    this.costs[slot] = 0;
                    this.enchantClue[slot] = -1;
                    this.levelClue[slot] = -1;
                } else {
                    this.enchantClue[slot] = ids.getId(offer.get(0).enchantment);
                    this.levelClue[slot] = offer.get(0).level;
                }
            }
            ((AbstractContainerMenu) (Object) this).broadcastChanges();
        });
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"))
    private void someonesperfectend$rememberItem(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = this.enchantSlots.getItem(0);
        boolean reEnchant = EndChanter.isReEnchantable(stack);
        this.someonesperfectend$reEnchantRepairCost = reEnchant ? EndChanter.repairCost(stack) : -1;
        this.someonesperfectend$enchantmentsBefore = reEnchant ? stack.getEnchantments() : null;
    }

    /** After a re-enchant, raises the repair cost and refreshes the offers for the new cost. */
    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void someonesperfectend$raiseRepairCost(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        int repairCost = this.someonesperfectend$reEnchantRepairCost;
        ItemEnchantments before = this.someonesperfectend$enchantmentsBefore;
        this.someonesperfectend$reEnchantRepairCost = -1;
        this.someonesperfectend$enchantmentsBefore = null;
        ItemStack stack = this.enchantSlots.getItem(0);
        if (repairCost < 0 || stack.isEmpty() || stack.getEnchantments().equals(before)) {
            return;
        }
        stack.set(DataComponents.REPAIR_COST, EndChanterRules.nextRepairCost(repairCost));
        this.enchantSlots.setChanged();
    }
}
