package net.bettercombat.logic;

import net.bettercombat.BetterCombat;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.ComboState;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.utils.ResettableCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static net.minecraft.entity.EquipmentSlot.MAINHAND;

public class PlayerAttackHelper {

    public static final Logger LOGGER = LogManager.getLogger(PlayerAttackHelper.class);
    public static final ResettableCounter<AttackHand> resettableCounter = new ResettableCounter<>();

    public static float getDualWieldingAttackDamageMultiplier(PlayerEntity player, AttackHand hand) {
        return isDualWielding(player)
                ? (hand.isOffHand()
                ? BetterCombat.config.dual_wielding_off_hand_damage_multiplier
                : BetterCombat.config.dual_wielding_main_hand_damage_multiplier)
                : 1;
    }

    public static boolean shouldAttackWithOffHand(PlayerEntity player, int comboCount) {
        return PlayerAttackHelper.isDualWielding(player) && comboCount % 2 == 1;
    }

    public static boolean isDualWielding(PlayerEntity player) {
        var mainAttributes = WeaponRegistry.getAttributes(player.getMainHandStack());
        var offAttributes = WeaponRegistry.getAttributes(player.getOffHandStack());
        return mainAttributes != null && !mainAttributes.isTwoHanded()
                && offAttributes != null && !offAttributes.isTwoHanded();
    }

    public static boolean isTwoHandedWielding(PlayerEntity player) {
        var mainAttributes = WeaponRegistry.getAttributes(player.getMainHandStack());
        if (mainAttributes != null) {
            return mainAttributes.isTwoHanded();
        }
        return false;
    }

    public static float getAttackCooldownTicksCapped(PlayerEntity player) {
        // `getAttackCooldownProgressPerTick` should be called `getAttackCooldownLengthTicks`
        return Math.max(player.getAttackCooldownProgressPerTick(), BetterCombat.config.attack_interval_cap);
    }

    public static AttackHand getAttackHand(PlayerEntity player, int comboCount) {
        if(player.getWorld().isClient){
            PlayerInputState.get(player.getUuid()).setMask(InputManager.generateMask(MinecraftClient.getInstance()));
        }
        var cachedValue = resettableCounter.get();
        if (cachedValue != null) {
            return cachedValue;
        }
        if (isDualWielding(player)) {
            boolean isOffHand = shouldAttackWithOffHand(player, comboCount);
            var itemStack = isOffHand
                    ? player.getOffHandStack()
                    : player.getMainHandStack();
            var attributes = WeaponRegistry.getAttributes(itemStack);
            if (attributes != null && (attributes.attacks() != null || attributes.blocks() != null || attributes.animations() != null)) {
                int handSpecificComboCount = ((isOffHand && comboCount > 0) ? (comboCount - 1) : (comboCount)) / 2;

                AttackSelection attackSelection = null;
                BlockSelection blockSelection = null;
                AnimationSelection animationSelection = null;
                if (attributes.blocks() != null) {
                    blockSelection = selectBlock(attributes, player, isOffHand);
                }
                if (attributes.animations() != null) {
                    animationSelection = selectAnimation(attributes, player, isOffHand);
                }
                if (attributes.attacks() != null) {
                    attackSelection = selectAttack(handSpecificComboCount, attributes, player, isOffHand);
                }
                var attack = attackSelection.attack;
                var combo = attackSelection.comboState;
                WeaponAttributes.Block block = blockSelection == null ? null : blockSelection.block;
                WeaponAttributes.Animation animation = animationSelection == null ? null : animationSelection.animation;
                var attackHand = new AttackHand(attack, block, animation, combo, isOffHand, attributes, itemStack);
                return attackHand;
            }
        } else {
            var itemStack = player.getMainHandStack();
            WeaponAttributes attributes = WeaponRegistry.getAttributes(itemStack);
            if (attributes != null && (attributes.attacks() != null || attributes.blocks() != null || attributes.animations() != null)) {
                BlockSelection blockSelection = null;
                AnimationSelection animationSelection = null;
                AttackSelection attackSelection = null;
                if (attributes.blocks() != null) {
                    blockSelection = selectBlock(attributes, player, false);
                }
                if (attributes.animations() != null) {
                    animationSelection = selectAnimation(attributes, player, false);
                }
                if (attributes.attacks() != null) {
                    attackSelection = selectAttack(comboCount, attributes, player, false);
                }
                var attack = attackSelection.attack;
                var combo = attackSelection.comboState;
                WeaponAttributes.Block block = blockSelection == null ? null : blockSelection.block;
                WeaponAttributes.Animation animation = animationSelection == null ? null : animationSelection.animation;
                var attackHand = new AttackHand(attack, block, animation, combo, false, attributes, itemStack);
                return attackHand;
            }
        }
        return null;
    }

