# PORTAL-178 Portal Notifications Enablement — implementation plan

Release: APIM 4.14, next-gen Developer Portal. Estimate: 15 working days (backend 11, frontend 4), one PR per user
story, upper bound 4 weeks. Requirements: PRD PN-01 to PN-23.

## 1. Goal

A signed-in consumer of the next-gen portal can, without opening Console:

- read their notifications in an inbox (Dashboard → Notifications), filter them by API, application and read state,
  open one to read its message, which marks it read; see an unread count in the avatar menu;
- set, per application they manage, which subscription and certificate events they want, with a days-before-expiry
  value;
- set, per API they subscribe to, which subscription, API-key and lifecycle events they want, with a
  days-before-expiry value — without picking an application;
- turn all events of an API on or off in one click from the catalog (bell);
- receive close-to-expiry notices for subscriptions, API keys and client certificates at the number of days they
  chose, and a notice when a client certificate expires.

## 2. Decisions

### 2.1 Product

| # | Decision |
| --- | --- |
| P1 | Preferences are **per user**. App preferences: per user and application. API preferences: per user and API, independent of the application used to subscribe. |
| P2 | Apps tab lists every application where the user holds `APPLICATION_NOTIFICATION` (owner or member with that permission, direct or through a group). |
| P3 | APIs tab and bell only concern APIs the user **subscribes to** (§2.3). The bell is hidden on other APIs. |
| P4 | Lifecycle events offered to consumers: `API_STARTED`, `API_STOPPED`, `API_DEPLOYED`, `API_DEPRECATED`, `API_UPDATED`. `API_UPDATED` fires on every publisher save. |
| P5 | Consumer-worded portal templates for API-key, lifecycle and notice events; they never show the publisher's name. |
| P6 | Days before expiry: integer 1 to 366, default 30, one value per preference shared by all its close-to-expiry events. |
| P7 | Bell: blue when at least one event is on for that API. Click on grey: turns on every PN-20 event and keeps the existing close-to-expiry settings. Click on blue: turns every event off, close-to-expiry included. |
| P8 | Notices (close to expiry, certificate expired) are written to the portal inbox only: no email, no webhook. The platform's global pre-expiration email schedule (`services.subscription.pre-expiration-notification-schedule`) is unchanged. |
| P9 | API Products are out of scope: no product in the APIs tab, no bell on product cards, no product preference. Product subscription events still reach app preferences, which already handle `apiProduct` in their templates. |
| P10 | The inbox is scoped to the environment: a notification stores the environment it was triggered in, and the portal of an environment shows that environment's notifications plus those with no environment (organization-level triggers, rows created before 4.14). The unread count follows the same scope. |

### 2.2 Events per preference

| Category | Event | App preference | API preference | Bell (PN-20) | Kind |
| --- | --- | --- | --- | --- | --- |
| Subscription | `SUBSCRIPTION_NEW`, `SUBSCRIPTION_ACCEPTED`, `SUBSCRIPTION_CLOSED`, `SUBSCRIPTION_PAUSED`, `SUBSCRIPTION_RESUMED`, `SUBSCRIPTION_REJECTED`, `SUBSCRIPTION_TRANSFERRED`, `SUBSCRIPTION_FAILED` | ✓ | ✓ | ✓ | existing hook |
| Subscription | `SUBSCRIPTION_CLOSE_TO_EXPIRY` | ✓ | ✓ | — | notice |
| API key | `APIKEY_EXPIRED`, `APIKEY_RENEWED`, `APIKEY_REVOKED` | — | ✓ | ✓ | existing hook |
| API key | `APIKEY_CLOSE_TO_EXPIRY` | — | ✓ | — | notice |
| Lifecycle | `API_STARTED`, `API_STOPPED`, `API_DEPLOYED`, `API_DEPRECATED`, `API_UPDATED` | — | ✓ | ✓ | existing hook |
| Certificate | `CERTIFICATE_EXPIRED`, `CERTIFICATE_CLOSE_TO_EXPIRY` | ✓ | — | — | notice |

Any other value is rejected with 400. **Hooks** are the existing `ApplicationHook` / `ApiHook` values fired by the
platform. **Notices** are produced by the new daily job (US 7, US 8); they are not new `ApplicationHook` / `ApiHook`
values, so Console publisher settings, email notifiers, webhooks and the CRD `consoleNotification` enum are
untouched.

### 2.3 Subscriber of an API

A user **subscribes to** an API when they are a member (any role, direct or through a group) of an application that
has a subscription in status `ACCEPTED`, `PAUSED` or `PENDING` on a plan of that API. Subscriptions to API Product
plans do not count. The rule is evaluated at save time (preference endpoints) **and** at delivery time, so a user
who loses their subscription stops receiving events and never sees another consumer's subscription.

