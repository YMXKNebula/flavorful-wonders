package dev.baiwei.test;

import dev.baiwei.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import java.util.Optional;

public final class BaiweiGameTests {
    private static ServerPlayer player(GameTestHelper t){
        var p=(ServerPlayer)t.makeMockServerPlayer(GameType.SURVIVAL);
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        t.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(),false));
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.getAbilities().invulnerable=false;
        p.setHealth(20);p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(0);((FoodAccess)p.getFoodData()).baiwei$bind(p);return p;
    }
    private static void effect(LivingEntity e,int id){e.addEffect(new MobEffectInstance(Effects.get(id),2000,0));}
    private static ItemStack food(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));}
    private static void eq(GameTestHelper t,float actual,float expected,String message){t.assertTrue(Math.abs(actual-expected)<.001,message+": "+actual+" expected "+expected);}

    @GameTest public void allTwentyOneEffectsRegister(GameTestHelper t){for(int n=1;n<=21;n++)t.assertTrue(Effects.get(n)!=null,"Missing original "+n);t.succeed();}
    @GameTest public void fullHungerStillConsumesAndReturnsBowl(GameTestHelper t){
        var p=player(t);p.getFoodData().setFoodLevel(20);var rice=food("farmersdelight:cooked_rice");
        t.assertTrue(rice.get(DataComponents.CONSUMABLE).canConsume(p,rice),"Full player must be able to consume");
        var result=rice.finishUsingItem(t.getLevel(),p);
        t.assertTrue(result.is(Items.BOWL)||p.getInventory().contains(new ItemStack(Items.BOWL)),"Native bowl must be returned");
        t.assertTrue(Effects.has(p,3),"Cooked rice must grant approved vitality");t.succeed();
    }
    @GameTest public void removalDoesNotClearExistingNourishment(GameTestHelper t){
        var p=player(t);var n=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("farmersdelight:nourishment")).orElseThrow();
        p.addEffect(new MobEffectInstance(n,999,0));food("farmersdelight:beef_stew").finishUsingItem(t.getLevel(),p);
        t.assertTrue(p.getEffect(n)!=null&&p.getEffect(n).getDuration()==999,"Removed food effect must not clear a preexisting buff");t.succeed();
    }
    @GameTest public void doubleMealAndStomachUseSeparateCapAndNutrition(GameTestHelper t){
        var p=player(t);effect(p,17);effect(p,18);p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(0);
        new ItemStack(Items.BREAD).finishUsingItem(t.getLevel(),p);
        eq(t,p.getFoodData().getFoodLevel(),30,"Bread nutrition must double once");eq(t,p.getFoodData().getSaturationLevel(),12,"Bread saturation must double once");
        p.removeEffect(Effects.get(18));RuntimeEffects.playerTick(p);eq(t,p.getFoodData().getFoodLevel(),20,"Expired stomach must clamp immediately");t.succeed();
    }
    @GameTest public void stomachRaisesBothResourcesAndRestoresCaps(GameTestHelper t){
        var p=player(t);effect(p,18);effect(p,17);p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(0);
        for(int i=0;i<4;i++)new ItemStack(Items.COOKED_BEEF).finishUsingItem(t.getLevel(),p);
        eq(t,p.getFoodData().getFoodLevel(),40,"Hunger capacity 40");eq(t,p.getFoodData().getSaturationLevel(),40,"Saturation capacity 40");
        p.getFoodData().setFoodLevel(40);p.getFoodData().setSaturation(40);p.removeEffect(Effects.get(18));
        eq(t,p.getFoodData().getFoodLevel(),20,"Hunger expiry clamps immediately");eq(t,p.getFoodData().getSaturationLevel(),20,"Saturation expiry clamps immediately");t.succeed();
    }
    @GameTest public void lowFoodSprintDamageStacksWithNativeStarvation(GameTestHelper t){
        var low=player(t);low.getFoodData().setFoodLevel(6);low.setSprinting(true);
        for(int i=0;i<80;i++)low.getFoodData().tick(low);eq(t,low.getHealth(),19,"Six food while sprinting: one additional native-rate hit");
        var empty=player(t);empty.getFoodData().setFoodLevel(0);empty.setSprinting(true);
        for(int i=0;i<80;i++)empty.getFoodData().tick(empty);eq(t,empty.getHealth(),18,"Zero food: original plus sprint hit both apply despite hurt cooldown");
        var still=player(t);still.getFoodData().setFoodLevel(0);still.setSprinting(false);
        for(int i=0;i<80;i++)still.getFoodData().tick(still);eq(t,still.getHealth(),19,"Standing at zero retains only native starvation");
        var healthy=player(t);healthy.getFoodData().setFoodLevel(7);healthy.setSprinting(true);
        for(int i=0;i<80;i++)healthy.getFoodData().tick(healthy);eq(t,healthy.getHealth(),20,"Above six has no sprint penalty");t.succeed();
    }
    @GameTest public void lowFoodSprintGateRemovedWithoutChangingFoodReserveHelper(GameTestHelper t){
        var p=player(t);p.getFoodData().setFoodLevel(0);
        try{var m=net.minecraft.world.entity.player.Player.class.getDeclaredMethod("hasEnoughFoodToDoExhaustiveManoeuvres");m.setAccessible(true);t.assertTrue((Boolean)m.invoke(p),"Actual sprint food predicate must allow zero");}
        catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        t.assertTrue(!p.getFoodData().hasEnoughFood(),"Other food-reserve consumers still receive the real hunger state");t.succeed();
    }
    @GameTest public void fractionalCompensationDoesNotRoundCostUp(GameTestHelper t){
        var p=player(t);p.getFoodData().setFoodLevel(5);var account=(FoodAccess)p.getFoodData();
        eq(t,account.baiwei$pay(.25f),.25f,"Fractional payment");eq(t,account.baiwei$available(),4.75f,"Exact remainder");
        eq(t,account.baiwei$pay(100),4.75f,"Limited by real remaining food");eq(t,account.baiwei$available(),0,"No negative resources");t.succeed();
    }
    @GameTest public void vitalityAndMetabolismShareOneHealingLoop(GameTestHelper t){
        var p=player(t);p.setHealth(10);p.getFoodData().setFoodLevel(10);p.getFoodData().setSaturation(0);effect(p,3);effect(p,4);
        for(int i=0;i<80;i++)p.getFoodData().tick(p);
        eq(t,p.getHealth(),12,"Two slow-regeneration ticks, not two simultaneous loops");t.succeed();
    }
    @GameTest public void handAmplifiesInsightAndLifestealExcludesOverkill(GameTestHelper t){
        var p=player(t);p.setHealth(1);effect(p,11);effect(p,12);p.addEffect(new MobEffectInstance(Effects.get(7),1000,49));
        var target=t.spawn(EntityTypes.PIG,new BlockPos(2,2,2));target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);
        target.hurtServer(t.getLevel(),p.damageSources().playerAttack(p),5);
        eq(t,target.getHealth(),0,"(5 + 10) * 10 must kill 100-health unarmored target");eq(t,p.getHealth(),20,"Lifesteal limited by actual loss and player's cap");t.succeed();
    }
    @GameTest public void immovableWinsAgainstMighty(GameTestHelper t){
        var p=player(t);effect(p,16);var target=t.spawn(EntityTypes.PIG,new BlockPos(3,2,3));effect(target,14);var before=target.position();
        target.hurtServer(t.getLevel(),p.damageSources().playerAttack(p),1);
        t.assertTrue(target.position().distanceToSqr(before)<.00001,"Immovable must prevent extra four-block displacement");t.assertTrue(target.getDeltaMovement().horizontalDistanceSqr()<.00001,"Native knockback must also be zero");t.succeed();
    }
    @GameTest public void fastEatingHalvesUseDuration(GameTestHelper t){
        var p=player(t);var stack=new ItemStack(Items.APPLE);int normal=stack.getUseDuration(p);effect(p,15);t.assertValueEqual(stack.getUseDuration(p),(normal+1)/2,"Half native eating time");t.succeed();
    }
    @GameTest public void durabilityLevelOneAlwaysPreventsRealItemWear(GameTestHelper t){
        var p=player(t);effect(p,1);var tool=new ItemStack(Items.DIAMOND_PICKAXE);
        for(int i=0;i<100;i++)tool.hurtAndBreak(10,t.getLevel(),p,s->{});
        t.assertValueEqual(tool.getDamageValue(),0,"Level I always prevents wear");
        p.removeEffect(Effects.get(1));tool.hurtAndBreak(10,t.getLevel(),p,s->{});
        t.assertValueEqual(tool.getDamageValue(),10,"Native wear resumes after expiry");t.succeed();
    }
    @GameTest public void freeTradeStillChecksInputsAndKeepsBothCosts(GameTestHelper t){
        var offer=new MerchantOffer(new ItemCost(Items.WHEAT,5),Optional.of(new ItemCost(Items.CARROT,2)),new ItemStack(Items.EMERALD),10,1,0);
        var a=new ItemStack(Items.WHEAT,5);var b=new ItemStack(Items.CARROT,2);
        TradeContext.begin(true);try{t.assertTrue(offer.take(a,b),"Valid trade succeeds");t.assertValueEqual(a.getCount(),5,"First cost kept");t.assertValueEqual(b.getCount(),2,"Second cost kept");t.assertTrue(!offer.take(ItemStack.EMPTY,b),"Free proc must not bypass input requirements");}finally{TradeContext.end();}
        t.assertTrue(offer.take(a,b)&&a.isEmpty()&&b.isEmpty(),"Normal trade still consumes both costs");t.succeed();
    }
    @GameTest public void forestMaturesAgainAfterHarvestWithoutExtraCooldown(GameTestHelper t){
        var p=player(t);var pos=t.absolutePos(new BlockPos(2,2,2));p.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        t.getLevel().setBlockAndUpdate(pos.below(),Blocks.FARMLAND.defaultBlockState());
        t.getLevel().setBlockAndUpdate(pos,Blocks.WHEAT.defaultBlockState());RuntimeEffects.auraTick(t.getLevel(),p,21);
        t.assertValueEqual(t.getLevel().getBlockState(pos).getValue(CropBlock.AGE),7,"Wheat immediately mature");
        t.getLevel().setBlockAndUpdate(pos,Blocks.WHEAT.defaultBlockState());RuntimeEffects.auraTick(t.getLevel(),p,21);
        t.assertValueEqual(t.getLevel().getBlockState(pos).getValue(CropBlock.AGE),7,"Replanted crop matures without invented cooldown");t.succeed();
    }
    @GameTest public void oneCowBreedsAloneAndRetainsNativeCooldown(GameTestHelper t){
        var p=player(t);var cow=t.spawn(EntityTypes.COW,new BlockPos(2,2,2));p.snapTo(cow.getX(),cow.getY(),cow.getZ(),0,0);cow.setAge(0);
        RuntimeEffects.auraTick(t.getLevel(),p,20);t.assertTrue(cow.getAge()>0,"Parent native cooldown must be set");
        var cows=t.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.cow.Cow.class,cow.getBoundingBox().inflate(2));
        t.assertValueEqual(cows.size(),2,"One parent produces one baby without a mate");
        RuntimeEffects.auraTick(t.getLevel(),p,20);t.assertValueEqual(t.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.cow.Cow.class,cow.getBoundingBox().inflate(2)).size(),2,"Cooldown prevents immediate repeat");t.succeed();
    }
    @GameTest public void breedingHeartsIncludePlayersBossesAndUnbreedableMobs(GameTestHelper t){
        var p=player(t);var pos=t.absolutePos(new BlockPos(2,2,2));p.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);p.tickCount=20;
        var other=player(t);other.snapTo(p.getX()+1,p.getY(),p.getZ(),0,0);
        var boss=t.spawn(EntityTypes.WITHER,new BlockPos(3,2,2));
        var hostile=t.spawn(EntityTypes.ZOMBIE,new BlockPos(2,2,3));
        var calf=t.spawn(EntityTypes.COW,new BlockPos(1,2,2));calf.setAge(-24000);
        var cooling=t.spawn(EntityTypes.COW,new BlockPos(2,2,1));cooling.setAge(6000);
        var far=t.spawn(EntityTypes.PIG,new BlockPos(10,2,2));
        HeartParticleCapture.begin();java.util.List<net.minecraft.world.phys.Vec3> positions;
        try{RuntimeEffects.auraTick(t.getLevel(),p,20);}finally{positions=HeartParticleCapture.end();}
        for(var e:java.util.List.of(p,other,boss,hostile,calf,cooling)){
            var head=new net.minecraft.world.phys.Vec3(e.getX(),e.getY()+e.getBbHeight()+.3,e.getZ());
            t.assertTrue(positions.stream().anyMatch(v->v.distanceToSqr(head)<1e-8),"Missing native heart dispatch over "+e.getType());
        }
        t.assertTrue(positions.stream().noneMatch(v->Math.abs(v.x-far.getX())<.01&&Math.abs(v.z-far.getZ())<.01),"Outside-radius mob must not receive hearts");
        t.assertValueEqual(calf.getAge(),-24000,"Visual effect must not make a calf breed");t.assertValueEqual(cooling.getAge(),6000,"Hearts must not bypass breeding cooldown");t.succeed();
    }
    @GameTest public void absorptionFoodAndHealthLossAreCountedExactlyOnce(GameTestHelper t){
        var target=player(t);target.setHealth(10);target.getFoodData().setFoodLevel(5);
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION,1000,0));target.setAbsorptionAmount(2);eq(t,target.getAbsorptionAmount(),2,"Absorption fixture initialized");effect(target,2);effect(target,6);
        var attacker=t.spawn(EntityTypes.PIG,new BlockPos(2,2,2));attacker.setHealth(1);effect(attacker,12);
        target.hurtServer(t.getLevel(),attacker.damageSources().mobAttack(attacker),100);
        eq(t,target.getHealth(),1,"Death resistance floor");eq(t,target.getFoodData().getFoodLevel(),0,"Five food paid first");
        eq(t,attacker.getHealth(),9f,"50% of actual loss: 2 absorption + 5 food + 9 health; no prevented overkill");t.succeed();
    }
    @GameTest public void eachHealingEffectSpendsHungerBelowNativeThreshold(GameTestHelper t){
        for(int id:new int[]{3,4}){
            var p=player(t);p.setHealth(1);p.getFoodData().setFoodLevel(10);effect(p,id);
            for(int tick=0;tick<160;tick++)p.getFoodData().tick(p);
            eq(t,p.getHealth(),id==3?3:5,"One native healing loop; metabolism is twice as fast");
            t.assertTrue(p.getFoodData().getFoodLevel()<10,"Healing must spend hunger after saturation is empty");
        }t.succeed();
    }
    @GameTest public void healingUsesSaturationThenHungerAndStopsWhenEmpty(GameTestHelper t){
        var p=player(t);p.setHealth(1);p.getFoodData().setFoodLevel(10);p.getFoodData().setSaturation(1);effect(p,4);
        for(int tick=0;tick<41;tick++)p.getFoodData().tick(p);
        eq(t,p.getFoodData().getSaturationLevel(),0,"Saturation spent before hunger");eq(t,p.getFoodData().getFoodLevel(),10,"Hunger remains until saturation is depleted");
        for(int tick=0;tick<80;tick++)p.getFoodData().tick(p);
        t.assertTrue(p.getFoodData().getFoodLevel()<10,"Continued healing spends hunger");
        p.getFoodData().setFoodLevel(0);float health=p.getHealth();for(int tick=0;tick<40;tick++)p.getFoodData().tick(p);
        t.assertTrue(p.getHealth()<=health,"No healing without food resources");t.succeed();
    }
    @GameTest public void lifestealHealsHalfActualDamageAndNothingOnBlockedHit(GameTestHelper t){
        var p=player(t);p.setHealth(1);effect(p,12);var target=t.spawn(EntityTypes.PIG,new BlockPos(2,2,2));
        target.hurtServer(t.getLevel(),p.damageSources().playerAttack(p),4);eq(t,p.getHealth(),3,"Four actual damage heals two health");
        target.hurtServer(t.getLevel(),p.damageSources().playerAttack(p),4);eq(t,p.getHealth(),3,"Invulnerability-blocked hit has no lifesteal");t.succeed();
    }
    @GameTest public void mightyAddsNativeStyleVelocityWithoutTeleporting(GameTestHelper t){
        var p=player(t);var pos=t.absolutePos(new BlockPos(1,2,2));p.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        var target=t.spawn(EntityTypes.PIG,new BlockPos(3,2,2));var before=target.position();
        target.setOnGround(true);target.knockback(.5,-1,0,p.damageSources().playerAttack(p),1);
        var nativeVelocity=target.getDeltaMovement();effect(p,16);RuntimeEffects.mightyKnockback(target,p);
        t.assertTrue(target.position().equals(before),"Mighty must add velocity, never immediately displace the entity");
        t.assertTrue(target.getDeltaMovement().x>nativeVelocity.x,"Mighty adds to existing native knockback");
        t.assertTrue(target.getDeltaMovement().y<=.4,"Grounded lift uses the vanilla vertical cap");
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);RuntimeEffects.mightyKnockback(target,p);
        t.assertTrue(target.getDeltaMovement().horizontalDistanceSqr()>0,"Mighty still affects resistant mobs");t.succeed();
    }
    @GameTest public void waterStrideSneakingSubmergesAndWaterLandingIsSafe(GameTestHelper t){
        var p=player(t);var pos=t.absolutePos(new BlockPos(2,2,2));t.getLevel().setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
        p.snapTo(pos.getX()+.5,pos.getY()+8.0/9,pos.getZ()+.5,0,0);effect(p,8);
        p.setShiftKeyDown(true);var context=net.minecraft.world.phys.shapes.CollisionContext.of(p);
        t.assertTrue(Blocks.WATER.defaultBlockState().getCollisionShape(t.getLevel(),pos,context).isEmpty(),"Shift disables surface collision for diving");
        p.causeFallDamage(20,1,p.damageSources().fall());eq(t,p.getHealth(),20,"Water landing does not cause fall damage");
        t.getLevel().setBlockAndUpdate(pos,Blocks.LAVA.defaultBlockState());p.causeFallDamage(8,1,p.damageSources().fall());
        t.assertTrue(p.getHealth()<20,"Water-stride does not grant lava fall immunity");t.succeed();
    }
    @GameTest(maxTicks=50) public void mightyMovesNaturallyAboutFourBlocksOnFlatGround(GameTestHelper t){
        for(int x=0;x<=12;x++)for(int z=1;z<=3;z++)t.setBlock(new BlockPos(x,1,z),Blocks.STONE);
        var p=player(t);var at=t.absolutePos(new BlockPos(1,2,2));p.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        var target=t.spawn(EntityTypes.PIG,new BlockPos(2,2,2));target.setNoAi(false);target.setOnGround(true);target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        var start=target.position();RuntimeEffects.mightyKnockback(target,p);
        t.runAfterDelay(30,()->{
            double distance=target.position().subtract(start).horizontalDistance();
            BaiweiMod.LOG.info("BAIWEI_MIGHTY_FLAT_GROUND: {} blocks after 30 ticks",distance);
            t.assertTrue(distance>=3&&distance<=5,"Native physics should carry a flat-ground target about four blocks, got "+distance);t.succeed();
        });
    }
    @GameTest public void featherEffectPreventsFallEvenBeforeFlightTick(GameTestHelper t){
        var p=player(t);effect(p,13);p.causeFallDamage(30,1,p.damageSources().fall());eq(t,p.getHealth(),20,"Feather fall protection independent of flight toggle");
        p.removeEffect(Effects.get(13));p.causeFallDamage(8,1,p.damageSources().fall());t.assertTrue(p.getHealth()<20,"Normal fall damage returns after effect ends");t.succeed();
    }
    @GameTest public void waterStrideAddsCollisionForWaterAndLava(GameTestHelper t){
        var p=player(t);var pos=t.absolutePos(new BlockPos(2,2,2));p.snapTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);
        var context=net.minecraft.world.phys.shapes.CollisionContext.of(p);
        var water=Blocks.WATER.defaultBlockState();var lava=Blocks.LAVA.defaultBlockState();
        t.assertTrue(water.getCollisionShape(t.getLevel(),pos,context).isEmpty(),"Ordinary water remains intangible");effect(p,8);
        t.assertTrue(!water.getCollisionShape(t.getLevel(),pos,context).isEmpty(),"Water surface solid for effect bearer");
        t.assertTrue(!lava.getCollisionShape(t.getLevel(),pos,context).isEmpty(),"Lava surface also solid");t.succeed();
    }
    @GameTest public void immovableCancelsExplosionStyleImpulse(GameTestHelper t){
        var p=player(t);effect(p,14);var before=p.getDeltaMovement();p.push(1,2,3);t.assertTrue(p.getDeltaMovement().equals(before),"Forced impulse must be blocked");t.succeed();
    }
    @GameTest public void hoverRestoresOriginalAbilitiesAfterExpiry(GameTestHelper t){
        var p=player(t);t.assertTrue(!p.getAbilities().mayfly,"Survival starts without flight");effect(p,13);RuntimeEffects.playerTick(p);
        t.assertTrue(p.getAbilities().mayfly&&p.getAbilities().flying,"Hover enables native three-dimensional flight");
        p.removeEffect(Effects.get(13));RuntimeEffects.playerTick(p);t.assertTrue(!p.getAbilities().mayfly&&!p.getAbilities().flying,"Survival flight restored: mayfly="+p.getAbilities().mayfly+" flying="+p.getAbilities().flying+" creative="+p.isCreative()+" spectator="+p.isSpectator()+" effect="+Effects.has(p,13));t.succeed();
    }
    @GameTest public void hoverRestoresPermissionAfterNativeSaveAndLoad(GameTestHelper t){
        var first=player(t);effect(first,13);RuntimeEffects.playerTick(first);
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,t.getLevel().registryAccess());
        first.saveWithoutId(output);var tag=output.buildResult();
        var loaded=player(t);loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,t.getLevel().registryAccess(),tag));
        RuntimeEffects.playerTick(loaded);t.assertTrue(loaded.getAbilities().mayfly&&loaded.hasEffect(Effects.get(13)),"Native save reload keeps active hover");
        loaded.removeEffect(Effects.get(13));
        t.assertTrue(!loaded.getAbilities().mayfly&&!loaded.getAbilities().flying,"Native reload restores original survival permission immediately");
        var external=player(t);external.getAbilities().mayfly=true;effect(external,13);RuntimeEffects.playerTick(external);external.removeEffect(Effects.get(13));
        t.assertTrue(external.getAbilities().mayfly&&!external.getAbilities().flying,"Previously granted external flight permission preserved");
        var old=player(t);effect(old,13);RuntimeEffects.playerTick(old);var respawn=player(t);respawn.getAbilities().mayfly=true;
        RuntimeEffects.copyHover(old,respawn);t.assertTrue(!respawn.getAbilities().mayfly,"Player replacement restores permission when effect did not carry over");t.succeed();
    }
    @GameTest public void nonHostilityIsRestrictedToSpecifiedDimension(GameTestHelper t){
        var p=player(t);effect(p,9);effect(p,10);t.assertTrue(!RuntimeEffects.dimensionPeace(p),"Neither sovereign effect grants overworld non-hostility");
        var nether=t.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        var end=t.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        var n=EntityTypes.ZOMBIE.create(nether,net.minecraft.world.entity.EntitySpawnReason.COMMAND);var e=EntityTypes.ENDERMAN.create(end,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        effect(n,9);effect(e,10);t.assertTrue(RuntimeEffects.dimensionPeace(n)&&RuntimeEffects.dimensionPeace(e),"Matching dimensions protected");
        var hostile=EntityTypes.ZOMBIE.create(nether,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        t.assertTrue(!hostile.canAttack(n),"Native mob targeting rejects protected entity");hostile.setTarget(n);t.assertTrue(hostile.getTarget()==null,"Target assignment rejected");t.succeed();
    }
    @GameTest public void fieldSaladUsesHasteAndRetainsOtherNativeFunctions(GameTestHelper t){
        if(!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("delightful")){t.succeed();return;}
        var p=player(t);food("delightful:field_salad").finishUsingItem(t.getLevel(),p);
        t.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.HASTE),"Corrected haste applies");
        t.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.CONDUIT_POWER),"Berry must not be mistaken for seafood");
        var n=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("farmersdelight:nourishment")).orElseThrow();
        t.assertTrue(!p.hasEffect(n)&&p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Only nourishment removed; native regeneration retained");t.succeed();
    }
    @GameTest public void everyAvailableMappedFoodCompletesNativeConsumption(GameTestHelper t){
        var p=player(t);int eaten=0,nourishmentRemoved=0,nourishmentRetained=0;
        try(var in=BaiweiGameTests.class.getResourceAsStream("/data/baiweiqixiao/food_effects.json")){
            var data=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("foods");
            for(var entry:data.entrySet()){
                var key=Identifier.parse(entry.getKey());var found=BuiltInRegistries.ITEM.getOptional(key);if(found.isEmpty())continue;
                var stack=new ItemStack(found.get());if(!stack.has(DataComponents.CONSUMABLE))continue;
                p.removeAllEffects();p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(0);p.setHealth(20);
                stack.finishUsingItem(t.getLevel(),p);eaten++;
                for(var element:entry.getValue().getAsJsonObject().getAsJsonArray("added")){
                    var r=element.getAsJsonObject();var holder=BuiltInRegistries.MOB_EFFECT.get(Effects.id(r.get("effect").getAsString())).orElseThrow();
                    if(r.get("probability").getAsDouble()<1)continue;
                    if(holder.value().isInstantaneous())t.assertTrue(!p.hasEffect(holder),key+" instant effect must execute without a lingering status");
                    else{
                        var applied=p.getEffect(holder);t.assertTrue(applied!=null,key+" missing effect "+r.get("effect"));
                        t.assertValueEqual(applied.getAmplifier()+1,r.get("level").getAsInt(),key+" effect level");
                        t.assertValueEqual(applied.getDuration(),r.get("ticks").getAsInt(),key+" independent duration");
                    }
                }
                var nativeRules=entry.getValue().getAsJsonObject().getAsJsonObject("native");
                if(nativeRules!=null&&nativeRules.has("farmersdelight:nourishment")){
                    var rule=nativeRules.getAsJsonObject("farmersdelight:nourishment");
                    var holder=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("farmersdelight:nourishment")).orElseThrow();
                    if(rule.get("remove").getAsBoolean()){
                        t.assertTrue(!p.hasEffect(holder),key+" must not grant removed nourishment during actual consumption");nourishmentRemoved++;
                    }else{
                        var applied=p.getEffect(holder);t.assertTrue(applied!=null,key+" missing retained nourishment");
                        if(rule.has("ticks")&&!rule.get("ticks").isJsonNull())t.assertValueEqual(applied.getDuration(),rule.get("ticks").getAsInt(),key+" nourishment duration");
                        if(rule.has("level")&&!rule.get("level").isJsonNull())t.assertValueEqual(applied.getAmplifier()+1,rule.get("level").getAsInt(),key+" nourishment level");
                        nourishmentRetained++;
                    }
                }
            }
        }catch(Exception ex){throw new RuntimeException(ex);}
        t.assertTrue(eaten>50,"Food smoke-test must actually consume registered items");
        BaiweiMod.LOG.info("GameTest consumed {} registered mapped foods",eaten);
        BaiweiMod.LOG.info("BAIWEI_NOURISHMENT_CONSUMPTION_VERIFIED: {} removals and {} retained adjustments",nourishmentRemoved,nourishmentRetained);t.succeed();
    }
    @GameTest public void turtleKeepsEggPregnancyInsteadOfSpawningBaby(GameTestHelper t){
        var p=player(t);var turtle=t.spawn(EntityTypes.TURTLE,new BlockPos(2,2,2));turtle.setAge(0);turtle.setHomePos(turtle.blockPosition());p.snapTo(turtle.getX(),turtle.getY(),turtle.getZ(),0,0);
        RuntimeEffects.auraTick(t.getLevel(),p,20);t.assertTrue(turtle.hasEgg()&&turtle.getAge()>0,"Turtle pregnant with native cooldown");
        t.assertValueEqual(t.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.turtle.Turtle.class,turtle.getBoundingBox().inflate(2)).size(),1,"No instant baby turtle");t.succeed();
    }
    @GameTest public void frogKeepsNativePregnancyAndSpawningAi(GameTestHelper t){
        var p=player(t);var frog=t.spawn(EntityTypes.FROG,new BlockPos(2,2,2));frog.setAge(0);p.snapTo(frog.getX(),frog.getY(),frog.getZ(),0,0);
        RuntimeEffects.auraTick(t.getLevel(),p,20);
        t.assertTrue(frog.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.IS_PREGNANT),"Frog enters native pregnancy memory");
        t.assertTrue(frog.getAge()>0,"Native frog cooldown");t.succeed();
    }
    @GameTest public void snifferDropsNativeEggInsteadOfInstantBaby(GameTestHelper t){
        var p=player(t);var sniffer=t.spawn(EntityTypes.SNIFFER,new BlockPos(2,2,2));sniffer.setAge(0);p.snapTo(sniffer.getX(),sniffer.getY(),sniffer.getZ(),0,0);
        RuntimeEffects.auraTick(t.getLevel(),p,20);
        t.assertTrue(sniffer.getAge()>0,"Native sniffer cooldown");
        var eggs=t.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,sniffer.getBoundingBox().inflate(2),i->i.getItem().is(Items.SNIFFER_EGG));
        t.assertValueEqual(eggs.size(),1,"One native sniffer egg");t.succeed();
    }
}
