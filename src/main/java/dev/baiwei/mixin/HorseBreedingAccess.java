package dev.baiwei.mixin;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(AbstractHorse.class)
public interface HorseBreedingAccess {
    @Invoker("canParent") boolean baiwei$canParent();
}
