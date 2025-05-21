package io.hhplus.tdd.point;

public class PointPolicy {

    private PointPolicy() {
        throw new IllegalStateException("Utility class");
    }

    public static final Long MAX_POINT = 100_000_000L;
}
