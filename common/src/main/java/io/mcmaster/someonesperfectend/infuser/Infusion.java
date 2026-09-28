package io.mcmaster.someonesperfectend.infuser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.mcmaster.someonesperfectend.SomeonesPerfectEnd;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code someonesperfectend:infusion} data component: a beacon effect infused into gear at an Infuser, its
 * amplifier (fixed at the first infusion), and the stored time left.
 */
public record Infusion(Holder<MobEffect> effect, int amplifier, int remainingTicks) {
    public static final Codec<Infusion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MobEffect.CODEC.fieldOf("effect").forGetter(Infusion::effect),
            Codec.intRange(0, 1).fieldOf("amplifier").forGetter(Infusion::amplifier),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("remaining_ticks").forGetter(Infusion::remainingTicks)
    ).apply(instance, Infusion::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Infusion> STREAM_CODEC = StreamCodec.composite(
            MobEffect.STREAM_CODEC, Infusion::effect,
            ByteBufCodecs.VAR_INT, Infusion::amplifier,
            ByteBufCodecs.VAR_INT, Infusion::remainingTicks,
            Infusion::new);

    /** "Infused: %s — %s remaining" */
    public static final String TOOLTIP_KEY = "tooltip." + SomeonesPerfectEnd.MOD_ID + ".infusion";

    public static Infusion of(InfusionRules.Charge charge) {
        return new Infusion(charge.effect().holder(), charge.amplifier(), charge.remainingTicks());
    }

    public InfusionRules.Current toCurrent() {
        return new InfusionRules.Current(InfusionEffect.of(effect), amplifier, remainingTicks);
    }

    /** For example "Infused: Haste II — 12:34 remaining". */
    public Component tooltip() {
        Component level = Component.translatable("enchantment.level." + (amplifier + 1));
        Component name = Component.translatable("potion.withAmplifier",
                Component.translatable(effect.value().getDescriptionId()), level);
        return Component.translatable(TOOLTIP_KEY, name, InfusionRules.formatTime(remainingTicks));
    }

    @Nullable
    public static Infusion get(ItemStack stack) {
        return stack.get(Infusers.INFUSION.get());
    }

    @Nullable
    public static InfusionRules.Current currentOf(ItemStack stack) {
        Infusion infusion = get(stack);
        return infusion == null ? null : infusion.toCurrent();
    }

    /** The infusion slots an item counts as, from its item tags. */
    public static Set<InfusionRules.Kind> kindsOf(ItemStack stack) {
        Set<InfusionRules.Kind> kinds = EnumSet.noneOf(InfusionRules.Kind.class);
        if (stack.is(ItemTags.FOOT_ARMOR)) {
            kinds.add(InfusionRules.Kind.FEET);
        }
        if (stack.is(ItemTags.CHEST_ARMOR)) {
            kinds.add(InfusionRules.Kind.CHEST);
        }
        if (stack.is(ItemTags.MINING_ENCHANTABLE)) {
            kinds.add(InfusionRules.Kind.MINING);
        }
        if (stack.is(ItemTags.WEAPON_ENCHANTABLE)) {
            kinds.add(InfusionRules.Kind.WEAPON);
        }
        return kinds;
    }
}
