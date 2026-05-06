# Flux Limiter
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
![Redis](https://img.shields.io/badge/Redis-7-red)
![License](https://img.shields.io/badge/License-MIT-blue)

A distributed, multi-tenant rate limiter service backed by Redis. Three algorithms behind a unified interface, atomic operations via Lua scripts, horizontally scalable behind nginx.

## Why I built this

I wanted a project that demonstrates actual backend depth instead of another CRUD app. Rate limiters sit in front of every real API and have a few interesting design problems: how do you keep a counter consistent across multiple service instances, how do you handle race conditions without bottlenecking on a Java lock, and what algorithm tradeoffs actually matter in production.

This was also an excuse to use Lua inside Redis, which I'd never done before.

## Features

- 3 algorithms: token bucket, sliding window log, fixed window counter
- Per-tenant configuration (each API key gets its own algorithm and limits)
- Distributed by default; all state in Redis, atomic via Lua
- Admin REST API for runtime config updates (no service restart)
- Sub-5ms p99 latency under light load, 6,000+ req/sec sustained throughput
- Dockerized, runs with `docker compose up`
- k6 benchmark suite included
- JUnit + Testcontainers integration tests

## Architecture

```
Client → nginx (round-robin)  → app1 ┐
                              → app2 ├──→ Redis (Lua scripts)
                              → app3 ┘
```

One Spring Boot service replicated 3 times behind nginx. All instances are stateless every rate limit decision goes through Redis, which is the only stateful component. Lua scripts ensure read-modify-write happens atomically per request, so race conditions across instances are impossible.

The strategy pattern picks the right algorithm at runtime based on the tenant's config:

```
Request → Controller → TenantConfigService (read config from Redis)
                    → RateLimiterFactory (pick TOKEN_BUCKET / SLIDING_WINDOW / FIXED_WINDOW)
                    → RateLimiter.check() → executes Lua against Redis → returns response
```

## Algorithm comparison

| Algorithm | Memory | Accuracy | Allows bursts? | Best for |
|-----------|--------|----------|----------------|----------|
| Token bucket | O(1) | Good | Yes (controlled) | General APIs |
| Fixed window | O(1) | Burst-at-boundary issue | At boundary | Simple internal services |
| Sliding window log | O(N) per window | Perfect | No | High-value, low-volume APIs |

If you're building a real rate limiter and don't have a specific reason otherwise, token bucket is what you want. AWS API Gateway uses it. Sliding window log is more accurate but stores every request timestamp, which gets expensive at scale.

## Quick start

You need Docker Desktop running. Then:

```bash
git clone https://github.com/sirhashir/flux-limiter
cd flux-limiter
docker compose up -d
```

That's it. nginx is on `localhost:8080`, three Spring Boot instances behind it, Redis 7 underneath.

Create a tenant:

```bash
curl -X POST http://localhost:8080/admin/tenants \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"my-app","algorithm":"TOKEN_BUCKET","limit":100,"windowSeconds":60}'
```

Check a request against the limit:

```bash
curl -X POST http://localhost:8080/api/check \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"my-app","key":"endpoint-1"}'
```

Returns:

```json
{"allowed":true,"remaining":99,"resetAt":1713352800}
```

Hit it 100 times and watch `remaining` count down to 0, then `allowed` flips to false.

## API

### Data plane

`POST /api/check` : check if a request is allowed

```json
Request:  {"tenantId": "my-app", "key": "endpoint-1"}
Response: {"allowed": true, "remaining": 99, "resetAt": 1713352800}
```

### Admin

- `POST /admin/tenants` : create a tenant config
- `GET /admin/tenants/{id}` : read a tenant config
- `PUT /admin/tenants/{id}` : update a tenant config
- `DELETE /admin/tenants/{id}` : delete a tenant config

Admin endpoints take/return:

```json
{
  "tenantId": "my-app",
  "algorithm": "TOKEN_BUCKET",
  "limit": 100,
  "windowSeconds": 60
}
```

`algorithm` is one of: `TOKEN_BUCKET`, `SLIDING_WINDOW`, `FIXED_WINDOW`.

## Benchmarks

Real numbers measured on this hardware:

```
Throughput:    6,255 req/sec (50 VUs sustained 30s)
p99 latency:   < 5ms (under light load)
p95 latency:   < 3.5ms
p50 latency:   < 2ms
Burst test:    100% success at 1000 concurrent requests
```

Tested on i5-11400H, Windows 11, Docker Desktop, all containers colocated. On Linux this benchmarks 2-3x higher because there's no WSL2 overhead. Full methodology in [BENCHMARKS.md](BENCHMARKS.md).

## Design decisions

### Why Redis + Lua and not just Java locks?

Java locks (`synchronized`, `ReentrantLock`) only work within one JVM. With 3 instances behind nginx, a Java lock is useless i.e. instances don't share JVMs. The lock has to live in shared state.

Redis executes Lua scripts atomically. The entire script runs as a single uninterruptible operation, regardless of how many clients are connected. This is the same guarantee as a `synchronized` block, except the lock is in Redis, which all instances share.

### Why three algorithms instead of just one?

Real rate limiters offer a choice. Stripe's API uses different algorithms for different endpoints. Building all three forced me to actually understand the tradeoffs instead of just memorizing them; token bucket's lazy refill math, fixed window's boundary problem, sliding window log's memory cost.

### Why the strategy pattern?

The algorithm choice is a runtime decision (per tenant), not compile-time. Polymorphism via subclassing doesn't work because the controller isn't choosing between algorithms by class type, it's reading the config from Redis and dispatching. The factory + interface pattern is the standard solution.

### What happens when Redis goes down?

Currently fail-closed: 503 to all clients with a clean error response. In production you'd typically fail open (allow requests when the rate limiter is down) so a Redis outage doesn't take down your entire API. I went fail-closed for v1 because it's simpler to reason about. A `failOpen` flag in tenant config would be a small addition.

## Tech stack

- Java 17, Spring Boot 3.5
- Redis 7 (Alpine), Lua scripts via `DefaultRedisScript`
- Lettuce as the Redis client (default in Spring Data Redis, Netty-based)
- Maven (via wrapper)
- Docker Compose for orchestration
- nginx for load balancing
- k6 for benchmarking
- JUnit 5 + Testcontainers for tests

## Running tests

```bash
.\mvnw test
```

11 tests, ~12 seconds. Integration tests spin up real Redis containers via Testcontainers and make sure Docker is running.

## What I'd do in v2

- Test on Linux to get the real performance ceiling (Docker Desktop on Windows costs ~50% throughput)
- Prometheus metrics endpoint for production observability
- Local cache for tenant configs with short TTL to handle Redis blips
- gRPC interface alongside REST
- A `failOpen` flag per tenant
- Persistent storage for tenant configs (currently lost if Redis volume dies)

## Project structure

```
src/main/java/com/sirhashir/fluxlimiter/
├── controller/      # HTTP layer
├── service/         # Business logic + algorithm implementations
├── model/           # DTOs and the Algorithm enum
├── exception/       # Custom exceptions
└── FluxLimiterApplication.java

src/main/resources/
├── application.yml
└── scripts/         # Lua scripts (one per algorithm)
    ├── token_bucket.lua
    ├── fixed_window.lua
    └── sliding_window.lua

benchmarks/          # k6 scripts
src/test/            # JUnit + Testcontainers tests
```

## License

MIT
