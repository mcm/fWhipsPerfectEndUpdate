package io.mcmaster.someonesperfectend.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.mcmaster.someonesperfectend.enchanting.EndChanter;
import io.mcmaster.someonesperfectend.enchanting.EndChanterRules;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
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
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Shadow @Final private Container enchantSlots;
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final public int[] costs;
    @Shadow @Final public int[] enchantClue;
    @Shadow @Final public int[] levelClue;

    /** The repair cost of the item being re-enchanted by the current click, or -1. */
    @Unique private int someonesperfectend$reEnchantRepairCost = -1;
    @Unique private ItemEnchantments someonesperfectend$enchantmentsBefore;

    @Shadow
    private List<EnchantmentInstance> getEnchantmentList(RegistryAccess registries, ItemStack stack, int slot,
            int cost) {
        throw new AssertionError();
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
