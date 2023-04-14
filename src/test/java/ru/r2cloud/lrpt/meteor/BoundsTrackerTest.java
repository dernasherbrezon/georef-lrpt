package ru.r2cloud.lrpt.meteor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BoundsTrackerTest {

	@Test
	public void testSouthToNorthPass() {
		BoundsTracker tracker = new BoundsTracker();
		tracker.update(149.52915755044418, -45.837704600923644);
		tracker.update(174.50890318780168, -44.100997541514076);
		tracker.update(-177.61654129184285, -42.3200985765115);
		tracker.update(-173.72678903385165, -41.191670087805825);
		tracker.resetLine();

		tracker.update(146.15634591446144, -9.14849576673683);
		tracker.update(172.38199008351287, -5.37249789591718);

		assertEquals(146.15634591446144, tracker.getLeft(), 0.0);
		assertEquals(-173.72678903385165, tracker.getRight(), 0.0);
		assertEquals(-5.37249789591718, tracker.getTop(), 0.0);
		assertEquals(-45.837704600923644, tracker.getBottom(), 0.0);

		assertEquals("146_-46_-173_-5", tracker.formatRough());
	}
	
	@Test
	public void testNorthToSouthPass() {
		BoundsTracker tracker = new BoundsTracker();
		tracker.update(-173.72678903385165, -41.191670087805825);
		tracker.update(-177.61654129184285, -42.3200985765115);
		tracker.update(174.50890318780168, -44.100997541514076);
		tracker.update(149.52915755044418, -45.837704600923644);
		tracker.resetLine();

		tracker.update(172.38199008351287, -5.37249789591718);
		tracker.update(146.15634591446144, -9.14849576673683);

		assertEquals(146.15634591446144, tracker.getLeft(), 0.0);
		assertEquals(-173.72678903385165, tracker.getRight(), 0.0);
		assertEquals(-5.37249789591718, tracker.getTop(), 0.0);
		assertEquals(-45.837704600923644, tracker.getBottom(), 0.0);

		assertEquals("146_-46_-173_-5", tracker.formatRough());
	}
	
	@Test
	public void testNorthToSouthPassNoWrap() {
		BoundsTracker tracker = new BoundsTracker();
		tracker.update(13.79074130256729, 83.60930411105197);
		tracker.update(-4.002470525197459, 82.95524070550265);
		tracker.resetLine();

		tracker.update(12.491244810706508, 24.422978386679777);
		tracker.update(10.139439398947587, 24.59338400634908);

		assertEquals(-4.002470525197459, tracker.getLeft(), 0.0);
		assertEquals(13.79074130256729, tracker.getRight(), 0.0);
		assertEquals(83.60930411105197, tracker.getTop(), 0.0);
		assertEquals(24.422978386679777, tracker.getBottom(), 0.0);

		assertEquals("-5_24_14_84", tracker.formatRough());
	}

}
