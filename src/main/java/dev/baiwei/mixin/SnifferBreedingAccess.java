package dev.baiwei.mixin;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Sniffer.class)
public interface SnifferBreedingAccess {
    @Invoker("getState") Sniffer.State baiwei$getState();
}
