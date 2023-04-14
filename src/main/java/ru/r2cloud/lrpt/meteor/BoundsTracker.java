package ru.r2cloud.lrpt.meteor;

public class BoundsTracker {

	private static final double UNDEFINED = -360;

	private double right = -360.0f;
	private double left = 360.0f;
	private boolean wrapDetected = false;
	private double top = -90.0f;
	private double bottom = 90.0f;

	private double previousX = UNDEFINED;

	public void update(double x, double y) {
		if (y > top) {
			top = y;
		}
		if (y < bottom) {
			bottom = y;
		}
		if (!wrapDetected && previousX != UNDEFINED) {
			if (x < 0 && previousX > 170.0) {
				// going north
				wrapDetected = true;
				right = -180.0;
			}
			if (x > 170.0 && previousX < 0) {
				wrapDetected = true;
				left = 180;
			}
		}
		if (wrapDetected) {
			if (x < 0) {
				right = Math.max(x, right);
			}
			if (x > 0) {
				left = Math.min(x, left);
			}
		} else {
			left = Math.min(x, left);
			right = Math.max(x, right);
		}
		previousX = x;
	}

	public void resetLine() {
		this.previousX = UNDEFINED;
	}

	public double getRight() {
		return right;
	}

	public double getLeft() {
		return left;
	}

	public double getTop() {
		return top;
	}

	public double getBottom() {
		return bottom;
	}

	public String formatRough() {
		StringBuilder result = new StringBuilder();
		result.append((int) Math.floor(left)).append("_").append((int) Math.floor(bottom)).append("_").append((int) Math.ceil(right)).append("_").append((int) Math.ceil(top));
		return result.toString();
	}
}
