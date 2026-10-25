package ratelimiter.strategy;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;


public final class LeakyBucketStrategy implements RateLimitStrategy {

    private final int capacity;
    private final double leakPerSecond;
    private final Clock clock;
    private final Map<String, Bucket> bucketsByClient = new HashMap<>();

    public LeakyBucketStrategy(int capacity, double leakPerSecond, Clock clock) {
        this.capacity = capacity;
        this.leakPerSecond = leakPerSecond;
        this.clock = clock;
    }

    @Override
    public boolean allowRequest(String clientId) {
        long now = clock.millis();
        Bucket bucket = bucketsByClient.computeIfAbsent(clientId, id -> new Bucket(now));

        bucket.leak(now, leakPerSecond);
        return bucket.tryAddRequest(capacity);
    }

    @Override
    public String describe() {
        return "Leaky Bucket (capacity " + capacity + ", leak " + leakPerSecond + " req/s)";
    }

    private static final class Bucket {
        private double level;
        private long lastLeakMillis;

        Bucket(long now) {
            this.lastLeakMillis = now;
        }

        void leak(long now, double perSecond) {
            double elapsedSeconds = (now - lastLeakMillis) / 1000.0;
            level = Math.max(0.0, level - elapsedSeconds * perSecond);
            lastLeakMillis = now;
        }

        boolean tryAddRequest(int capacity) {
            if (level + 1.0 > capacity) {
                return false;
            }
            level += 1.0;
            return true;
        }
    }
}
