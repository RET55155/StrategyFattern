package ratelimiter.strategy;


public interface RateLimitStrategy {

    boolean allowRequest(String clientId);

    String describe();
}
