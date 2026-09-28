package io.mcmaster.someonesperfectend.trinket;

import io.wispforest.accessories.api.AccessoriesContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A one-slot {@link Container} view of the player's Accessories trinket container. The Accessories container is
 * looked up on every call: it doesn't exist yet when the player's {@code InventoryMenu} is built, and Accessories
 * rebuilds it when slots are reloaded or synced. Until it exists the view is empty and ignores writes (the slot
 * refuses items in that state, so nothing is lost).
 */
public final class TrinketContainer implements Container {
    private final Player player;

    public TrinketContainer(Player player) {
        this.player = player;
    }

    @Nullable
    private Container backing() {
        AccessoriesContainer container = Trinkets.container(this.player);
        return container == null ? null : container.getAccessories();
    }

    public boolean isAvailable() {
        return backing() != null;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getItem(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        Container backing = backing();
        return backing == null ? ItemStack.EMPTY : backing.getItem(0);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        Container backing = backing();
        return backing == null ? ItemStack.EMPTY : backing.removeItem(0, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        Container backing = backing();
        return backing == null ? ItemStack.EMPTY : backing.removeItemNoUpdate(0);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        Container backing = backing();
        if (backing != null) {
            backing.setItem(0, stack);
        }
    }

    @Override
    public void setChanged() {
        Container backing = backing();
        if (backing != null) {
            backing.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        setItem(0, ItemStack.EMPTY);
    }
}