## 3. What exists today (master)

| Area | Today | Gap |
| --- | --- | --- |
| Engine | `NotifierServiceImpl.trigger(...)` feeds the inbox (`triggerPortalNotifications`: `PortalNotificationConfigRepository.findByReferenceAndHook` → users + group members → `PortalNotificationService.create`) and email/webhook (`triggerGenericNotifications`) in one call. Core use cases go through `TriggerNotificationDomainService` → `TriggerNotificationDomainServiceFacadeImpl` → `NotifierService`. | Reused as is; consumer routing added. |
| Subscription hooks | `SubscriptionServiceImpl.triggerSubscriptionNotificationsForApi` (~l.990) and the core subscription domain services fire the hook on the API **and** on the application. | Consumers on the API never get them. |
| API-key / lifecycle hooks | `ApiKeyServiceImpl` (`APIKEY_RENEWED`, `APIKEY_EXPIRED`), `RevokeApiKeyDomainService` (`APIKEY_REVOKED`), `ApiServiceImpl` (`API_STARTED`/`STOPPED`), `v4/impl/ApiNotificationServiceImpl` (lifecycle, skipped when the actor is not authenticated). API reference only. | Consumers never get them. |
| Templates | `templates/notifications/portal/<SCOPE>.<HOOK>.yml`, loaded at startup by `NotificationTemplateServiceImpl.afterPropertiesSet` from the hook enums, overridable per organization. `API.API_*` templates contain "by ${user.displayName}". | No consumer wording. |
| `portal_notifications` | `id`, `title`, `message`, `user`, `created_at`. `update()` throws. Mongo index `u1` on `user` (`portalnotifications/UserIndexUpgrader`). | No read state, hook, API, application or environment. |
| `GET/DELETE /user/notifications` (Portal API) | `findByUser` then in-memory sort and paging, no filters, every environment of the organization mixed. | No read state, filters, environment scope, total from the database. |
| `portal_notification_configs` | One row per (`user`, `reference_type`, `reference_id`), `hooks` list (JDBC table `portal_notification_config_hooks`), `groups`, `origin`. Type `APPLICATION` used by the classic portal. Type `API` used by API members (Console). | No consumer per-API type, no days. |
| `GET/PUT /applications/{id}/notifications` (Portal API) | Flat `string[]`; PUT overwrites the caller's hooks **and** every email/webhook config of the app; no validation. | Unsafe to reuse. |
| Close to expiry | `ScheduledSubscriptionPreExpirationNotificationService`: hourly cron, global 90/45/30 days, emails the subscriber and the app primary owner. Not a hook. | No inbox notice, no per-user days. |
| Certificates | `ClientCertificate` (per application): `startsAt`, `endsAt`, `certificateExpiration`, `status` (`ACTIVE`, `ACTIVE_WITH_END`, `SCHEDULED`, `REVOKED`). Revocation scheduler only. | No expiry event. |
| Next-gen portal | `dashboard` routes (applications, subscriptions, workspaces, analytics), `user-avatar` menu, `api-card` (grid) and list view in `catalog.component`, `configuration.scheduler.notificationsInSeconds`. | No notification code. |
| Portal API docs | Operations tagged `x-gravitee-portal: next` / `classic` / `both` in `portal-openapi.yaml`. | New operations to tag `next`; `GET /user/notifications` to tag `both`. |

## 4. Target design

### 4.1 Data model

**`portal_notifications`** (Mongo `portalnotifications`, JDBC `portal_notifications`), new nullable fields:

| Field | Mongo | JDBC | Meaning |
| --- | --- | --- | --- |
| `readAt` | `readAt` | `read_at` timestamp | null = unread |
| `hook` | `hook` | `hook` nvarchar(64) | hook or notice name |
| `apiId` | `apiId` | `api_id` nvarchar(64) | API concerned, if any |
| `applicationId` | `applicationId` | `application_id` nvarchar(64) | application concerned, if any |
| `environmentId` | `environmentId` | `environment_id` nvarchar(64) | environment of the trigger; null for an organization-level trigger |

Index: (`user`, `environmentId`, `readAt`, `createdAt` desc) — Mongo `IndexUpgrader` next to
`portalnotifications/UserIndexUpgrader`, JDBC index in the changelog. Existing rows stay as they are: unread, no hook,
no API, no application, no environment (so visible in every environment, P10); they are not backfilled.

