package dev.baiwei.mixin;
import dev.baiwei.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class FluidCollisionMixin {
    @Inject(method="getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",at=@At("RETURN"),cancellable=true)
    private void baiwei$surface(BlockGetter level,BlockPos pos,CollisionContext context,CallbackInfoReturnable<VoxelShape> cir){
        if(!(context instanceof EntityCollisionContext ec)||!(ec.getEntity() instanceof LivingEntity entity)||!Effects.has(entity,8)||entity.isShiftKeyDown())return;
        var state=(BlockBehaviour.BlockStateBase)(Object)this;var fluid=state.getFluidState();
        if(fluid.isEmpty())return;
        double height=fluid.getHeight(level,pos);
        var surface=Shapes.box(0,0,0,1,height,1);
        if(context.isAbove(surface,pos,true))cir.setReturnValue(Shapes.or(cir.getReturnValue(),surface));
    }
}
