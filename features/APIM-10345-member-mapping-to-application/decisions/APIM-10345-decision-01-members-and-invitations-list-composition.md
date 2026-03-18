# APIM-10345-decision-01 Members and Invitations List Composition

## Decision Summary
- Epic: `APIM-10345 Member Mapping to Application`
- Decision status: `Agreed`
- Decision date: `2025-03-18`
- Decision meeting: `APIM Core Refinement`
- Approved option: `Option 02 Separate lists for members and invited people`

## Decision Topic
Decide whether the application member-management experience should present:
- one combined list containing both active members and invited people
- or two separate lists, one for active members and one for invited people

## Options Considered
### Option 01 Unified list of members and invited people
This option would present active members and invited people in one shared list, with one shared sorting, filtering, and pagination model.

Arguments in favor:
- It can feel more natural from a business and user perspective because all access-related entries are visible in one place.
- It can reduce context switching between active access and pending access.
- It may help users understand the full collaboration state of the application at a glance.

Arguments against:
- It creates significant technical risk for the first delivery because the visible list would have to behave as one coherent paginated collection while being backed by two different data sources and two different lifecycle models.
- It makes sorting, pagination, and refresh behavior harder to define consistently when active members and invitations do not share the same underlying read model.
- It increases delivery complexity for backend contracts and frontend state management in a part of the feature that already includes invitations, role changes, and deletion flows.

### Option 02 Separate lists for members and invited people
This option keeps active members in the main members list and exposes invited people in a separate pending-invitations list or section within the same feature area.

Arguments in favor:
- It is significantly easier to deliver reliably because each list can have its own clear source of data, sorting model, pagination, and empty state.
- It keeps a clean distinction between active access and pending access, which also matches the business difference between a membership and an invitation.
- It reduces implementation risk for the first version while still keeping both kinds of information available in the same overall user journey.

Arguments against:
- It is less compact than a single unified list and may feel less convenient for some users.
- It requires users to scan two related list areas instead of one.
- It may be seen as less elegant from a product perspective than a single consolidated collaboration view.

### Option 03 Unified list with database-side aggregation
This option keeps the unified-list product model but pushes the merge, unified sorting, filtering, and pagination logic down to the persistence layer instead of doing the final merge in Java memory.

Arguments in favor:
- It preserves coherent unified-list semantics while avoiding a Java-side full-list merge for every request.
- For MongoDB, it fits an existing technical style in the repository module because custom aggregation pipelines already exist in the codebase, even if this specific cross-collection access-entry query would still be new.
- It gives a clearer path to one queryable unified page than trying to merge two independently paged sources in the service layer.

Arguments against:
- It is not one shared implementation for all repository technologies. MongoDB and JDBC would require different query implementations and separate performance validation.
- It moves a more complex business-shaped read model into persistence-specific code, which increases repository complexity and maintenance cost.
- Even in the database, sorting and filtering on a computed unified display field can still become expensive for larger datasets.

## People Involved
- `Varun Goel`, Product Owner, final approver
- `APIM Core Refinement` participants, reviewers of benchmark results and delivery options

## Agreed Direction
The decision taken during `APIM Core Refinement` on `2025-03-18` is to change the first-delivery UX and split the application access experience into two separate screens:
- one `Members` screen for active application members
- one `Invitations` screen for pending and expired invitations

The current mockups that still suggest an inline or unified presentation must therefore be treated as pre-decision artifacts and updated to reflect the agreed separate-screen direction.

This means the first delivery must not attempt to expose one unified paginated collection of members and invitations.

If customers later come back with a validated need for a combined access view, that requirement should be reopened as a new product and technical decision rather than reintroduced implicitly in the current delivery scope.

The agreed implementation consequences are:
- members and invitations keep separate read models and separate list semantics
- sorting, filtering, pagination, and refresh behavior are defined independently per list
- story and implementation descriptions should treat invitation management as a dedicated screen instead of inline rows inside the members table

## Decision Rationale
During `APIM Core Refinement` on `2025-03-18`, the team reviewed benchmark results and discussed the available implementation options.

The agreed conclusion was that the benchmark discussion did not justify taking on the additional product and technical complexity of a unified paginated list for the first delivery.