**`portal_notification_configs`**: new nullable `closeToExpiryDays` (JDBC `close_to_expiry_days` integer).
`NotificationReferenceType` gains `API_CONSUMER`: a row (`user`, `API_CONSUMER`, `apiId`) is one consumer's preference
on one API. It fits the current primary key. Notices are stored in the existing `hooks` list next to hook names.

**`portal_notification_notices`** (new, Mongo `portalnotificationnotices`, JDBC `portal_notification_notices`):
record of notices already sent.

| Field | Meaning |
| --- | --- |
| `id` | UUID |
| `user` | recipient |
| `notice` | `SUBSCRIPTION_CLOSE_TO_EXPIRY`, `APIKEY_CLOSE_TO_EXPIRY`, `CERTIFICATE_CLOSE_TO_EXPIRY`, `CERTIFICATE_EXPIRED` |
| `targetType` / `targetId` | `SUBSCRIPTION` / `API_KEY` / `CLIENT_CERTIFICATE` and its id |
| `thresholdDays` | days value in force when sent (0 for `CERTIFICATE_EXPIRED`) |
| `endsAt` | end date of the target, used for the purge |
| `notifiedAt` | send date |

Unique index (`user`, `notice`, `targetId`, `thresholdDays`); index on `endsAt`.

Migrations:

- **No data upgrader** (`io.gravitee.rest.api.service.Upgrader`, tracked in `upgrades`): fields are additive and
  nullable, the new collection/table starts empty, nothing is backfilled.
- **Mongo indexes**: two new repository `IndexUpgrader` components (idempotent index creation at startup, the existing
  mechanism): `portalnotifications` (`user`, `environmentId`, `readAt`, `createdAt` desc) in US 1;
  `portalnotificationnotices` unique (`user`, `notice`, `targetId`, `thresholdDays`) and `endsAt` in US 7. The
  existing `u1` index on `user` becomes a prefix of the new `portalnotifications` index and is redundant; it is kept in
  4.14 and noted for a later cleanup. JDBC has no index on `portal_notifications.user`, nothing to clean there.
- **JDBC**: one changelog file per story, each included in `master.yml`, numbered after the last `v4_14_0` file at merge
  time: `0X_portal_notifications_read_state.yml` (US 1), `0Y_portal_notification_configs_close_to_expiry_days.yml`
  (US 3), `0Z_portal_notification_notices.yml` (US 7). A merged changeset is never edited: its checksum is already
  recorded on dev environments and the nightly, and Liquibase refuses to start on a mismatch.

### 4.2 Hooks and templates

- `HookScope` gains `API_CONSUMER(true)`.
- New enum `ConsumerHook implements Hook` (scope `API_CONSUMER`) with: `APIKEY_EXPIRED`, `APIKEY_RENEWED`,
  `APIKEY_REVOKED`, `API_STARTED`, `API_STOPPED`, `API_DEPLOYED`, `API_DEPRECATED`, `API_UPDATED`,
  `SUBSCRIPTION_CLOSE_TO_EXPIRY`, `APIKEY_CLOSE_TO_EXPIRY`, `CERTIFICATE_EXPIRED`, `CERTIFICATE_CLOSE_TO_EXPIRY`.
  It is used only to render inbox rows; it is never passed to `NotifierService.trigger`.
- `NotificationTemplateServiceImpl.afterPropertiesSet` registers `ConsumerHook.values()`; email template loading is
  skipped for scope `API_CONSUMER` (portal only, no startup warning). Templates are listed and overridable in the
  organization notification templates like the others; the Console templates page gets a label and an icon for the
  new scope (display only).
- 12 files `templates/notifications/portal/API_CONSUMER.<HOOK>.yml`, consumer wording, no publisher name. Parameters:
  `api`, `application`, `plan`, `subscription`, `apiKey` (masked), `certificate` (name, end date), `daysBeforeExpiry`.
- Subscription events keep the existing `APPLICATION.SUBSCRIPTION_*` templates (already consumer-oriented, handle
  API and API Product).

### 4.3 Delivery routing (`NotifierServiceImpl.triggerPortalNotifications`)

Existing recipients are unchanged. Consumer recipients are added as follows:

| Trigger | Consumer recipients added | Template | Duplicate rule |
| --- | --- | --- | --- |
| `APPLICATION` reference, `SUBSCRIPTION_*` hook, `apiId` known | `API_CONSUMER` configs on `apiId` with that hook, kept only if the user is a member of that application | existing `APPLICATION.<HOOK>` | merged with the app recipients before the single `create`: one row per user |
| `API` reference, `SUBSCRIPTION_*` hook | none (handled by the application trigger above) | — | — |
| `API` reference, `APIKEY_*` hook | `API_CONSUMER` configs on the API with that hook, kept only if the user is a member of the key's application | `API_CONSUMER.<HOOK>` | users already notified as API members are skipped |
| `API` reference, lifecycle hook | `API_CONSUMER` configs on the API with that hook, kept only if the user subscribes to the API (§2.3) | `API_CONSUMER.<HOOK>` | users already notified as API members are skipped |
| `API_PRODUCT` reference | none (P9) | — | — |

