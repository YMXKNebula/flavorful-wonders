package dev.baiwei.mixin;
import net.minecraft.world.entity.animal.turtle.Turtle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Turtle.class)
public interface TurtleBreedingAccess {
    @Invoker("setHasEgg") void baiwei$setHasEgg(boolean hasEgg);
}
