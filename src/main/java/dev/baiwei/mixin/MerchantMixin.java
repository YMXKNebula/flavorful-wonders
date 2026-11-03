package dev.baiwei.mixin;
import dev.baiwei.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
@Mixin(MerchantResultSlot.class)
abstract class MerchantMixin {
    @WrapMethod(method="onTake")
    private void baiwei$trade(Player player,ItemStack stack,Operation<Void> original){
        boolean free=!player.level().isClientSide()&&Effects.level(player,5)>0&&player.getRandom().nextFloat()<Mechanics.chance(.10,Effects.level(player,5));
        TradeContext.begin(free);
        try{original.call(player,stack);}finally{TradeContext.end();}
    }
}
