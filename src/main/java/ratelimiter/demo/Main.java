package ratelimiter.demo;

import ratelimiter.RateLimiter;
import ratelimiter.strategy.FixedWindowStrategy;
import ratelimiter.strategy.LeakyBucketStrategy;
import ratelimiter.strategy.RateLimitStrategy;
import ratelimiter.strategy.SlidingWindowLogStrategy;
import ratelimiter.strategy.TokenBucketStrategy;

import java.time.Clock;
import java.util.List;
import java.util.function.Function;

public final class Main {

    private static final String CLIENT_ID = "user-42";
    private static final int LIMIT = 5;
    private static final long WINDOW_MS = 10_000;

    private static final List<Function<Clock, RateLimitStrategy>> STRATEGY_FACTORIES = List.of(
            clock -> new FixedWindowStrategy(LIMIT, WINDOW_MS, clock),
            clock -> new SlidingWindowLogStrategy(LIMIT, WINDOW_MS, clock),
            clock -> new TokenBucketStrategy(LIMIT, 0.5, clock),
            clock -> new LeakyBucketStrategy(LIMIT, 0.5, clock)
    );

    public static void main(String[] args) {
        System.out.println("=== Scenario 1: 7 requests at once | +2s: 2 requests | +8s: 3 requests ===");
        runScenario(Main::simpleBurstScenario);

        System.out.println();
        System.out.println("=== Scenario 2: 5 requests at t=9s | 5 requests at t=10s (window boundary) ===");
        runScenario(Main::boundaryScenario);
    }

    private static void runScenario(Scenario scenario) {
        RateLimiter rateLimiter = null;

        for (Function<Clock, RateLimitStrategy> factory : STRATEGY_FACTORIES) {
            MutableClock clock = new MutableClock();
            RateLimitStrategy strategy = factory.apply(clock);

            rateLimiter = (rateLimiter == null) ? new RateLimiter(strategy) : switchStrategy(rateLimiter, strategy);

            String result = scenario.play(rateLimiter, clock);
            System.out.printf("%-55s %s%n", rateLimiter.currentStrategyName(), result);
        }
    }

    private static RateLimiter switchStrategy(RateLimiter rateLimiter, RateLimitStrategy strategy) {
        rateLimiter.setStrategy(strategy);
        return rateLimiter;
    }

    private static String simpleBurstScenario(RateLimiter limiter, MutableClock clock) {
        StringBuilder log = new StringBuilder();
        sendRequests(limiter, 7, log);
        clock.advanceMillis(2_000);
        log.append("| ");
        sendRequests(limiter, 2, log);
        clock.advanceMillis(8_000);
        log.append("| ");
        sendRequests(limiter, 3, log);
        return log.toString();
    }

    private static String boundaryScenario(RateLimiter limiter, MutableClock clock) {
        StringBuilder log = new StringBuilder();
        clock.advanceMillis(9_000);
        sendRequests(limiter, 5, log);
        clock.advanceMillis(1_000);
        log.append("| ");
        sendRequests(limiter, 5, log);
        return log.toString();
    }

    private static void sendRequests(RateLimiter limiter, int count, StringBuilder log) {
        for (int i = 0; i < count; i++) {
            log.append(limiter.tryAcquire(CLIENT_ID) ? "OK " : "-- ");
        }
    }

    @FunctionalInterface
    private interface Scenario {
        String play(RateLimiter limiter, MutableClock clock);
    }
}