Application membership and subscriber checks are computed once per event for the opted-in users only (no scan of
every member). The trigger is already `@Async`.

### 4.4 Reference extraction

`PortalNotificationService.create` fills `environmentId` from the `ExecutionContext` (null when it has no
environment), and receives the hook and fills `hook`, `apiId` and `applicationId` through one
`NotificationReferenceExtractor`: reads `params["api"]` and `params["application"]` for each type passed today
(`ApiEntity`, `GenericApiEntity`, `IndexableApi`, core `Api`, `ApiModel`, `ApplicationEntity`, core
`BaseApplicationEntity`, `ApplicationModel`, …), falls back to the trigger's reference id when the reference type is
`API` / `APPLICATION`; an unknown type gives null and a debug log.

### 4.5 Portal API (OpenAPI first, all new operations tagged `x-gravitee-portal: next`)

`GET /user/notifications` moves from `x-gravitee-portal: classic` to `both`, since the next-gen inbox and badge call
it. The other existing operations stay `classic`: `GET/PUT /applications/{applicationId}/notifications`,
`GET /applications/hooks`, `DELETE /user/notifications` and `DELETE /user/notifications/{notificationId}` (the
next-gen portal does not call them).

| Operation | Auth / checks | Request | Response |
| --- | --- | --- | --- |
| `GET /user/notifications` (extended) | authenticated, caller's rows of the current environment or with no environment (P10) | `page`, `size`, `read` (bool), `apiId`, `applicationId` | `data[]`: `id`, `title`, `message`, `created_at`, **`read`**, **`hook`**, **`api` {id, name}**, **`application` {id, name}**; `metadata.pagination.total` from the database |
| `GET /user/notifications/_filters` | authenticated, same scope | — | distinct `apis[]` and `applications[]` {id, name} referenced by the caller's notifications |
| `POST /user/notifications/{notificationId}/_read` | caller owns the notification and it is in scope, else 404 | — | 204 |
| `POST /user/notifications/_read` | authenticated, same scope | — | 204, marks all the caller's unread rows of that scope |
| `DELETE /user/notifications` (existing, now scoped) | authenticated, same scope | — | 204, deletes the caller's rows of that scope only |
| `DELETE /user/notifications/{notificationId}` (existing, now scoped) | caller owns the notification and it is in scope, else 404 | — | 204 |
| `GET /user/notifications/preferences/applications` | authenticated | — | applications where the caller holds `APPLICATION_NOTIFICATION` (P2): `id`, `name`, `enabledEvents` count |
| `GET /applications/{applicationId}/notifications/preferences` | `APPLICATION_NOTIFICATION[READ]`, app exists | — | `NotificationPreferences` |
| `PUT /applications/{applicationId}/notifications/preferences` | `APPLICATION_NOTIFICATION[UPDATE]` | `{ events[], closeToExpiryDays }` | `NotificationPreferences` |
| `GET /user/notifications/preferences/apis` | authenticated | — | APIs the caller subscribes to (§2.3): `api` {id, name, version}, `events[]`, `closeToExpiryDays` |
| `GET /apis/{apiId}/notifications/preferences` | authenticated, caller subscribes to the API, else 403 | — | `NotificationPreferences` |
| `PUT /apis/{apiId}/notifications/preferences` | same | `{ events[], closeToExpiryDays }` | `NotificationPreferences` |

A notification of another environment answers 404, like a notification of another user, so every operation sees
exactly what the list shows.

`NotificationPreferences`: `events[]` (enabled), `closeToExpiryDays`, `available[]` {`event`, `category`} from §2.2,
so the UI renders categories without hard-coding them.

PUT rules: every event must belong to the preference's column in §2.2 (400 otherwise); `closeToExpiryDays` 1 to 366
(400 otherwise), defaults to 30 when absent. Only the caller's `PortalNotificationConfig` is written; hooks already in
that config but outside §2.2 (e.g. set from the classic portal) are kept; email and webhook configs of the application
are never touched. The classic `GET/PUT /applications/{id}/notifications` is unchanged. An API name resolved for the
inbox comes from the current API; a deleted API or application returns `name: null` and the UI shows
"Deleted API" / "Deleted application".

Inbox names are resolved per page in one batch lookup for APIs and one for applications.

### 4.6 Code layout

