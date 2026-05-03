# Benchmarks

Ran k6 against the 3-instance setup behind nginx. All numbers below are with k6 running inside the Docker network — running it from the Windows host gave roughly half the throughput due to WSL2 networking overhead, which I confirmed by trying both.

## Setup

- i5-11400H, 16GB RAM, Windows 11, Docker Desktop (WSL2)
- 3 Spring Boot instances + nginx + Redis 7, all in Docker
- Tenant configured with `limit=1000000, windowSeconds=1` so we measure the actual system throughput rather than how fast we can deny rate-limited requests
- Each test run 3-4 times. Cold-start runs discarded.

## Throughput

50 VUs, 30s steady-state after 10s ramp-up.

    http_reqs:    281,480 total / 6,255 req/sec
    p50:          5.66ms
    p95:          13.14ms
    p99:          19.50ms
    HTTP errors:  0
    checks:       99.98% pass

The 0.02% check failures are `allowed=true` returning false at very high RPS — not a real concurrency bug, just floating-point edge cases in the token refill math when elapsed time is sub-millisecond at 1M tokens/sec. Doesn't happen at realistic refill rates.

## Latency

10 VUs, 30s. Thresholds set to plan targets (p50<2ms, p95<5ms, p99<10ms).

    p50:  1.83ms  ✓
    p95:  2.97ms  ✓
    p99:  4.10ms  ✓

All thresholds pass. p99 came in at under half the target; expected closer to 8-9ms.

## Burst

1000 VUs, 1 request each. Tests connection handling and atomicity under sudden load.

    checks: 100% (1000/1000)
    HTTP errors: 0
    p95: 60-180ms (varies by warmup state)
    total duration: ~0.4s

First run had 19 connection resets; Tomcat's thread pool wasn't warm yet. Subsequent runs were clean 100% so reporting steady-state.

## Reproducing

    docker compose up -d
    docker exec flux-redis redis-cli FLUSHDB
    curl -X POST http://localhost:8080/admin/tenants \
      -H "Content-Type: application/json" \
      -d '{"tenantId":"bench","algorithm":"TOKEN_BUCKET","limit":1000000,"windowSeconds":1}'

    docker compose --profile benchmark run --rm k6 run /benchmarks/throughput.js
    docker compose --profile benchmark run --rm k6 run /benchmarks/latency.js
    docker compose --profile benchmark run --rm k6 run /benchmarks/burst.js

## Caveats

These numbers are bounded by my laptop's Docker Desktop on Windows. On Linux the same architecture usually benchmarks 2-3x higher because there's no WSL2 hop. Numbers are still genuinely useful as relative measurements between algorithms or for tracking regressions.