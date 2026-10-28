# Synthetic evaluation

**NOT EXECUTED:** PostgreSQL was unavailable for executable experiments in this workspace. No CSV observations or numerical performance claims are included. H1/H2 remain unevaluated. Expected assertions are not measurements.

The runnable harness is `src/test/java/com/motoservice/EvaluationIT.java`. The unsafe baseline is test-only and cannot be reached through application endpoints. Every scenario starts from fresh synthetic rows. PostgreSQL is supplied by Testcontainers by default; an explicit dedicated `motoservice_test` database is also supported.

## Experiment A

- Request counts/capacity: 20/2 and 50/5.
- Three repetitions for each baseline and transactional approach: 12 rows in total if the evaluation succeeds.
- 60 synthetic customer/motorcycle pairs, fixed IDs; no RNG seed is needed.
- Start latch and bounded future timeouts; one worker per request.
- Naive baseline reads occupancy and then inserts using separate JDBC statements, without the slot lock. No sleeps or forced race outcomes.
- Proposed approach invokes the real proxied Java service with its PostgreSQL transaction and slot lock.
- Record requested/confirmed/rejected, overbooking=max(0,occupancy-capacity), final occupancy, wall-clock duration, median and nearest-rank p95 operation latency.
- Assert exact proposed capacity occupancy, not just “at most” capacity. Unexpected exceptions fail the run. Baseline may or may not overbook in a particular repetition; report whatever occurs.

## Experiment B

Both scenarios start with capacity 5, five confirmed bookings, ten eligible waiting customers and the same first-two cancellation sequence. Baseline only changes confirmed bookings to cancelled in the isolated harness. Proposed invokes service cancellation and FIFO promotion. Record cancelled spaces, recovered spaces, unused capacity, fill rate=occupancy/capacity, and promoted customers. The test asserts the specified behavior, but no actual result has been observed here.

## Run and reset

```powershell
.\scripts\benchmark\evaluate.ps1 -ResetOnly
.\scripts\benchmark\evaluate.ps1
```

The first command resets only the dedicated/disposable synthetic database. The second also resets data between comparisons and writes `target/evaluation/overbooking.csv`, `recovery.csv`, `environment.txt` and `results.md`. Do not copy old CSV files into a report for a newer failed run. GitHub Actions uploads test/evaluation evidence if produced.

## Performance interpretation

Operation latency includes service/database work and lock waiting, but excludes HTTP and browser rendering. Baseline direct JDBC and proposed JPA have different overhead; this is not an isolated algorithm speed benchmark. With 20 or 50 samples per row, p95 is a coarse descriptive statistic. No warm-up, production tuning or controlled hardware isolation is claimed.

`scripts/benchmark/http-read.ps1` separately measures actual sequential GET /api/slots request latency, total/success/failed counts, median and p95, concurrency=1. This runner is also NOT EXECUTED here. Record machine/OS/runtime when using it. Neither experiment proves scalability, revenue, satisfaction, production availability or superiority to published optimization research.
