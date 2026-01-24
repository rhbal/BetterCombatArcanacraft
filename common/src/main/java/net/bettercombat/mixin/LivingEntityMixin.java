package net.bettercombat.mixin;

import net.bettercombat.logic.InputManager;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.PlayerAttackProperties;
import net.bettercombat.logic.PlayerInputState;
import net.bettercombat.logic.knockback.ConfigurableKnockback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements ConfigurableKnockback {

    // FEATURE: Dual wielded attacking - Client side weapon cooldown for offhand

    @Inject(method = "getAttributeValue(Lnet/minecraft/entity/attribute/EntityAttribute;)D", at = @At("HEAD"), cancellable = true)
    public void getAttributeValue_Inject(EntityAttribute attribute, CallbackInfoReturnable<Double> cir) {
        var object = (Object) this;
        if (object instanceof PlayerEntity) {
            var player = (PlayerEntity) object;
            var comboCount = ((PlayerAttackProperties) player).getComboCount();
            if (player.getWorld().isClient
                    && comboCount > 0
                    && PlayerAttackHelper.shouldAttackWithOffHand(player, comboCount)) {
                PlayerAttackHelper.offhandAttributes(player, () -> {
                    var value = player.getAttributes().getValue(attribute);
                    cir.setReturnValue(value);
                });
                cir.cancel();
            }
        }
    }

    // MARK: ConfigurableKnockback
    private float customKnockbackMultiplier_BetterCombat = 1;

    @Override
    public void setKnockbackMultiplier_BetterCombat(float value) {
        customKnockbackMultiplier_BetterCombat = value;
    }


    @Inject(method = "blockedByShield", at = @At("HEAD"), cancellable = true)
    public void blockedByWeapon(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Entity direct = source.getSource();
        if (direct instanceof ProjectileEntity){
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        int selfMask = PlayerInputState.get(self.getUuid()).getMask();
        boolean isRightDirection = rightDirection(source, self);
        if (InputManager.rightClick(selfMask) && self.getMainHandStack().getItem() instanceof SwordItem && isRightDirection) {

            Entity attacker = source.getAttacker();
            if (attacker instanceof PlayerEntity) {
                int attackerMask = PlayerInputState.get(attacker.getUuid()).getMask();
                if((InputManager.a(selfMask) && InputManager.d(attackerMask)) ||
                        (InputManager.d(selfMask) && InputManager.a(attackerMask)) ||
                        (InputManager.w(selfMask) && InputManager.w(attackerMask)) ||
                        (InputManager.s(selfMask) && InputManager.s(attackerMask)) ||
                        (InputManager.notMoved(selfMask) && InputManager.notMoved(attackerMask))
                ){
                    cir.setReturnValue(true);
                }
            } else {
                cir.setReturnValue(true);
            }
        }
    }

    private static boolean rightDirection(DamageSource source, LivingEntity self) {
        Vec3d vec3d = source.getPosition();
        if (vec3d != null) {
            Vec3d vec3d2 = self.getRotationVec(1.0F);
            Vec3d vec3d3 = vec3d.relativize(self.getPos()).normalize();
            vec3d3 = new Vec3d(vec3d3.x, (double) 0.0F, vec3d3.z);
            return vec3d3.dotProduct(vec3d2) < (double) 0.0F;
        }
        return false;
    }

    @ModifyVariable(method = "takeKnockback", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    public double takeKnockback_HEAD_changeStrength(double knockbackStrength) {
        return knockbackStrength * customKnockbackMultiplier_BetterCombat;
    }
}