    private record AttackSelection(WeaponAttributes.Attack attack, ComboState comboState) {
    }

    private record BlockSelection(WeaponAttributes.Block block) {
    }

    private record AnimationSelection(WeaponAttributes.Animation animation) {
    }

    private static AttackSelection selectAttack(int comboCount, WeaponAttributes attributes, PlayerEntity player, boolean isOffHandAttack) {
        var attacks = attributes.attacks();
        attacks = Arrays.stream(attacks)
                .filter(attack ->
                        attack.conditions() == null
                                || attack.conditions().length == 0
                                || evaluateConditions(attack.conditions(), player, isOffHandAttack)
                )
                .toArray(WeaponAttributes.Attack[]::new);
        if (comboCount < 0) {
            comboCount = 0;
        }
        int index = comboCount % attacks.length;
        return new AttackSelection(attacks[index], new ComboState(index + 1, attacks.length));
    }

    private static BlockSelection selectBlock(WeaponAttributes attributes, PlayerEntity player, boolean isOffHandAttack) {
        var blocks = attributes.blocks();
        blocks = Arrays.stream(blocks)
                .filter(block ->
                        block.conditions() == null
                                || block.conditions().length == 0
                                || evaluateBlockConditions(block.conditions(), player, isOffHandAttack)
                )
                .toArray(WeaponAttributes.Block[]::new);
        if (blocks != null && blocks.length > 0) {
            return new BlockSelection(blocks[0]);
        }
        return null;
    }

    private static AnimationSelection selectAnimation(WeaponAttributes attributes, PlayerEntity player, boolean isOffHandAttack) {
        var animations = attributes.animations();
        animations = Arrays.stream(animations)
                .filter(animation ->
                        animation.condition() == null
                                || evaluateAnimationConditions(animation.condition(), player, isOffHandAttack)
                )
                .toArray(WeaponAttributes.Animation[]::new);
        if (animations != null && animations.length > 0) {
            return new AnimationSelection(animations[0]);
        }
        return null;
    }

    private static boolean evaluateBlockConditions(WeaponAttributes.Block.Condition[] conditions, PlayerEntity player, boolean isOffHandAttack) {
        return Arrays.stream(conditions).allMatch(condition -> evaluateBlockCondition(condition, player, isOffHandAttack));
    }

    private static boolean evaluateAnimationConditions(WeaponAttributes.Animation.Condition condition, PlayerEntity player, boolean isOffHandAttack) {
        return evaluateAnimationCondition(condition, player, isOffHandAttack);
    }

    private static boolean evaluateBlockCondition(WeaponAttributes.Block.Condition condition, PlayerEntity player, boolean isOffHandAttack) {
        if (condition == null) {
            return true;
        }
        var mask = PlayerInputState.get(player.getUuid()).getMask();
        switch (condition) {
            case BLOCK_BUTT -> {
                return InputManager.rightKey(mask);
            }
            case BLOCK_LEFT -> {
                return InputManager.leftKey(mask);
            }
            case BLOCK_RIGHT -> {
                return InputManager.backKey(mask);
            }
            case BLOCK_UP -> {
                return InputManager.forwardKey(mask);
            }
            case BLOCK_DEFAULT -> {
                return !InputManager.forwardKey(mask) &&
                        !InputManager.leftKey(mask) &&
                        !InputManager.rightKey(mask) &&
                        !InputManager.backKey(mask);
            }
        }
        return true;
    }

