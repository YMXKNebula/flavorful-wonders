package dev.baiwei.mixin;
import dev.baiwei.RuntimeEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WitherBoss.class)
abstract class WitherTargetMixin {
    @Inject(method="setAlternativeTarget",at=@At("HEAD"),cancellable=true)
    private void baiwei$headTarget(int head,int id,CallbackInfo ci){
        var e=((WitherBoss)(Object)this).level().getEntity(id);if(e instanceof LivingEntity living&&RuntimeEffects.dimensionPeace(living))ci.cancel();
    }
    @Inject(method="customServerAiStep",at=@At("HEAD"))
    private void baiwei$clearHeads(ServerLevel level,CallbackInfo ci){
        var wither=(WitherBoss)(Object)this;
        for(int head=0;head<3;head++){var target=level.getEntity(wither.getAlternativeTarget(head));if(target instanceof LivingEntity living&&RuntimeEffects.dimensionPeace(living))wither.setAlternativeTarget(head,0);}
    }
}
