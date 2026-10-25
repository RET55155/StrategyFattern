package ratelimiter.strategy;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;


public final class SlidingWindowLogStrategy implements RateLimitStrategy {

    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Deque<Long>> timestampsByClient = new HashMap<>();

    public SlidingWindowLogStrategy(int maxRequests, long windowMillis, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    @Override
    public boolean allowRequest(String clientId) {
        long now = clock.millis();
        Deque<Long> timestamps = timestampsByClient.computeIfAbsent(clientId, id -> new ArrayDeque<>());

        removeExpired(timestamps, now);
        return tryRegister(timestamps, now);
    }

    @Override
    public String describe() {
        return "Sliding Window Log (" + maxRequests + " req / " + windowMillis + " ms)";
    }

    private void removeExpired(Deque<Long> timestamps, long now) {
        long oldestAllowed = now - windowMillis;
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= oldestAllowed) {
            timestamps.pollFirst();
        }
    }

    private boolean tryRegister(Deque<Long> timestamps, long now) {
        if (timestamps.size() >= maxRequests) {
            return false;
        }
        timestamps.addLast(now);
        return true;
    }
}
