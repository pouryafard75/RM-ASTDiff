package com.sap.olingo.jpa.processor.core.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JPAClaimsPairTest {

  @Test
  public void checkCreateIntegerPairOnlyMin() {
    final JPAClaimsPair<Integer> cut = new JPAClaimsPair<>(7);
    assertEquals((Integer) 7, cut.min);
  }

  @Test
  public void checkCreateIntegerPairNoUpperBoundry() {
    final JPAClaimsPair<Integer> cut = new JPAClaimsPair<>(7);
    assertFalse(cut.hasUpperBoundary);
  }

  @Test
  public void checkCreateIntegerPair() {
    final JPAClaimsPair<Integer> cut = new JPAClaimsPair<>(7, 10);
    assertEquals((Integer) 7, cut.min);
    assertEquals((Integer) 10, cut.max);
  }

  @Test
  public void checkCreateIntegerPairUpperBoundry() {
    final JPAClaimsPair<Integer> cut = new JPAClaimsPair<>(7, 10);
    assertTrue(cut.hasUpperBoundary);
  }

}
