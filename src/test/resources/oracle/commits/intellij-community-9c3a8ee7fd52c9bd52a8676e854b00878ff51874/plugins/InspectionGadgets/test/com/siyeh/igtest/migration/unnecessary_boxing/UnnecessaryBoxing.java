package com.siyeh.igtest.migration.unnecessary_boxing;




public class UnnecessaryBoxing {

    Integer foo(String foo, Integer bar) {
        return foo == null ? Integer.valueOf(0) : bar;
    }

    public static void main(String[] args)
    {
        final Integer intValue = <warning descr="Unnecessary boxing 'new Integer(3)'">new Integer(3)</warning>;
        final Long longValue = <warning descr="Unnecessary boxing 'new Long(3L)'">new Long(3L)</warning>;
        final Long longValue2 = <warning descr="Unnecessary boxing 'new Long(3)'">new Long(3)</warning>;
        final Short shortValue = <warning descr="Unnecessary boxing 'new Short((short)3)'">new Short((short)3)</warning>;
        final Double doubleValue = <warning descr="Unnecessary boxing 'new Double(3.0)'">new Double(3.0)</warning>;
        final Float floatValue = <warning descr="Unnecessary boxing 'new Float(3.0F)'">new Float(3.0F)</warning>;
        final Byte byteValue = <warning descr="Unnecessary boxing 'new Byte((byte)3)'">new Byte((byte)3)</warning>;
        final Boolean booleanValue = <warning descr="Unnecessary boxing 'new Boolean(true)'">new Boolean(true)</warning>;
        final Character character = <warning descr="Unnecessary boxing 'new Character('c')'">new Character('c')</warning>;
    }

    Integer foo2(String foo, int bar) {
        return foo == null ? <warning descr="Unnecessary boxing 'Integer.valueOf(0)'">Integer.valueOf(0)</warning> : bar;
    }

    void noUnboxing(Object val) {
        if (val == Integer.valueOf(0)) {

        } else if (Integer.valueOf(1) == val) {}
        boolean b = true;
        Boolean.valueOf(b).toString();
    }

    public Integer getBar() {
        return null;
    }

    void doItNow(UnnecessaryBoxing foo) {
        Integer bla = foo == null ? Integer.valueOf(0) : foo.getBar();
    }

    private int i;

    private String s;

    public <T>T get(Class<T> type) {
        if (type == Integer.class) {
            return (T) new Integer(i);
        } else if (type == String.class) {
            return (T) s;
        }
        return null;
    }
}
class IntIntegerTest {
  public IntIntegerTest(Integer val) {
    System.out.println("behavoiur 1");
  }

  public IntIntegerTest(int val) {
    System.out.println("behavoiur 2");
  }

  public static void f(Integer val) {
    System.out.println("behavoiur 1");
  }

  public static void f(int val) {
    System.out.println("behavoiur 2");
  }

  public IntIntegerTest() {
  }

  public void test() {
    new IntIntegerTest(new Integer(1)); // <-- incorrectly triggered
    f(new Integer(1)); // <-- not triggered
  }
}