# Rate Limiter — Strategy Pattern (Assignment 5)

Course: Software Design Patterns, AITU. Language: Java 17+.

## Idea
An API must protect itself from clients that send too many requests.
There are several interchangeable algorithms for that, so the choice of algorithm
is encapsulated with the **Strategy** pattern and can be switched at runtime.

## Pattern roles
| Role | Class | Responsibility |
|---|---|---|
| Strategy interface | `RateLimitStrategy` | `allowRequest(clientId)` + `describe()` |
| Concrete strategy | `FixedWindowStrategy` | counter per aligned time window |
| Concrete strategy | `SlidingWindowLogStrategy` | timestamps of requests in the last N ms |
| Concrete strategy | `TokenBucketStrategy` | refilling bucket of tokens, allows bursts |
| Concrete strategy | `LeakyBucketStrategy` | bucket that leaks at a constant rate |
| Context | `RateLimiter` | holds a strategy, delegates, `setStrategy()` at runtime |
| Client | `demo.Main` | builds and configures strategies, plugs them into the context |

## Clean Code principles applied
1. **Polymorphism instead of conditionals.** `RateLimiter` has no `if/switch` on the algorithm type; the client picks the strategy, the context just delegates. (Clean Code, Ch. 10 / G23 "Prefer Polymorphism to If/Else or Switch/Case").
2. **Intention-revealing names.** `FixedWindowStrategy`, `TokenBucketStrategy`, `removeExpired`, `tryTakeToken`, `startNewWindowIfNeeded` say what they do without comments. (N1, Meaningful Names).
3. **Single Responsibility.** Each strategy implements exactly one algorithm; the context only delegates; `MutableClock` only provides time; `Main` only wires things together.
4. **Context is algorithm-agnostic.** `RateLimiter` depends only on the `RateLimitStrategy` abstraction (Dependency Inversion), it knows nothing about windows, buckets or timestamps.
5. **Open/Closed — easy extensibility.** A new algorithm (e.g. `SlidingWindowCounterStrategy`) is added as a new class + one line in `Main`; no existing class is modified.
6. **Small functions, one level of abstraction.** `allowRequest` reads like a short story: get state -> update state -> decide. Details are hidden in private helpers and small nested state classes.
7. **Dependency injection for testability.** Strategies receive a `Clock`, so time can be controlled (`MutableClock`) and the demo is deterministic without `Thread.sleep`.
8. **Immutability / encapsulation.** Configuration fields are `final`, per-client state is private inside nested classes, concrete strategies are `final`.