- Core (`io.gravitee.apim.core.portal_notification`): use cases `SearchUserPortalNotificationsUseCase`,
  `MarkPortalNotificationsReadUseCase`, `GetUserNotificationFiltersUseCase`,
  `Get/UpdateApplicationNotificationPreferencesUseCase`, `Get/UpdateApiNotificationPreferencesUseCase`,
  `ListUserNotificationPreferencesUseCase`; domain services `NotificationPreferenceValidator` (§2.2, days) and
  `ApiSubscriberDomainService` (§2.3: `isSubscriber(user, apiId)`, `subscribedApiIds(user)`,
  `filterSubscribers(users, apiId)`, `filterApplicationMembers(users, applicationId)`); query/crud service ports with
  infra adapters over the repositories, in-memory alternatives for tests.
- Portal REST: `UserNotificationsResource` (extended), new `UserNotificationPreferencesResource`,
  `ApplicationNotificationPreferencesResource` (sub-resource of `ApplicationResource`),
  `ApiNotificationPreferencesResource` (sub-resource of the Portal `ApiResource`), mappers.
- Repository: API model and criteria, Mongo and JDBC implementations, TCK test data for both.
- Portal-next: `services/portal-notification.service.ts`, `services/notification-preferences.service.ts`,
  `entities/notification/*`, `app/dashboard/notifications/` (inbox, preferences with Apps and APIs tabs),
  `components/notification-bell/`.

### 4.7 Cleanup

- API deleted: `portalNotificationConfigService.deleteReference(API_CONSUMER, apiId)` next to the existing `API`
  cleanup in `service/impl/ApiServiceImpl` (~l.1726), `service/v4/impl/ApiServiceImpl` (~l.731) and
  `DeleteEnvironmentCommandHandler` (~l.527).
- User deleted: `deleteByUser` already removes every config type; the notices of that user are deleted too.
- Subscription closed: the preference is kept (it is ignored at delivery while the user does not subscribe) and the
  API disappears from the APIs tab and the bell; re-subscribing restores it.
- Notice records: purged by the daily job once `endsAt` is more than 1 day in the past.

## 5. User stories

### US 1 — Inbox (PN-12 to PN-18) · 3 days (backend 2, frontend 1)

Backend:
- `PortalNotification` + Mongo/JDBC: `readAt`, `hook`, `apiId`, `applicationId`, `environmentId`; Mongo
  `IndexUpgrader`; changelog `0X_portal_notifications_read_state.yml`.
- `PortalNotificationRepository`: `search(PortalNotificationSearchCriteria{user, environmentId, read, apiId,
  applicationId}, Pageable)` returning a `Page` sorted by `createdAt` desc, where `environmentId` matches that
  environment or null; `markAsRead(user, environmentId, id, date)` and `delete(user, environmentId, id)` returning
  whether a row was updated / deleted (no row for another user or another environment → 404, in one query; the
  resource no longer checks ownership through `findByUser`); `markAllAsRead(user, environmentId, date)`;
  `findDistinctReferences(user, environmentId)` (API and application ids); `deleteAll(user, environmentId)` added next
  to `deleteAll(user)`, which stays for the Console (organization-wide) and user deletion. `update()` keeps throwing.
- `NotificationReferenceExtractor` (§4.4); `PortalNotificationServiceImpl.create` stores `hook`, `apiId`,
  `applicationId`, `environmentId`.
- Use cases and `UserNotificationsResource`: list with filters and server paging, `_filters`, `{id}/_read`, `_read`;
  the existing `DELETE` and `DELETE {id}` scoped the same way (§4.5). The classic portal keeps working: same response,
  additional fields only, same paging parameters; its list and deletions are now limited to the current environment
  (plus rows with no environment).
- Console `UserNotificationsResource` (Management API) unchanged.

Frontend:
- Route `dashboard/notifications` (lazy), entry in the dashboard navigation.
- Table: date, type (hook label), API, application, title; unread rows in bold.
- Filters: API, application (from `_filters`), status (All / Unread / Read); "Clear filters" resets all three; filters
  in the query string.
- Clicking a row opens the message (title + message) in a side panel; an unread row is marked read
  (`POST {id}/_read`) and the table updates without reload. "Mark all as read" button.
- Empty state when there is no notification or no match.

Tests:
- TCK (Mongo, JDBC): each filter alone and combined, environment scope (current environment and null kept, another
  environment excluded, also for mark all read, delete all and distinct references), paging and total, sort, mark
  read and delete by id (own row updated / deleted; another user's row and another environment's row untouched, `false`
  returned), mark all read, distinct references, legacy row without new fields.
