package com.andova.app;

/**
 * Created by Administrator on 2018-03-08.
 *
 * @author kzaxil
 * @since 1.0.0
 */
class Sample {
    /**
     * 测试赋值引用
     */
    void testAssign() {
        Demo demoA = new Demo();
        demoA.a = "demoA_a";
        demoA.b = "demoA_b";
        Demo demoB = new Demo();
        demoB.a = "demoB_a";
        demoB.b = "demoB_b";
        System.out.println("demoA:" + demoA + ",demoB:" + demoB);
        demoB = demoA;
        demoA = null;
        System.out.println("demoA:" + demoA + ",demoB:" + demoB);
        System.out.println("==================================");
        Demo demoC = new Demo();
        demoC.a = "demoC_a";
        demoC.b = "demoC_b";
        Demo demoD = demoC;
        System.out.println("demoC:" + demoC + ",demoD:" + demoD);
        demoC = null;
        System.out.println("demoC:" + demoC + ",demoD:" + demoD);
    }

    public static class Demo {
        String a;
        String b;
    }
}