`Varun Goel`, acting as Product Owner, approved the direction to separate `Members` and `Invitations`, update the mockups accordingly, and treat that UX change as the basis for the implementation plan and detailed task descriptions.

## Technical Analysis Considered During Decision
The sections below document the technical analysis that was reviewed while comparing the unified-list alternatives against the now-agreed separate-list direction.

## Technical Analysis of the Unified-List Option
### Existing data sources in the current codebase
Active members and pending invitations are not currently exposed through one shared query path.

The active-members path already exists through `MembershipService.getMembersByReference(...)`. In `MembershipServiceImpl`, the service reads repository memberships through `membershipRepository.findByReferencesAndRoleId(...)`, converts them into `MemberEntity`, enriches them with user information, and then paginates them through the internal `paginate(...)` method. That pagination currently depends on an in-memory sorted collection of `MemberEntity`, with sorting based on member display name.

The invitations path is separate. `InvitationService.findByReference(...)` reads invitations through `invitationRepository.findByReferenceIdAndReferenceType(...)`, converts them into `InvitationEntity`, and sorts them by invitation email. The current repository API for invitations returns a full `List<Invitation>` and does not expose pageable or search-aware retrieval.

At the portal REST layer, `ApplicationMembersResource` currently reads the full active-members collection and only afterwards applies portal response pagination through `createListResponse(...)`. That means the existing portal members endpoint is already collection-based rather than backed by a repository-level pageable query.

### Repository methods that would likely be used
If a unified endpoint were introduced in the current architecture, the most direct data sources would be:
- `MembershipRepository.findByReferencesAndRoleId(...)` through `MembershipService`
- `InvitationRepository.findByReferenceIdAndReferenceType(...)` through `InvitationService`

Those methods are sufficient to retrieve both datasets for one application, but they do not provide a natural unified-page query at repository level.

The most important asymmetry is:
- active members already have a service-level conversion and pagination path
- invitations only have a full-list retrieval path with in-memory sorting by email

### Where the unification would most likely happen
The most realistic place to join both datasets would be above the repository layer, in a dedicated query or use-case layer for the portal-facing endpoint.

The repository layer is not currently shaped around a shared model for members and invitations. Forcing that merge directly into repository code would couple two unrelated persistence concepts too early and would still not remove the core pagination problem.

The cleaner approach would be:
- keep repository reads separate
- keep membership and invitation conversion separate
- introduce one dedicated portal-facing query service or use case that builds a unified read model

That unified read model could be something like a single collection of access entries containing:
- entry kind: `member` or `invitation`
- stable row id
- identity label used for display and sorting
- email
- type
- status
- role
- timestamps needed for invitation rows

This is the place where `MemberEntity` and `InvitationEntity` would be mapped into one common response shape before filtering, sorting, and pagination are applied.

### How filtering could work
For a unified list, filtering would need to run on the unified business fields, not on the raw repository models.

The likely rule set would be:
- member rows match on display name, email, type, status, and role
- invitation rows match on invitation email, type, status, and role

In the current codebase, that filtering would be easiest to implement after both sources are mapped into the unified model. That keeps the search behavior consistent with what the user sees on screen.

### How pagination could work
This is the main technical difficulty.

If the endpoint must expose one coherent paginated list, then the final page must be calculated after:
- both sources are fetched
- both sources are mapped to one model
- one common filter is applied
- one common sort order is applied

That means the straightforward implementation would be:
1. load all active members for the application
2. load all invitations for the application
3. map both collections to one unified list
4. apply the query filter
5. sort the merged result
6. slice the requested page

This would produce correct and deterministic pagination semantics, but only by doing the merge in memory.

### Why independent source pagination would not be enough
Trying to paginate each source independently before the merge would not produce a correct unified page.

For example, if members and invitations are each paginated separately, the endpoint would not know which items really belong to unified page 2 after one common sort order is applied. A row that should appear early in the merged list might be hidden in a later page of one source, which would make the merged result inconsistent.

Because of that, source-level pagination is not sufficient unless there is:
- a real shared queryable read model
- or a much more complex merge algorithm with read-ahead from both sources