- Extractor: one parameterized test listing every param type passed today (a new type without a case fails).
- Service: `create` stores hook, references and environment for an API hook, an application hook and a portal hook (no
  references); a context without environment stores null.
- Resource: filters passed through, 404 on another user's notification and on a notification of another environment
  (`_read` and `DELETE {id}`), 204 on read, total in metadata, `DELETE` leaves other environments' rows.
- Regression: Console user notification list (Management API, organization-wide) returns the same content as before;
  classic portal list returns the same fields, limited to the environment scope.
- Jest: table rendering, each filter, clear filters, open = read, mark all as read, "Deleted API" label, empty state.

### US 2 — Unread badge (PN-11) · 0.5 day (frontend)

- `PortalNotificationService.unreadCount` signal: `GET /user/notifications?read=false&size=1` →
  `metadata.pagination.total`.
- Polling every `configuration.scheduler.notificationsInSeconds` seconds while the user is logged in; no polling when
  the value is absent or 0; refresh on avatar menu open and after any read action.
- `user-avatar`: "Notifications" menu entry with the count (badge), routing to `dashboard/notifications`; badge also
  on the avatar button when count > 0. Same entry in the mobile navigation.

The count follows the environment scope of the list (P10): no extra parameter, the badge of an environment never counts
another environment's notifications.

Tests (Jest): count displayed, hidden at 0, refreshed on menu open, refreshed after mark read, polling interval
respected, no call when logged out, count = the total returned for the current environment.

### US 3 — App preferences (PN-01 to PN-05) · 1.5 days (backend 0.5, frontend 1)

Backend:
- `PortalNotificationConfig` + Mongo/JDBC: `closeToExpiryDays`; changelog
  `0Y_portal_notification_configs_close_to_expiry_days.yml`.
- `NotificationPreferenceValidator` with the App column of §2.2.
- `GET /user/notifications/preferences/applications` (`applicationService.findIdsByUserAndPermission` on
  `APPLICATION_NOTIFICATION` READ), `GET/PUT /applications/{applicationId}/notifications/preferences` (§4.5).

Frontend:
- Route `dashboard/notifications/preferences`, tab **Apps**.
- Searchable application selector (autocomplete over the list endpoint).
- Checkboxes grouped by category (Subscription, Certificate) with "select all" per category; days input shown when a
  close-to-expiry event is checked, validated 1 to 366; Save; success and error messages.

Tests:
- Resource: 403 without `APPLICATION_NOTIFICATION`, 404 unknown app, 400 on an API-only event (`API_STARTED`), 400 on
  days 0 / 367, default 30, hooks outside §2.2 preserved, generic (email/webhook) configs unchanged, other users'
  configs unchanged.
- TCK: `closeToExpiryDays` round trip.
- Jest: app search, categories, select all, days input visibility and validation, save payload.

### US 4 — API preferences (PN-06 to PN-10) · 2.5 days (backend 1.5, frontend 1)

Backend:
- `NotificationReferenceType.API_CONSUMER`; every `switch` on the enum reviewed.
- `ApiSubscriberDomainService` (§2.3): applications of the user (`findIdsByUser`, groups included) → subscriptions with
  statuses `ACCEPTED`, `PAUSED`, `PENDING` and reference type API → API ids.
- `GET /user/notifications/preferences/apis`, `GET/PUT /apis/{apiId}/notifications/preferences` (§4.5) with the API
  column of §2.2. The PR is flagged security-sensitive: authenticated access with a subscription check instead of a
  role permission.
- Cleanup on API deletion (§4.7, three paths).

Frontend:
- Tab **APIs**: searchable API selector over `GET /user/notifications/preferences/apis` (no application selector);
  categories Subscription, API key, Lifecycle; days input for the close-to-expiry events.
- Empty state with a link to the catalog when the user subscribes to no API.

Tests:
- Domain service: member of a subscribed app, group member, closed subscription excluded, rejected excluded,
  API Product subscription excluded, another environment excluded.
- Resource: 403 without subscription, 403 with an API Product subscription only, 400 on `CERTIFICATE_EXPIRED`, 400
  days, list returns subscribed APIs only.
- TCK: `API_CONSUMER` config create / find by reference and hook / delete by reference.
- API deletion removes the `API_CONSUMER` rows on the three paths.
- Jest: API list, categories, save, empty state.

### US 5 — Events from my APIs (PN-09, PN-20) · 3 days (backend)

- `ConsumerHook`, `HookScope.API_CONSUMER`, template registration, 8 templates for API-key and lifecycle events
  (§4.2), Console template page label.
- Routing in `NotifierServiceImpl.triggerPortalNotifications` (§4.3), using `ApiSubscriberDomainService` and
  application membership (direct + groups).
