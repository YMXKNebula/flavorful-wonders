package dev.baiwei.mixin;
import dev.baiwei.Effects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
abstract class EntityImpulseMixin {
    @Inject(method="push(DDD)V",at=@At("HEAD"),cancellable=true)
    private void baiwei$blockForcedImpulse(double x,double y,double z,CallbackInfo ci){if((Object)this instanceof LivingEntity e&&Effects.has(e,14))ci.cancel();}
}
