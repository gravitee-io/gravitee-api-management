A second AI agent reviewed our story decomposition. Here's their feedback:

1) Missing Stories / Gaps in Coverage
Missing explicit contract/validation story for query semantics

Why it's a problem: AC 1–4 depend on strict parameter behavior (field required for STATS/GROUP_BY, interval for DATE_HISTO, supported field whitelist). This is spread across stories but not treated as a single testable contract.
Fix: Add a dedicated backend story: "Unified endpoint request validation & error model" covering required params, unsupported field, invalid order, from > to, negative/zero interval, and consistent 400 payload format.
Missing non-functional verification story

Why: PRD has hard NFRs (<2s, graceful degradation, all widgets refresh together). Current stories mention behavior but don't include measurable validation tasks.
Fix: Add a small cross-cutting story: "NFR validation and instrumentation" with explicit perf test scenario and degraded ES behavior checks.
Missing compatibility story for existing line charts contract stability

Why: AC 9 says existing line charts must not break; current stories mention "don't remove" but not explicit backward-compat test matrix.
Fix: Add acceptance criteria in US-08/US-10 to verify old service methods and existing chart inputs remain unchanged.
2) Stories Too Large (Should Split)
US-01 is too broad

Includes endpoint creation, COUNT behavior, OpenAPI, new enum/dispatcher, permission, index routing.
Why: Hard to deliver/verify in one increment; high merge risk.
Fix split:
US-01a: endpoint skeleton + auth + COUNT only
US-01b: API contract/openapi + shared dispatcher scaffolding
US-08 is too broad

Layout composition + event orchestration + regressions + all-empty state + routing constraints.
Why: mixes UI composition and behavioral integration testing concerns.
Fix split:
US-08a: layout + component composition
US-08b: refresh orchestration + non-regression + all-empty page behavior
US-10 is too broad for frontend tests

Includes service tests, several widget tests, dashboard integration tests.
Fix split: US-10a (service + widget unit tests), US-10b (dashboard integration/non-regression tests).
3) Stories Too Small (Should Merge)
US-05 likely too small as standalone

A timeframe filter bar already exists in current console implementation pattern.
Why: "new standalone component" may duplicate existing behavior; this story may become pure refactor churn.
Fix: Merge US-05 into US-08 (dashboard wiring), unless there is a confirmed need for net-new component.
US-04 could be merged with initial dashboard wiring

Why: service changes alone provide limited business value without first consuming widgets.
Fix: Either keep as technical enabler with strict scope, or merge with US-06/US-07 implementation slices.
4) Wrong Ordering / Missing Dependencies
US-04 dependency is incomplete

It depends not only on US-01 but on final backend request/response contract (US-02/US-03 too).
Fix: Set dependency as "US-01 + contract freeze from US-02/US-03" or implement typed service incrementally by query type.
Testing stories are too late

US-09/US-10 in Sprint 3 only increases late defect risk.
Fix: Move test work to be embedded per story:
backend tests with US-01/02/03
frontend tests with US-06/07/08
US-03 depending on US-02 is not strictly required

DATE_HISTO can be implemented independently after endpoint skeleton.
Fix: both US-02 and US-03 should depend on US-01 only; run in parallel.
5) Over-engineering Beyond PRD
US-03 bucket-limit rule (>1440 buckets)

Why: Not in PRD/AC; may be useful but is extra policy that can block valid queries.
Fix: Move to "optional guardrail" note, not must-have AC.
US-05 introduces new component + signals/no-BehaviorSubject constraint

Why: PRD asks predefined ranges and OnPush/signals generally, but existing service already manages timeframe. Forcing "no BehaviorSubject" may cause unnecessary rewrite.
Fix: Keep functional requirements; avoid prescribing internal state implementation unless needed.
US-07 strict color ramps + tooltip formatting specifics

Why: Not required by AC; too design-detailed for M1.
Fix: Reduce to "use existing chart library conventions" and keep exact palette as UX enhancement.
US-09 line-coverage target >=90%

Why: not in PRD/AC and can cause process friction.
Fix: replace with scenario-based mandatory cases tied to AC.
6) Edge Cases Missed in Current Decomposition
Time-range validation
from >= to, huge window, future timestamps, missing timezone assumptions.
No-data semantics consistency
COUNT=0, STATS with zero docs, empty GROUP_BY, empty DATE_HISTO should be consistently handled by UI.
Partial failure behavior
If one widget query fails and others succeed, page should degrade gracefully (PRD asks graceful degradation).
Metadata robustness
GROUP_BY metadata for unknown status-like values or non-status fields.
Ordering determinism
Ties in GROUP_BY ordering, explicit default order handling.
Auth edge
User with API access but no API_ANALYTICS:READ in mixed-role contexts.
Suggested Revised Story Structure (Actionable)
Backend

Endpoint skeleton + auth + COUNT
Unified request validation/error model
STATS + GROUP_BY
DATE_HISTO
OpenAPI contract finalization
Frontend 6. Unified analytics service (typed) + backward compatibility 7. Stats cards integration 8. HTTP status pie integration 9. Dashboard composition + shared timeframe wiring + non-regression of existing charts 10. Empty/error/partial-failure states across widgets

Testing & NFR 11. Backend tests per query type + validation + permission 12. Frontend component/integration tests + refresh orchestration 13. NFR verification (<2s, graceful degradation)

Review each suggestion and either:
- Accept it and update STORIES.md accordingly
- Reject it with a brief explanation of why

Update ./docs/workshop/STORIES.md with the final refined version. Add a
"Refinement Notes" section at the bottom documenting what changed and why.
