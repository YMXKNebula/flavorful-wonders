package dev.baiwei.mixin;
import dev.baiwei.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(FoodData.class)
abstract class FoodDataMixin implements FoodAccess {
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Shadow private float exhaustionLevel;
    @Unique private Player baiwei$owner;
    @Unique private float baiwei$fraction;
    @Unique private int baiwei$sprintHungerTicks;
    @Override public void baiwei$bind(Player p){baiwei$owner=p;}
    @Override public float baiwei$available(){return foodLevel+baiwei$fraction;}
    @Override public float baiwei$pay(float request){
        float paid=Math.min(Math.max(request,0),baiwei$available()),left=baiwei$available()-paid;
        foodLevel=(int)Math.floor(left);baiwei$fraction=left-foodLevel;
        saturationLevel=Math.min(saturationLevel,left);return paid;
    }
    @Override public void baiwei$updateCapacity(){
        int cap=baiwei$owner!=null&&Effects.has(baiwei$owner,18)?40:20;
        if(foodLevel>=cap){foodLevel=cap;baiwei$fraction=0;}
        saturationLevel=Math.min(saturationLevel,Math.min(cap,baiwei$available()));
    }
    @WrapMethod(method="add")
    private void baiwei$nutrition(int food,float saturation,Operation<Void> original){
        if(baiwei$owner==null){original.call(food,saturation);return;}
        int cap=Effects.has(baiwei$owner,18)?40:20;
        int multiplier=FoodRules.currentFood()!=null&&Effects.has(baiwei$owner,17)?2:1;
        float total=Math.clamp(baiwei$available()+food*multiplier,0,cap);
        foodLevel=(int)Math.floor(total);baiwei$fraction=total-foodLevel;
        saturationLevel=Math.clamp(saturationLevel+saturation*multiplier,0,total);
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void baiwei$bindTick(ServerPlayer p,CallbackInfo ci){
        baiwei$owner=p;baiwei$updateCapacity();
        if(foodLevel==0&&baiwei$fraction>0&&exhaustionLevel>4&&saturationLevel<=0)baiwei$fraction=0;
    }
    @Inject(method="tick",at=@At("RETURN"))
    private void baiwei$sprintHunger(ServerPlayer p,CallbackInfo ci){
        if(baiwei$available()>6){baiwei$sprintHungerTicks=0;return;}
        if(!p.isSprinting()||p.isCreative()||p.isSpectator())return;
        if(++baiwei$sprintHungerTicks>=80){baiwei$sprintHungerTicks=0;SprintHunger.hurt(p);}
    }
    @ModifyConstant(method="tick",constant=@Constant(intValue=18))
    private int baiwei$vitalityThreshold(int n){return baiwei$healsFromFood()?1:n;}
    @Unique private boolean baiwei$healsFromFood(){return baiwei$owner!=null&&(Effects.has(baiwei$owner,3)||Effects.has(baiwei$owner,4));}
    @ModifyConstant(method="tick",constant=@Constant(intValue=10))
    private int baiwei$fastRegeneration(int n){return baiwei$owner!=null&&Effects.has(baiwei$owner,4)?n/2:n;}
    @ModifyConstant(method="tick",constant=@Constant(intValue=80,ordinal=0))
    private int baiwei$slowRegeneration(int n){return baiwei$owner!=null&&Effects.has(baiwei$owner,4)?n/2:n;}
    @ModifyExpressionValue(method="tick",at=@At(value="INVOKE",target="Ljava/lang/Boolean;booleanValue()Z"))
    private boolean baiwei$effectHealing(boolean rule){return rule||baiwei$healsFromFood();}
    @ModifyExpressionValue(method="tick",at=@At(value="FIELD",target="Lnet/minecraft/world/food/FoodData;foodLevel:I",opcode=org.objectweb.asm.Opcodes.GETFIELD))
    private int baiwei$fractionalReserve(int level){return level==0&&baiwei$fraction>0?1:level;}
    @Inject(method="needsFood",at=@At("HEAD"),cancellable=true)
    private void baiwei$capacity(CallbackInfoReturnable<Boolean> cir){if(baiwei$owner!=null&&Effects.has(baiwei$owner,18))cir.setReturnValue(baiwei$available()<40);}
    @Inject(method="readAdditionalSaveData",at=@At("RETURN"))
    private void baiwei$read(ValueInput input,CallbackInfo ci){baiwei$fraction=Math.clamp(input.getFloatOr("baiwei_food_fraction",0),0,.999999f);baiwei$sprintHungerTicks=Math.clamp(input.getIntOr("baiwei_sprint_hunger_ticks",0),0,79);}
    @Inject(method="addAdditionalSaveData",at=@At("RETURN"))
    private void baiwei$save(ValueOutput output,CallbackInfo ci){output.putFloat("baiwei_food_fraction",baiwei$fraction);output.putInt("baiwei_sprint_hunger_ticks",baiwei$sprintHungerTicks);}
    @Inject(method="setFoodLevel",at=@At("HEAD"))
    private void baiwei$resetFraction(int value,CallbackInfo ci){baiwei$fraction=0;}
}
