package ratelimiter.strategy;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;


public final class TokenBucketStrategy implements RateLimitStrategy {

    private final int capacity;
    private final double refillTokensPerSecond;
    private final Clock clock;
    private final Map<String, Bucket> bucketsByClient = new HashMap<>();

    public TokenBucketStrategy(int capacity, double refillTokensPerSecond, Clock clock) {
        this.capacity = capacity;
        this.refillTokensPerSecond = refillTokensPerSecond;
        this.clock = clock;
    }

    @Override
    public boolean allowRequest(String clientId) {
        long now = clock.millis();
        Bucket bucket = bucketsByClient.computeIfAbsent(clientId, id -> new Bucket(capacity, now));

        bucket.refill(now, refillTokensPerSecond, capacity);
        return bucket.tryTakeToken();
    }

    @Override
    public String describe() {
        return "Token Bucket (capacity " + capacity + ", refill " + refillTokensPerSecond + " tokens/s)";
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefillMillis;

        Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.lastRefillMillis = now;
        }

        void refill(long now, double tokensPerSecond, int capacity) {
            double elapsedSeconds = (now - lastRefillMillis) / 1000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * tokensPerSecond);
            lastRefillMillis = now;
        }

        boolean tryTakeToken() {
            if (tokens < 1.0) {
                return false;
            }
            tokens -= 1.0;
            return true;
        }
    }
}
