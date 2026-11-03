package dev.baiwei;
/** Pure settlement arithmetic. Values are the user's fixed mechanisms, not food balance defaults. */
public final class Mechanics {
    public static float attack(float base,float previousHealth,boolean insight,boolean hand){return (base+(insight?previousHealth*.10f:0))*(hand?10:1);}
    public static float actualLoss(float healthBefore,float healthAfter,float absorptionBefore,float absorptionAfter,float hungerPaid){return Math.max(0,healthBefore-healthAfter)+Math.max(0,absorptionBefore-absorptionAfter)+Math.max(0,hungerPaid);}
    public static float floorHealth(float proposed,boolean deathResistance){return deathResistance?Math.max(1,proposed):proposed;}
    public static double chance(double perLevel,int level){return Math.min(1,Math.max(0,perLevel*level));}
}
