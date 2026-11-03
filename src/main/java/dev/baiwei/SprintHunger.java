package dev.baiwei;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.*;
public final class SprintHunger {
    private static final ResourceKey<DamageType> TYPE=ResourceKey.create(Registries.DAMAGE_TYPE,Identifier.fromNamespaceAndPath(BaiweiMod.ID,"sprint_hunger"));
    /** Native starvation eligibility and one native damage point; independent 80-tick timer. */
    public static void hurt(ServerPlayer p){
        var difficulty=p.level().getDifficulty();float hp=p.getHealth();
        if(hp>10||difficulty==Difficulty.HARD||hp>1&&difficulty==Difficulty.NORMAL){
            var holder=p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(TYPE);
            p.hurtServer(p.level(),new DamageSource(holder),1);
        }
    }
}
