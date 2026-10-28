# Research foundation

**Applied software engineering prototype.** Independent student work; no affiliation with Royal Enfield. Sources checked on 28 October 2026 before implementation. This is an implementation opportunity, not a claim of global novelty or a replacement for research scheduling algorithms.

## Verified references and scope

1. Mohammad Behbahani, Reza Izadbakhsh and Hamidreza Izadbakhsh. **A multi-objective open shop model for optimizing scheduling in automotive repair shops.** *Decision Analytics Journal*, volume 18, article 100682, March 2026. [DOI](https://doi.org/10.1016/j.dajour.2026.100682). [Publisher](https://www.sciencedirect.com/science/article/pii/S277266222600010X). [Author institution record](https://research-portal.uu.nl/en/publications/a-multi-objective-open-shop-model-for-optimizing-scheduling-in-au/).
   The accessible abstract describes constrained repair-station assignment and sequencing, balancing vehicle flow time and station idle/overtime cost. It uses mixed-integer linear programming and a modified NSGA-II method. Our operational takeaway is to model finite workshop capacity explicitly. We implement fixed appointment-capacity units; we do not implement repair-station sequencing, MILP, genetic search, or claim comparable results. Access: indexed publisher abstract and institutional metadata; direct publisher retrieval returned 403. Full paper was not reviewed.

2. Yan Zhu, Zhixin Liu and Xiangtong Qi. **Nonsequential Appointment Scheduling With a Random Number of Requests.** *Production and Operations Management*, 33(1), 184–204, 2024. Publisher lists first online publication as 8 February 2024 and issue publication as January 2024. [DOI](https://doi.org/10.1177/10591478231224926). [Publisher abstract](https://journals.sagepub.com/doi/full/10.1177/10591478231224926).
   The abstract considers dynamically arriving appointment requests with a random number of potential customers. Its nonsequential policy and branch-and-bound approach allow scheduling choices beyond simple arrival order. Our takeaway is that admission policies should be explicit. We choose FIFO only for transparency, simplicity and testability; it is not asserted to be optimal. We exclude stochastic demand estimation, gap optimization, dynamic programming and branch-and-bound. Access: publisher metadata and abstract; article marked restricted access.

3. Amirhossein Moosavi, Onur Ozturk, Rafid Mahmood and Jonathan Patrick. **Deep Learning–Assisted Appointment Scheduling Under Uncertainty.** *INFORMS Journal on Data Science*, published online 25 August 2026, articles in advance. [DOI](https://doi.org/10.1287/ijds.2025.0090). [Publisher](https://pubsonline.informs.org/doi/10.1287/ijds.2025.0090).
   The abstract studies heterogeneous appointments and multiple providers under uncertain processing times, punctuality and no-shows. It combines a neural model with partial mixed-integer programming. We use this as a boundary on our claims: fixed-slot admission does not solve uncertain service execution. We deliberately exclude deep learning, prediction, optimization and no-show modelling. Access: publisher abstract and metadata; full paper not reviewed. We do not reuse its performance numbers.

## Business context

The official [service booking page](https://www.royalenfield.com/in/en/app/service-booking/) presents motorcycle details, service centers, service types, service dates and optional instructions. The official [app page](https://www.royalenfield.com/in/en/re-app/) also provides service-booking context. These establish that booking is a practical service workflow. They do **not** establish that existing platforms lack cancellation, waitlists, or concurrency control. No authenticated flow, private API or customer database was accessed.

## Implementation opportunity

Build a small, inspectable Java monolith that combines transactional capacity admission, persisted FIFO recovery, customer isolation, and auditable service transitions. Measure correctness in synthetic contention rather than asserting a new scheduling algorithm.

**Assumption:** Each appointment consumes one slot-capacity unit. Repair duration, technician availability, overlapping slots at different workshops and job-shop sequencing are outside scope.

## Research question and hypotheses

Can transactional capacity control and automatic FIFO waitlist promotion prevent workshop overbooking and recover cancelled service slots in a small, simulated motorcycle service center?

- **H1:** With PostgreSQL transactional locking, concurrent reservation requests will never cause non-cancelled bookings to exceed slot capacity. Finite tests can support the invariant for observed workloads, not prove “never” universally. A locking argument accompanies the tests.
- **H2:** Under identical synthetic cancellations and eligible waiting demand, automatic promotion fills more cancelled spaces than a baseline without reallocation.

## Method

`EvaluationIT` runs 20 requests/capacity 2 and 50 requests/capacity 5, three repetitions per approach, resetting the dedicated test database each time. An unsafe direct-JDBC read-then-insert baseline exists only in test source. Requests start from a latch; no sleeps or forced insert outcomes. If a naive run does not overbook, its actual zero must be reported. Measure requests, acceptances, rejections, occupancy, overbooking, duration, median and nearest-rank p95 operation latency.

Recovery uses capacity 5, five initial bookings, ten waiting requests, and cancellations of the first two bookings for both policies. Record recovered spaces, unused capacity, fill rate and promoted count. IDs 1–60 define deterministic synthetic input; no random generator is used. No real users or revenue.

**Current decision: H1 and H2 NOT EVALUATED.** Database experiments were not executed in this environment. Do not treat expected values in test assertions as observations. See evaluation.md and verification-report.md.

## Limitations and future work

Results, when run, apply to a finite local workload. The baseline and full service have different overhead, so latency is descriptive. No warm-up, realistic arrival distribution, customer study, duration model or revenue analysis. Future work may test lock timeouts, richer scheduling constraints, migration upgrades and realistic cancellation distributions, while preserving a clear capacity invariant.
