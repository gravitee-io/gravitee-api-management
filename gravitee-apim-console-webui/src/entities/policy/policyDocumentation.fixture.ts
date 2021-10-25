/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { PolicyDocumentation } from './policyDocumentation';

export function fakePolicyDocumentation(attributes?: PolicyDocumentation): PolicyDocumentation {
  const base: PolicyDocumentation = `
= RateLimit policy


ifdef::env-github[]
image:https://ci.gravitee.io/buildStatus/icon?job=gravitee-io/gravitee-policy-ratelimit/master["Build status", link="https://ci.gravitee.io/job/gravitee-io/job/gravitee-policy-ratelimit/"]
image:https://badges.gitter.im/Join Chat.svg["Gitter", link="https://gitter.im/gravitee-io/gravitee-io?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge"]
endif::[]

== Phase

|===
|onRequest |onResponse

| X
|

|===

== Description

There are three \`rate-limit\` policies:

* Quota: configures the number of requests allowed over a period of time (hours, days, weeks, months)
* Rate-Limit: configures the number of requests allowed over a limited period of time (seconds, minutes)
* Spike-Arrest: throttles the number of requests processed and sends them to the backend to avoid a spike

== Configuration

You can configure the policies with the following options:

=== Quota

The Quota policy configures the number of requests allowed over a large period of time (from hours to months).
This policy does not prevent request spikes.

|===
|Property |Required |Description |Type |Default

|key
|No
|Key to identify a consumer to apply the quota against. Leave it empty to apply the default behavior (plan/subscription pair). Supports Expression Language.
|String
|null

|limit
|No
|Static limit on the number of requests that can be sent (this limit is used if the value > 0).
|integer
|0

|dynamicLimit
|No
|Dynamic limit on the number of requests that can be sent (this limit is used if static limit = 0). The dynamic value is based on Expression Language expressions.
|string
|null

|periodTime
|Yes
|Time duration
|Integer
|1

|periodTimeUnit
|Yes
|Time unit (\`HOURS\`, \`DAYS\`, \`WEEKS\`, \`MONTHS\`)
|String
|MONTHS

|===

==== Configuration example

[source, json]
----
  "quota": {
    "limit": "1000",
    "periodTime": 1,
    "periodTimeUnit": "MONTHS"
  }
----

=== Rate-Limit

The Rate-Limit policy configures the number of requests allow over a limited period of time (from seconds to minutes).
This policy does not prevent request spikes.

|===
|Property |Required |Description |Type |Default

|key
|No
|Key to identify a consumer to apply rate-limiting against. Leave it empty to use the default behavior (plan/subscription pair). Supports Expression Language.
|String
|null

|limit
|No
|Static limit on the number of requests that can be sent (this limit is used if the value > 0).
|integer
|0

|dynamicLimit
|No
|Dynamic limit on the number of requests that can be sent (this limit is used if static limit = 0). The dynamic value is based on Expression Language expressions.
|string
|null

|periodTime
|Yes
|Time duration
|Integer
|1

|periodTimeUnit
|Yes
|Time unit ("SECONDS", "MINUTES" )
|String
|SECONDS

|===

==== Configuration example

[source, json]
----
  "rate": {
    "limit": "10",
    "periodTime": 10,
    "periodTimeUnit": "MINUTES"
  }
----

=== Spike Arrest

The Spike-Arrest policy configures the number of requests allow over a limited period of time (from seconds to minutes).
This policy prevents request spikes by throttling incoming requests.
For example, a SpikeArrest policy configured to 2000 requests/second will limit the execution of simultaneous requests to 200 requests per 100ms.

By default, the SpikeArrest policy is applied to a plan, not a consumer. To apply a spike arrest to a consumer, you need to use the \`key\` attribute, which supports Expression Language.

|===
|Property |Required |Description |Type |Default

|key
|No
|Key to identify a consumer to apply spike arresting against. Leave it empty to use the default behavior. Supports Expression Language (example: \`{#request.headers['x-consumer-id']}\`).
|String
|null

|limit
|No
|Static limit on the number of requests that can be sent (this limit is used if the value > 0).
|integer
|0

|dynamicLimit
|No
|Dynamic limit on the number of requests that can be sent (this limit is used if static limit = 0). The dynamic value is based on Expression Language expressions.
|string
|null

|periodTime
|Yes
|Time duration
|Integer
|1

|periodTimeUnit
|Yes
|Time unit (\`SECONDS\`, \`MINUTES\`)
|String
|SECONDS

|===

==== Configuration example

[source, json]
----
  "spike": {
    "limit": "10",
    "periodTime": 10,
    "periodTimeUnit": "MINUTES"
  }
----

== Errors

=== Default response override

You can use the response template feature to override the default response provided by the policies. These templates must be defined at the API level (see the API Console *Response Templates*
option in the API *Proxy* menu).

=== Error keys

The error keys sent by these policies are as follows:

[cols="2*", options="header"]
|===
^|Key
^|Parameters

.^|RATE_LIMIT_TOO_MANY_REQUESTS
^.^|limit - period_time - period_unit

.^|QUOTA_TOO_MANY_REQUESTS
^.^|limit - period_time - period_unit

.^|SPIKE_ARREST_TOO_MANY_REQUESTS
^.^|limit - period_time - period_unit - slice_limit - slice_period_time - slice_limit_period_unit

|===
`;

  return attributes ?? base;
}