### Most realistic short-term implementation
In the current architecture, the most realistic short-term implementation of a unified endpoint would therefore be:
- introduce a dedicated portal-facing query service for application access entries
- read members through `MembershipService`
- read invitations through `InvitationService`
- map both into one unified access-entry DTO
- filter, sort, and paginate in memory

This would be implementable without redesigning repository storage, but it would carry the known concerns around:
- scalability for larger datasets
- duplicated sorting and filtering logic
- increased complexity of the portal endpoint

### Application-side sorting and pagination proof-of-concept benchmark observations
A local MongoDB proof of concept and benchmark were also executed for the fallback model where:
- MongoDB is used only as flat storage
- memberships, invitations, and users are read independently
- `displayName` is computed in the application layer
- merge, filtering, sorting, and pagination are executed in Java
- pagination is applied at `offset=100` and `limit=50`
- three full benchmark series are executed per dataset
- each series contains five measured runs after a warm-up query
- the reported result uses the min, median, and max of the series averages

The observed response times on the latest local benchmark run were:
- `500` total entries: unfiltered `24.09 / 25.63 / 40.71 ms`, filtered `23.77 / 28.73 / 42.80 ms`
- `2,000` total entries: unfiltered `30.86 / 31.79 / 34.57 ms`, filtered `30.50 / 33.37 / 34.28 ms`
- `5,000` total entries: unfiltered `47.97 / 51.83 / 52.62 ms`, filtered `55.83 / 56.46 / 56.86 ms`
- `10,000` total entries: unfiltered `88.87 / 91.71 / 98.18 ms`, filtered `88.75 / 90.09 / 98.27 ms`

These numbers are also only local directional signals, not production-grade SLA evidence. Still, they are useful in the decision discussion because they show that the simpler application-side sorting model is not automatically slower than the database-side aggregation model for MongoDB in this local setup.

### Alternative short-term implementation with database-side aggregation
An alternative short-term path is to keep the unified endpoint but move the final merge to the database query layer.

For MongoDB, the proof of concept indicates that the query could work like this:
1. read memberships for one application
2. join each membership with `users` to compute a member `displayName`
3. project memberships into one unified access-entry shape
4. `union` that stream with invitations projected into the same shape
5. apply unified filtering on the computed display field
6. apply one shared sort order
7. apply `skip` and `limit`

That gives correct unified-list semantics without Java-side full-list merging, but it does not remove all scalability concerns because the database still has to build and sort the unified result.

### How much this would reuse existing repository patterns
This would be a new query for this feature, but not a completely new technical pattern in the MongoDB repository module.

The MongoDB module already uses persistence-specific query logic and aggregation-style repository implementations in several places. What would be new here is:
- unifying memberships and invitations into one shared read model
- projecting a computed `displayName` for members through a join with users
- applying one shared filter, sort, and pagination model across both sources

So the pattern is closer to "new use of an existing repository style" than to "entirely new infrastructure".

For JDBC, the equivalent idea would rely on SQL-side composition instead of a MongoDB aggregation pipeline. That means the general architectural direction can be shared, but the implementation details and performance profile must be validated separately per repository technology.

### MongoDB proof-of-concept benchmark observations
A local MongoDB proof of concept and benchmark were executed for a unified query using:
- a 50/50 split of members and invitations
- one shared computed `displayName`
- sorting, filtering, and pagination at `offset=100` and `limit=50`
- three full benchmark series per dataset
- five measured runs per series after a warm-up query
- the reported result uses the min, median, and max of the series averages

The observed response times on the latest local benchmark run were:
- `500` total entries: unfiltered `24.71 / 26.03 / 26.10 ms`, filtered `26.60 / 27.28 / 28.51 ms`
- `2,000` total entries: unfiltered `66.11 / 72.20 / 72.21 ms`, filtered `76.01 / 77.94 / 83.70 ms`
- `5,000` total entries: unfiltered `162.72 / 167.05 / 174.98 ms`, filtered `153.50 / 188.70 / 190.98 ms`
- `10,000` total entries: unfiltered `308.06 / 309.97 / 341.55 ms`, filtered `304.53 / 316.80 / 335.41 ms`

