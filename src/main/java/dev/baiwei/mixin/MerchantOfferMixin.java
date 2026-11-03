package dev.baiwei.mixin;
import dev.baiwei.TradeContext;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(MerchantOffer.class)
abstract class MerchantOfferMixin {
    @Redirect(method="take",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void baiwei$freeInputs(ItemStack input,int count){if(!TradeContext.free())input.shrink(count);}
}
