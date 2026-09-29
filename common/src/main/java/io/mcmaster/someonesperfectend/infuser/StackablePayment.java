package io.mcmaster.someonesperfectend.infuser;

/**
 * Implemented by the beacon menu's payment slot and its container (via mixin). Vanilla caps both at one item; the
 * Infuser lifts the cap so its payment slot holds a stack.
 */
public interface StackablePayment {
    void someonesperfectend$allowStacks();
}
