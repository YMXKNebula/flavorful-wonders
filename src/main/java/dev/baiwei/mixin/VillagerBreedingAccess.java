package dev.baiwei.mixin;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(VillagerMakeLove.class)
public interface VillagerBreedingAccess {
    @Invoker("tryToGiveBirth") void baiwei$tryToGiveBirth(ServerLevel level,Villager first,Villager second);
}