- `PortalNotificationService.create` called once for API members (existing template) and once for consumer
  recipients (`ConsumerHook` template) on API-key and lifecycle hooks; once with the merged list on subscription
  hooks.

Tests (written before any change to the notifier):
- Regression: for every existing hook, inbox recipients, email and webhook calls are unchanged when no `API_CONSUMER`
  config exists.
- Subscription: an opted-in subscriber gets the event; a user opted in on the API but not a member of that application
  does not (another consumer's subscription); app + API preferences → one row; product subscription → no consumer row.
- API key: only members of the key's application; API member who is also a consumer → one row.
- Lifecycle: subscriber gets it with consumer wording; closed or rejected subscription → nothing; `API_UPDATED`
  reaches a subscriber without the publisher's name.
- Templates: each `API_CONSUMER.*` template renders with its parameters.

### US 6 — Catalog bell (PN-19 to PN-23) · 0.5 day (frontend)

- `components/notification-bell/`: input API id + preference state; outlined grey when off, filled primary (blue)
  when ≥ 1 event on; tooltip "Notify me" / "Notifications on"; `aria-pressed`.
- `catalog.component`: one `GET /user/notifications/preferences/apis` per catalog load for a logged-in user; the bell
  is rendered on `app-api-card` (grid) and in the list view only for APIs in that list; never on API Product cards;
  never for anonymous users.
- Click on grey: `PUT /apis/{apiId}/notifications/preferences` with the PN-20 events + the existing close-to-expiry
  events and days. Click on blue: `PUT` with no event. Optimistic toggle, rollback and error message on failure.

Tests (Jest): hidden when not subscribed, hidden for anonymous, hidden on API Products, grey/blue state, partial
selection shows blue, on keeps close-to-expiry settings, off sends an empty list, rollback on error, grid and list
views.

### US 7 — Close to expiry (PN-04, PN-09) · 2.5 days (backend)

- Repository `PortalNotificationNoticeRepository` (Mongo, JDBC, TCK): `create`, `existsAtOrBelowThreshold(user,
  notice, targetId, threshold)`, `deleteByEndsAtBefore(date)`, `deleteByUser(user)`; Mongo `IndexUpgrader`, JDBC
  changelog `0Z_portal_notification_notices.yml`.
- `PortalNotificationConfigRepository.findByReferenceTypeAndHook(referenceType, hook)` (all organizations).
- `ScheduledConsumerExpiryNoticeService` in the `services` layer: daily cron at 03:00, scheduled on every node, runs
  only when `clusterManager.self().primary()` (same pattern as `ScheduledAuditCleanerService`), enabled by the same
  `services.subscription.enabled` switch.
- Per run:
  - App preferences with `SUBSCRIPTION_CLOSE_TO_EXPIRY`: subscriptions of the application in `ACCEPTED` / `PAUSED`
    with `endingAt` set.
  - API preferences with `SUBSCRIPTION_CLOSE_TO_EXPIRY`: same, restricted to the user's applications subscribed to
    that API (§2.3).
  - API preferences with `APIKEY_CLOSE_TO_EXPIRY`: non-revoked, non-expired keys with `expireAt` set of those
    subscriptions; a shared key gives one notice per user and key.
  - Remaining days = `ceil((end - now) / 1 day)`; notify when `0 < remaining ≤ closeToExpiryDays` and no record exists
    for (user, notice, target) with a threshold ≤ the current one. Consequences: a missed run still notifies on the
    next run; a lowered threshold notifies again when reached; a raised threshold does not re-notify.
  - Write the inbox row with `PortalNotificationService.create(ctx, ConsumerHook.<NOTICE>, [user], params)` with
    `hook`, `apiId`, `applicationId` set, in the organization of the preference and the environment of the
    application; then write the record. No `NotifierService.trigger`, no email, no webhook.
  - Purge records whose `endsAt` is more than 1 day past.
- Templates `API_CONSUMER.SUBSCRIPTION_CLOSE_TO_EXPIRY` and `API_CONSUMER.APIKEY_CLOSE_TO_EXPIRY` with
  `daysBeforeExpiry`.

Tests:
- Job: notifies at ≤ threshold once; nothing above threshold; missed day still notifies; no duplicate after restart
  (record present); lowered threshold notifies again; raised threshold does not; closed subscription and revoked key
  silent; two users with 10 and 30 days notified on their own days; not primary → no work; no email or webhook
  called; purge.
- TCK for the notice repository and the new config query.

### US 8 — Certificates (PN-04) · 1.5 days (backend)

- Same job and store. App preferences with `CERTIFICATE_CLOSE_TO_EXPIRY` / `CERTIFICATE_EXPIRED`: client certificates
  of the application in `ACTIVE`, `ACTIVE_WITH_END` or `SCHEDULED`.
- Certificate end = the earlier of `endsAt` and `certificateExpiration` when both are set, whichever is set
  otherwise; no end → ignored.
- Close to expiry: same rule as US 7 with the app's `closeToExpiryDays`.
- Expired: `end ≤ now`, end after the preference's `createdAt` (no backlog when opting in), no `CERTIFICATE_EXPIRED`
  record for (user, certificate); threshold stored as 0.
- Recipients: the user of the preference (preferences are already per user and application).
- Templates `API_CONSUMER.CERTIFICATE_CLOSE_TO_EXPIRY` and `API_CONSUMER.CERTIFICATE_EXPIRED` (certificate name, end
  date, application).

Tests: expired on the end date, once; close to expiry once; earlier of the two dates wins; only `endsAt` / only
`certificateExpiration`; no date → ignored; revoked certificate ignored; certificate expired before opting in → no
notice.

### Docs (along the stories)

Consumer notifications page (inbox, preferences, bell), list of events per preference (§2.2), notices and their
templates, next-gen Portal API reference, release note, known limits (§7).

## 6. Schedule

| Days | Story | Side |
| --- | --- | --- |
| 1–3 | US 1 Inbox | backend + frontend |
| 4 (½) | US 2 Unread badge | frontend |
| 4–5 | US 3 App preferences | backend + frontend |
| 6–8 | US 4 API preferences | backend + frontend |
| 8–10 | US 5 Events from my APIs | backend |
| 11 (½) | US 6 Catalog bell | frontend |
| 11–13 | US 7 Close to expiry | backend |
| 14–15 | US 8 Certificates | backend |

Day 1: the Portal API contract of §4.5 is merged in `portal-openapi.yaml` so the frontend works on mocks. Each story
starts while the previous PR is in review. US 8 is last and is the one that moves to a follow-up if the schedule
slips. Most likely overruns: US 5 (shared fan-out with email and webhook) and US 7 (new repository entity on Mongo
and JDBC).

Dependencies: the next-gen Portal API documentation split (`x-gravitee-portal`) is in place; the bell touches
`api-card`, which the GMD catalog cards work replaces — whichever lands second ports the bell.

## 7. Risks and known limits

- US 5 changes who receives inbox rows from `NotifierServiceImpl`, which also feeds email and webhook: regression
  tests on every existing hook land before the change.
- `portal_notifications` is shared with the classic portal and the Console user notifications: additive fields only,
  no purge, regression tests on both lists.
- `API_UPDATED` produces one inbox row per opted-in subscriber on every publisher save.
- Lifecycle events are not fired when a system or unauthenticated actor triggers them (`ApiNotificationServiceImpl`),
  e.g. some automated deploys: unchanged in 4.14, documented.
- Existing notifications have no API, application, type or environment: they show as unread with empty columns in
  every environment and do not match API / application filters.
- The classic portal shares `GET` and `DELETE /user/notifications`: its list and deletions become environment-scoped
  (P10), a visible change for users of several environments.
- API Products get no consumer preference or bell (P9) until Console offers API Product notifications.

## 8. Security, compatibility, impact

- Security-sensitive: inbox endpoints are scoped to the caller and the environment (ownership and environment check
  on `{notificationId}`); app preferences check `APPLICATION_NOTIFICATION`; API preferences check the subscription
  rule; delivery re-checks membership and subscription at send time; preferences and days are validated.
- Breaking API changes: none. Portal API and database changes are additive, no data upgrader (Mongo indexes through
  `IndexUpgrader`, JDBC through one changelog per story), the classic preference endpoints are unchanged, nothing
  changes for a user until they opt in.
- Behavior change: the user notification list, mark-all-read and deletions of the Portal API are scoped to the current
  environment (P10); the Console user notifications stay organization-wide.
- Settings and flags: none new; available where the next-gen portal is enabled (`portalNext.access.enabled`).
- Console / Gamma: display-only label for the new template scope; no new application or API hook.
- Gateway: none.
- Automation API / CRD: none. `ApiHook` and the CRD `consoleNotification` enum are unchanged; `API_CONSUMER` and
  `ConsumerHook` are not exposed through the Automation API.

## 9. Out of scope

Publisher-side notification settings; email or webhook notifiers created by consumers; Alert Engine and runtime
alerts; portal-admin, support, rating and review events; changes to the global pre-expiration email schedule; API
Product notifications; retention purge of `portal_notifications`; the `recipients` argument ignored by
`NotifierServiceImpl.trigger(ctx, ApplicationHook, appId, params, recipients)` (separate bug).
