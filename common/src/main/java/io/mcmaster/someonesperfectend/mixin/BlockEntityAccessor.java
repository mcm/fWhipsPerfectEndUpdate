package io.mcmaster.someonesperfectend.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets subclasses of vanilla block entities with a hardcoded type (e.g. the Infuser) use their own type. */
@Mixin(BlockEntity.class)
public interface BlockEntityAccessor {
    @Mutable
    @Accessor("type")
    void someonesperfectend$setType(BlockEntityType<?> type);
}
