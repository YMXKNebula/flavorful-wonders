package dev.baiwei.mixin;
import dev.baiwei.RuntimeEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(TargetingConditions.class)
abstract class TargetingMixin {
    @Shadow @Final private boolean isCombat;
    @Inject(method="test",at=@At("HEAD"),cancellable=true)
    private void baiwei$peacefulTarget(ServerLevel level,LivingEntity attacker,LivingEntity target,CallbackInfoReturnable<Boolean> cir){
        if(isCombat&&(attacker==null||attacker instanceof Mob)&&RuntimeEffects.dimensionPeace(target))cir.setReturnValue(false);
    }
}
