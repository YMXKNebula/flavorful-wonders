package dev.baiwei;
public final class MechanismTest {
    private static void eq(double actual,double expected){if(Math.abs(actual-expected)>1e-5)throw new AssertionError(actual+" != "+expected);}
    public static void main(String[] args){
        eq(Mechanics.attack(5,100,true,true),150);
        eq(Mechanics.attack(5,100,true,false),15);
        eq(Mechanics.actualLoss(100,1,20,0,5),124);
        eq(Mechanics.actualLoss(1,1,0,0,0),0);
        eq(Mechanics.floorHealth(-100,true),1);
        eq(Mechanics.chance(.20,5),1);
        eq(Mechanics.chance(.20,6),1);
        eq(Mechanics.chance(.02,3),.06);
        TradeContext.begin(true);if(!TradeContext.free())throw new AssertionError();TradeContext.begin(false);if(TradeContext.free())throw new AssertionError();TradeContext.end();if(!TradeContext.free())throw new AssertionError();TradeContext.end();
        System.out.println("9 settlement and nested trade-context checks passed.");
    }
}
