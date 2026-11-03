package dev.baiwei.mixin;
import dev.baiwei.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
abstract class ConsumptionMixin {
    @WrapMethod(method="finishUsingItem")
    private ItemStack baiwei$finish(Level level,LivingEntity user,Operation<ItemStack> original){
        var self=(ItemStack)(Object)this;
        if(user instanceof Player p)((FoodAccess)p.getFoodData()).baiwei$bind(p);
        var old=FoodRules.begin(user,self);
        String food=FoodRules.currentFood();
        try{var result=original.call(level,user);FoodRules.applyAdded(user,food);return result;}
        finally{FoodRules.restore(old);}
    }
    @Inject(method="getUseDuration",at=@At("RETURN"),cancellable=true)
    private void baiwei$speed(LivingEntity user,CallbackInfoReturnable<Integer> cir){
        var stack=(ItemStack)(Object)this;
        if(Effects.has(user,15)&&stack.has(DataComponents.CONSUMABLE))cir.setReturnValue(Math.max(1,(cir.getReturnValueI()+1)/2));
    }
    @Inject(method="processDurabilityChange",at=@At("HEAD"),cancellable=true)
    private void baiwei$durability(int amount,net.minecraft.server.level.ServerLevel level,net.minecraft.server.level.ServerPlayer player,CallbackInfoReturnable<Integer> cir){
        if(amount>0&&player!=null&&Effects.has(player,1))cir.setReturnValue(0);
    }
}