    private static boolean evaluateAnimationCondition(WeaponAttributes.Animation.Condition condition, PlayerEntity player, boolean isOffHandAttack) {
        if (condition == null) {
            return true;
        }
        var mask = PlayerInputState.get(player.getUuid()).getMask();
        return InputManager.isPressed(mask, condition.getButtons());
    }

    private static boolean evaluateConditions(WeaponAttributes.Condition[] conditions, PlayerEntity player, boolean isOffHandAttack) {
        return Arrays.stream(conditions).allMatch(condition -> evaluateCondition(condition, player, isOffHandAttack));
    }

    private static boolean evaluateCondition(WeaponAttributes.Condition condition, PlayerEntity player, boolean isOffHandAttack) {
        if (condition == null) {
            return true;
        }
        var mask = PlayerInputState.get(player.getUuid()).getMask();
        switch (condition) {
            case NOT_DUAL_WIELDING -> {
                return !isDualWielding(player);
            }
            case DUAL_WIELDING_ANY -> {
                return isDualWielding(player);
            }
            case DUAL_WIELDING_SAME -> {
                return isDualWielding(player) &&
                        (player.getMainHandStack().getItem() == player.getOffHandStack().getItem());
            }
            case DUAL_WIELDING_SAME_CATEGORY -> {
                if (!isDualWielding(player)) {
                    return false;
                }
                var mainHandAttributes = WeaponRegistry.getAttributes(player.getMainHandStack());
                var offHandAttributes = WeaponRegistry.getAttributes(player.getOffHandStack());
                if (mainHandAttributes.category() == null
                        || mainHandAttributes.category().isEmpty()
                        || offHandAttributes.category() == null
                        || offHandAttributes.category().isEmpty()) {
                    return false;
                }
                return mainHandAttributes.category().equals(offHandAttributes.category());
            }
            case NO_OFFHAND_ITEM -> {
                var offhandStack = player.getOffHandStack();
                if (offhandStack == null || offhandStack.isEmpty()) {
                    {
                        return true;
                    }
                }
                return false;
            }
            case OFF_HAND_SHIELD -> {
                var offhandStack = player.getOffHandStack();
                if (offhandStack != null || offhandStack.getItem() instanceof ShieldItem) {
                    {
                        return true;
                    }
                }
                return false;
            }
            case MAIN_HAND_ONLY -> {
                return !isOffHandAttack;
            }
            case OFF_HAND_ONLY -> {
                return isOffHandAttack;
            }
            case MOUNTED -> {
                return player.getVehicle() != null;
            }
            case NOT_MOUNTED -> {
                return player.getVehicle() == null;
            }
            case LEFT_MOVE -> {
                return InputManager.leftKey(mask);
            }
            case RIGHT_MOVE -> {
                return InputManager.backKey(mask) &&
                        !InputManager.leftKey(mask);
            }
            case UP_MOVE -> {
                return InputManager.forwardKey(mask) &&
                        !InputManager.leftKey(mask) &&
                        !InputManager.backKey(mask);
            }
            case BOTTOM_MOVE -> {
                return InputManager.rightKey(mask) &&
                        !InputManager.forwardKey(mask) &&
                        !InputManager.leftKey(mask) &&
                        !InputManager.backKey(mask);
            }
            case NOT_MOVE -> {
                return InputManager.notMoved(mask);
            }
        }
        return true;
    }

    private static final Object attributesLock = new Object();

    public static void offhandAttributes(PlayerEntity player, Runnable runnable) {
        synchronized (attributesLock) {
            setAttributesForOffHandAttack(player, true);
            runnable.run();
            setAttributesForOffHandAttack(player, false);
        }
    }

    public static void setAttributesForOffHandAttack(PlayerEntity player, boolean useOffHand) {
        var mainHandStack = player.getMainHandStack();
        var offHandStack = player.getOffHandStack();
        ItemStack add;
        ItemStack remove;
        if (useOffHand) {
            remove = mainHandStack;
            add = offHandStack;
        } else {
            remove = offHandStack;
            add = mainHandStack;
        }
        if (remove != null) {
            player.getAttributes().removeModifiers(remove.getAttributeModifiers(MAINHAND));
        }
        if (add != null) {
            player.getAttributes().addTemporaryModifiers(add.getAttributeModifiers(MAINHAND));
        }
    }
}
