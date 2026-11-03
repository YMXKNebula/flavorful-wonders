package dev.baiwei.test.mixin;
import dev.baiwei.test.HeartParticleCapture;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerLevel.class)
abstract class HeartParticleCaptureMixin {
    @Inject(method="sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",at=@At("HEAD"))
    private void baiwei$observeHearts(ParticleOptions options,double x,double y,double z,int count,double dx,double dy,double dz,double speed,CallbackInfoReturnable<Integer> cir){
        if(options.getType()==ParticleTypes.HEART)HeartParticleCapture.record(x,y,z);
    }
}
