package dev.baiwei.mixin;
import dev.baiwei.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @WrapMethod(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z")
    private boolean baiwei$nativeEffect(MobEffectInstance instance,Entity source,Operation<Boolean> original){
        var adjusted=FoodRules.nativeEffect((LivingEntity)(Object)this,instance);
        return adjusted!=null&&original.call(adjusted,source);
    }
    @WrapMethod(method="hurtServer")
    private boolean baiwei$attack(ServerLevel level,DamageSource source,float amount,Operation<Boolean> original){
        var target=(LivingEntity)(Object)this;
        var attacker=source.getEntity() instanceof LivingEntity a&&a!=target?a:null;
        float hp=target.getHealth();
        if(attacker!=null){int handLevel=Effects.level(attacker,7);boolean hand=handLevel>0&&attacker.getRandom().nextFloat()<Mechanics.chance(.02,handLevel);amount=Mechanics.attack(amount,hp,Effects.has(attacker,11),hand);}
        var frame=DamageLedger.begin(target);boolean result;
        try{result=original.call(level,source,amount);}finally{DamageLedger.end();}
        float loss=frame.total();
        if(attacker!=null&&loss>0){
            if(Effects.has(attacker,12))attacker.heal(loss*.50f);
            if(Effects.has(attacker,16))RuntimeEffects.mightyKnockback(target,attacker);
        }
        return result;
    }
    @WrapMethod(method="setHealth")
    private void baiwei$healthSettlement(float value,Operation<Void> original){
        var e=(LivingEntity)(Object)this;float before=e.getHealth();
        if(value<before&&!e.level().isClientSide()&&e instanceof Player p&&Effects.has(e,2)){
            var food=(FoodAccess)p.getFoodData();food.baiwei$bind(p);float paid=food.baiwei$pay(before-value);value+=paid;DamageLedger.record(e,paid);
        }
        value=Mechanics.floorHealth(value,before>0&&Effects.has(e,6));original.call(value);
        DamageLedger.record(e,Math.max(0,before-e.getHealth()));
    }
    @WrapMethod(method="setAbsorptionAmount")
    private void baiwei$absorptionSettlement(float value,Operation<Void> original){
        var e=(LivingEntity)(Object)this;float before=e.getAbsorptionAmount();original.call(value);DamageLedger.record(e,Math.max(0,before-e.getAbsorptionAmount()));
    }
    @Inject(method="onEffectsRemoved",at=@At("RETURN"))
    private void baiwei$expiredCapacity(java.util.Collection<MobEffectInstance> removed,CallbackInfo ci){
        if((Object)this instanceof Player p){var f=(FoodAccess)p.getFoodData();f.baiwei$bind(p);f.baiwei$updateCapacity();if(p instanceof net.minecraft.server.level.ServerPlayer sp&&!Effects.has(p,13))RuntimeEffects.finishHover(sp);}
    }
    @Inject(method="knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",at=@At("HEAD"),cancellable=true)
    private void baiwei$noKnockback(double strength,double x,double z,DamageSource source,float multiplier,boolean vertical,CallbackInfo ci){if(Effects.has((LivingEntity)(Object)this,14))ci.cancel();}
    @Inject(method="causeFallDamage",at=@At("HEAD"),cancellable=true)
    private void baiwei$fallProtection(double distance,float multiplier,DamageSource source,CallbackInfoReturnable<Boolean> cir){
        var entity=(LivingEntity)(Object)this;
        if(Effects.has(entity,13)||RuntimeEffects.waterSurfaceLanding(entity)){entity.resetFallDistance();cir.setReturnValue(false);}
    }
    @Inject(method="canAttack",at=@At("HEAD"),cancellable=true)
    private void baiwei$noHostility(LivingEntity target,CallbackInfoReturnable<Boolean> cir){if((Object)this instanceof Mob&&RuntimeEffects.dimensionPeace(target))cir.setReturnValue(false);}
}
