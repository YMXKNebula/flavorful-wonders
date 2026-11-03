package dev.baiwei;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import dev.baiwei.mixin.TurtleBreedingAccess;
import dev.baiwei.mixin.HorseBreedingAccess;
import dev.baiwei.mixin.VillagerBreedingAccess;
import dev.baiwei.mixin.SnifferBreedingAccess;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.*;

public final class RuntimeEffects {
    private record Flight(boolean mayFly,boolean flying){}
    private static final Map<ServerPlayer,Flight> FLIGHT=new WeakHashMap<>();
    public static void playerTick(ServerPlayer player){
        var f=(FoodAccess)player.getFoodData();f.baiwei$bind(player);f.baiwei$updateCapacity();
        var ability=player.getAbilities();
        if(Effects.has(player,13)){
            if(!FLIGHT.containsKey(player)){FLIGHT.put(player,new Flight(ability.mayfly,ability.flying));ability.mayfly=true;ability.flying=true;player.onUpdateAbilities();}
            else if(!ability.mayfly){ability.mayfly=true;player.onUpdateAbilities();}
        } else finishHover(player);
    }
    public static void finishHover(ServerPlayer player){
        var old=FLIGHT.remove(player);
        if(old!=null&&!player.isCreative()&&!player.isSpectator()){
            var ability=player.getAbilities();ability.mayfly=old.mayFly();ability.flying=old.flying()&&old.mayFly();player.onUpdateAbilities();
        }
    }
    public static void saveHover(ServerPlayer player,ValueOutput output){
        var old=FLIGHT.get(player);if(old==null)return;
        output.putBoolean("baiwei_hover_snapshot",true);
        output.putBoolean("baiwei_hover_mayfly",old.mayFly());output.putBoolean("baiwei_hover_flying",old.flying());
    }
    public static void loadHover(ServerPlayer player,ValueInput input){
        if(input.getBooleanOr("baiwei_hover_snapshot",false))FLIGHT.put(player,new Flight(input.getBooleanOr("baiwei_hover_mayfly",false),input.getBooleanOr("baiwei_hover_flying",false)));
        else FLIGHT.remove(player);
    }
    public static void copyHover(ServerPlayer oldPlayer,ServerPlayer newPlayer){
        var old=FLIGHT.remove(oldPlayer);if(old==null)return;
        FLIGHT.put(newPlayer,old);if(!Effects.has(newPlayer,13))finishHover(newPlayer);
    }
    public static boolean dimensionPeace(LivingEntity entity){
        var dim=entity.level().dimension();
        return dim.equals(Level.NETHER)&&Effects.has(entity,9)||dim.equals(Level.END)&&Effects.has(entity,10);
    }
    public static void auraTick(ServerLevel level,LivingEntity source,int kind){
        if(kind==21){forest(level,source);return;}
        if(kind==20&&source.tickCount%20==0){
            for(var entity:level.getEntitiesOfClass(LivingEntity.class,source.getBoundingBox().inflate(5),
                    e->e.isAlive()&&!(e instanceof net.minecraft.world.entity.decoration.ArmorStand)&&e.distanceToSqr(source)<=25)){
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,entity.getX(),entity.getY()+entity.getBbHeight()+.3,entity.getZ(),1,.15,.1,.15,0);
            }
        }
        var nearby=level.getEntitiesOfClass(Mob.class,source.getBoundingBox().inflate(5),m->m!=source&&m.isAlive()&&m.distanceToSqr(source)<=25);
        if(kind==19){
            for(var mob:nearby){
                Vec3 away=mob.position().subtract(source.position());
                if(away.horizontalDistanceSqr()<.0001)away=new Vec3(1,0,0);
                Vec3 dest=mob.position().add(away.normalize().scale(6));
                mob.getNavigation().moveTo(dest.x,dest.y,dest.z,1);
                mob.getMoveControl().setWantedPosition(dest.x,dest.y,dest.z,1);
                // Bosses/flying or immobilized AI still receive the same outward movement intent.
                if(mob.getNavigation().isDone())mob.setDeltaMovement(mob.getDeltaMovement().add(away.normalize().scale(.08)));
            }
        } else if(kind==20){
            for(var mob:nearby){
                if(mob instanceof Animal animal&&animal.getAge()==0&&animal.canFallInLove()){
                    if(animal instanceof Frog&&animal.getBrain().hasMemoryValue(MemoryModuleType.IS_PREGNANT))continue;
                    animal.setInLove(source instanceof net.minecraft.world.entity.player.Player p?p:null);
                    if(animal instanceof AbstractHorse horse&&!((HorseBreedingAccess)horse).baiwei$canParent()||animal instanceof Sniffer sniffer&&!Set.of(Sniffer.State.IDLING,Sniffer.State.SCENTING,Sniffer.State.FEELING_HAPPY).contains(((SnifferBreedingAccess)sniffer).baiwei$getState())){animal.resetLove();continue;}
                    if(animal instanceof Turtle turtle){
                        // Turtle breeding lives in TurtleBreedGoal, not Animal.spawnChildFromBreeding.
                        ((TurtleBreedingAccess)turtle).baiwei$setHasEgg(true);
                        turtle.finalizeSpawnChildFromBreeding(level,turtle,null);
                    }else animal.spawnChildFromBreeding(level,animal);
                } else if(mob instanceof Villager villager&&villager.getAge()==0&&villager.canBreed()){
                    // Native POI bed reservation, reachability, baby creation and cooldown.
                    ((VillagerBreedingAccess)new VillagerMakeLove()).baiwei$tryToGiveBirth(level,villager,villager);
                    if(villager.getAge()>0)villager.eatAndDigestFood();
                }
            }
        }
    }
    private static void forest(ServerLevel level,LivingEntity source){
        BlockPos center=source.blockPosition();
        for(BlockPos p:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,5,5))){
            if(p.distToCenterSqr(source.position())>25||!level.hasChunkAt(p))continue;
            BlockState state=level.getBlockState(p);
            if(!(state.getBlock() instanceof BonemealableBlock growable))continue;
            if(!growable.isValidBonemealTarget(level,p,state,BonemealSource.INTERACTION))continue;
            // Mature explicit age states once. No repeat growth or artificial cooldown on mature crops.
            IntegerProperty age=null;
            for(var prop:state.getProperties())if(prop instanceof IntegerProperty ip&&ip.getName().equals("age")){age=ip;break;}
            if(age!=null){
                int max=Collections.max(age.getPossibleValues());
                // Native growth preserves multi-block crops and addon side effects.
                for(int step=0;step<=max&&state.hasProperty(age)&&state.getValue(age)<max;step++){
                    var before=state;growable.performBonemeal(level,level.getRandom(),p.immutable(),state,BonemealSource.INTERACTION);
                    state=level.getBlockState(p);
                    if(state.equals(before)||!(state.getBlock() instanceof BonemealableBlock next))break;
                    growable=next;
                }
            }
            else if(growable.isBonemealSuccess(level,level.getRandom(),p,state,BonemealSource.INTERACTION))growable.performBonemeal(level,level.getRandom(),p.immutable(),state,BonemealSource.INTERACTION);
        }
    }
    public static void clearProtectedTarget(Mob mob){
        var target=mob.getTarget();
        if(target!=null&&dimensionPeace(target)){
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        }
    }
    public static void mightyKnockback(LivingEntity target,LivingEntity attacker){
        if(Effects.has(target,14))return;
        var delta=target.position().subtract(attacker.position());
        if(delta.horizontalDistanceSqr()<1e-8)delta=attacker.getLookAngle();
        // Native-style velocity impulse: normal movement handles inertia, collisions and synchronization.
        // Add to existing horizontal momentum so sprint/enchantment knockback remains effective.
        var horizontal=new Vec3(delta.x,0,delta.z).normalize().scale(.8);
        var velocity=target.getDeltaMovement();
        double lift=target.onGround()?Math.max(0,Math.min(.4,velocity.y/2+.8)-velocity.y):0;
        // The original mechanism applies to all living targets, including resistant mobs.
        // Entity.push still gives Immoveable the final veto and marks velocity for synchronization.
        target.push(horizontal.x,lift,horizontal.z);
    }
    public static boolean waterSurfaceLanding(LivingEntity entity){
        if(!Effects.has(entity,8))return false;
        var pos=BlockPos.containing(entity.getX(),entity.getY()-.05,entity.getZ());
        var fluid=entity.level().getFluidState(pos);
        return fluid.is(net.minecraft.tags.FluidTags.WATER)&&entity.getY()<=pos.getY()+fluid.getHeight(entity.level(),pos)+.06;
    }
}
