package dev.baiwei.mixin;
import dev.baiwei.*;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Mob.class)
abstract class MobMixin {
    @Inject(method="canAttack",at=@At("HEAD"),cancellable=true)
    private void baiwei$protect(LivingEntity target,CallbackInfoReturnable<Boolean> cir){if(RuntimeEffects.dimensionPeace(target))cir.setReturnValue(false);}
    @Inject(method="setTarget",at=@At("HEAD"),cancellable=true)
    private void baiwei$target(LivingEntity target,CallbackInfo ci){if(target!=null&&RuntimeEffects.dimensionPeace(target))ci.cancel();}
    @Inject(method="tick",at=@At("HEAD"))
    private void baiwei$clearTarget(CallbackInfo ci){if(!((Mob)(Object)this).level().isClientSide())RuntimeEffects.clearProtectedTarget((Mob)(Object)this);}
}