These numbers are useful only as local directional signals, not as production-grade SLA evidence. They do, however, support two conclusions:
- the approach is technically feasible on pure MongoDB
- response time grows fast enough that larger lists remain a real concern even when the merge is pushed into the database

Compared with the application-side sorting proof of concept above, the local benchmark signals suggest that MongoDB-side aggregation with `$lookup + $unionWith + unified sort` may actually be slower than the simpler flat-storage read plus Java-side merge for this specific read shape. That does not invalidate the database-side option, but it does weaken the assumption that pushing the merge into MongoDB is automatically the better short-term performance trade-off.

### JDBC proof-of-concept benchmark observations
A local JDBC proof of concept and benchmark were also executed for a unified SQL query using:
- a 50/50 split of members and invitations
- one shared computed `displayName`
- sorting, filtering, and pagination at `offset=100` and `limit=50`
- three full benchmark series per dataset
- five measured runs per series after a warm-up query
- the reported result uses the min, median, and max of the series averages

The observed response times on the latest local benchmark run were:
- `500` total entries: unfiltered `4.15 / 5.12 / 6.67 ms`, filtered `3.63 / 3.77 / 4.40 ms`
- `2,000` total entries: unfiltered `8.73 / 9.49 / 10.28 ms`, filtered `8.66 / 8.96 / 9.11 ms`
- `5,000` total entries: unfiltered `19.01 / 19.59 / 19.74 ms`, filtered `17.39 / 18.31 / 20.53 ms`
- `10,000` total entries: unfiltered `35.67 / 39.25 / 39.27 ms`, filtered `36.66 / 38.76 / 59.89 ms`

These numbers are also only local directional signals, not production-grade SLA evidence. Still, compared with the MongoDB proof of concept, they suggest that the JDBC path is materially more efficient for this specific read shape in the local test setup.

### JDBC application-side sorting and pagination proof-of-concept benchmark observations
A local JDBC proof of concept and benchmark were also executed for the fallback model where:
- JDBC is used as flat storage
- memberships, invitations, and users are read independently
- `displayName` is computed in the application layer
- merge, filtering, sorting, and pagination are executed in Java
- pagination is applied at `offset=100` and `limit=50`
- three full benchmark series are executed per dataset
- each series contains five measured runs after a warm-up query
- the reported result uses the min, median, and max of the series averages

The observed response times on the latest local benchmark run were:
- `500` total entries: unfiltered `8.58 / 11.84 / 12.76 ms`, filtered `10.16 / 10.36 / 11.46 ms`
- `2,000` total entries: unfiltered `23.66 / 44.27 / 45.62 ms`, filtered `42.27 / 44.65 / 49.08 ms`
- `5,000` total entries: unfiltered `26.36 / 26.96 / 29.79 ms`, filtered `27.48 / 29.56 / 30.20 ms`
- `10,000` total entries: unfiltered `48.90 / 50.11 / 51.75 ms`, filtered `50.14 / 50.41 / 57.18 ms`

These results are directionally useful, but the non-monotonic behavior between some dataset sizes suggests visible benchmark noise in the local setup. Even so, they still indicate that the JDBC fallback model is technically viable, while the SQL-side composition path appears more stable and generally faster for the same read shape.

Taken together, the current benchmarks support four conclusions:
- the database-side aggregation approach is technically feasible for both MongoDB and JDBC
- the application-side merge, filtering, sorting, and pagination approach is also technically feasible for both MongoDB and JDBC
- on MongoDB, the local benchmark signals still favor the simpler flat-storage read plus Java-side merge over the aggregation-heavy `$lookup + $unionWith` query shape
- on JDBC, the local benchmark signals favor SQL-side composition over Java-side merge for this specific read shape, but all measurements remain environment-specific and insufficient as standalone proof for production-scale behavior

### More robust longer-term implementation
A more robust solution would require a dedicated unified read model.

That could mean one of the following:
- a new repository-level search path that stores or materializes both active memberships and invitations into one queryable collection
- a dedicated query projection for application access entries
- or another persistence-backed aggregation model that can support one common sort and page operation directly

That approach would reduce runtime merge complexity, but it would be significantly more expensive to introduce than a separate-lists solution.
