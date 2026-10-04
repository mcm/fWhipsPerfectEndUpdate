package io.mcmaster.fwhipsperfectendupdate.infuser;

import dev.architectury.event.events.common.TickEvent;
import io.mcmaster.fwhipsperfectendupdate.totem.EffectTotems;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Applies infused gear's effects (US-031). Every {@link InfusionRules#GEAR_INTERVAL} ticks on the server, infused
 * items in their valid positions (boots on the feet, chestplates on the chest, tools and weapons in either hand)
 * apply their effect and use up stored time, unless paused: the main hand wins over the off hand for the same
 * effect, and a live tracked beacon effect at the same or a higher level takes precedence. While the player wears an
 * active effect totem, all infused gear is paused (US-033).
 */
public final class InfusedGear {
    private InfusedGear() {}

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (!player.level().isClientSide() && player.tickCount % InfusionRules.GEAR_INTERVAL == 0) {
                update(player);
            }
        });
    }

    /** One update cycle for the player's gear. */
    public static void update(Player player) {
        if (EffectTotems.hasActiveTotemEquipped(player)) {
            return;
        }
        Set<Holder<MobEffect>> claimed = new HashSet<>();
        update(player, player.getItemBySlot(EquipmentSlot.FEET), claimed, ItemTags.FOOT_ARMOR);
        update(player, player.getItemBySlot(EquipmentSlot.CHEST), claimed, ItemTags.CHEST_ARMOR);
        update(player, player.getMainHandItem(), claimed, ItemTags.MINING_ENCHANTABLE, ItemTags.WEAPON_ENCHANTABLE);
        update(player, player.getOffhandItem(), claimed, ItemTags.MINING_ENCHANTABLE, ItemTags.WEAPON_ENCHANTABLE);
    }

    @SafeVarargs
    private static void update(
            Player player, ItemStack stack, Set<Holder<MobEffect>> claimed, TagKey<Item>... validTags) {
        Infusion infusion = Infusion.get(stack);
        if (infusion == null || !isAny(stack, validTags) || !claimed.add(infusion.effect())) {
            return;
        }
        int beacon = BeaconEffectTracker.highestAmplifier(player, infusion.effect());
        if (InfusionRules.pausedByBeacon(infusion.amplifier(), beacon)) {
            return;
        }
        player.addEffect(new MobEffectInstance(
                infusion.effect(), InfusionRules.GEAR_EFFECT_TICKS, infusion.amplifier(), true, false, true));
        int remaining = InfusionRules.deplete(infusion.remainingTicks());
        if (remaining > 0) {
            stack.set(Infusers.INFUSION.get(), new Infusion(infusion.effect(), infusion.amplifier(), remaining));
        } else {
            stack.remove(Infusers.INFUSION.get());
        }
    }

    @SafeVarargs
    private static boolean isAny(ItemStack stack, TagKey<Item>... tags) {
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }
}
