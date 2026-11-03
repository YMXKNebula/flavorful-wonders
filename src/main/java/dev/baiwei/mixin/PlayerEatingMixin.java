package dev.baiwei.mixin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import dev.baiwei.RuntimeEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
abstract class PlayerEatingMixin {
    @Inject(method="addAdditionalSaveData",at=@At("RETURN"))
    private void baiwei$saveHover(ValueOutput output,CallbackInfo ci){if((Object)this instanceof ServerPlayer p)RuntimeEffects.saveHover(p,output);}
    @Inject(method="readAdditionalSaveData",at=@At("RETURN"))
    private void baiwei$loadHover(ValueInput input,CallbackInfo ci){if((Object)this instanceof ServerPlayer p)RuntimeEffects.loadHover(p,input);}
    @Inject(method="canEat",at=@At("RETURN"),cancellable=true)
    private void baiwei$allowFullHunger(boolean alwaysEdible,CallbackInfoReturnable<Boolean> cir){cir.setReturnValue(true);}
    @Inject(method="hasEnoughFoodToDoExhaustiveManoeuvres",at=@At("RETURN"),cancellable=true)
    private void baiwei$allowLowFoodSprinting(CallbackInfoReturnable<Boolean> cir){cir.setReturnValue(true);}
}
