package ratelimiter.strategy;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;


public final class FixedWindowStrategy implements RateLimitStrategy {

    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, WindowCounter> countersByClient = new HashMap<>();

    public FixedWindowStrategy(int maxRequests, long windowMillis, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    @Override
    public boolean allowRequest(String clientId) {
        long currentWindow = clock.millis() / windowMillis;
        WindowCounter counter = countersByClient.computeIfAbsent(clientId, id -> new WindowCounter(currentWindow));

        counter.startNewWindowIfNeeded(currentWindow);
        return counter.tryIncrement(maxRequests);
    }

    @Override
    public String describe() {
        return "Fixed Window (" + maxRequests + " req / " + windowMillis + " ms)";
    }

    private static final class WindowCounter {
        private long windowId;
        private int count;

        WindowCounter(long windowId) {
            this.windowId = windowId;
        }

        void startNewWindowIfNeeded(long currentWindow) {
            if (currentWindow != windowId) {
                windowId = currentWindow;
                count = 0;
            }
        }

        boolean tryIncrement(int limit) {
            if (count >= limit) {
                return false;
            }
            count++;
            return true;
        }
    }
}
