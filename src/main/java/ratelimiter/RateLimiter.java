package ratelimiter;

import ratelimiter.strategy.RateLimitStrategy;

import java.util.Objects;

public final class RateLimiter {

    private RateLimitStrategy strategy;

    public RateLimiter(RateLimitStrategy strategy) {
        setStrategy(strategy);
    }

    public void setStrategy(RateLimitStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
    }

    public boolean tryAcquire(String clientId) {
        return strategy.allowRequest(clientId);
    }

    public String currentStrategyName() {
        return strategy.describe();
    }
}
