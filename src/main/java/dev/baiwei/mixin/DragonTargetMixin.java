package dev.baiwei.mixin;
import dev.baiwei.RuntimeEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(DragonStrafePlayerPhase.class)
abstract class DragonTargetMixin {
    @Shadow private LivingEntity attackTarget;
    @Inject(method="setTarget",at=@At("HEAD"),cancellable=true)
    private void baiwei$protectedTarget(LivingEntity target,CallbackInfo ci){if(RuntimeEffects.dimensionPeace(target)){attackTarget=null;ci.cancel();}}
    @Inject(method="doServerTick",at=@At("HEAD"))
    private void baiwei$clearProtectedTarget(ServerLevel level,CallbackInfo ci){if(attackTarget!=null&&RuntimeEffects.dimensionPeace(attackTarget))attackTarget=null;}
}
