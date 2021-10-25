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
import { ResourceDocumentation } from './resourceDocumentation';

export function fakeResourceDocumentation(attributes?: ResourceDocumentation): ResourceDocumentation {
  const base: ResourceDocumentation = `
= Cache Resource

ifdef::env-github[]
image:https://ci.gravitee.io/buildStatus/icon?job=gravitee-io/gravitee-resource-cache/master["Build status", link="https://ci.gravitee.io/job/gravitee-io/job/gravitee-resource-cache/"]
image:https://badges.gitter.im/Join Chat.svg["Gitter", link="https://gitter.im/gravitee-io/gravitee-io?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge"]
endif::[]

== Description

The cache resource is used to maintain a cache and link it to the API lifecycle.
It means that the cache is initialized when the API is starting and released when API is stopped.

This cache is responsible to store HTTP response from the backend to avoid subsequent calls.

Current implementation of the cache resource is based on https://hazelcast.com/[Hazelcast].

== Configuration

You can configure the resource with the following options :

|===
|Property |Required |Description |Type |Default

.^|name
^.^|X
|The name of the cache.
^.^|string
^.^|my-cache

.^|timeToIdleSeconds
^.^|X
|The maximum number of seconds an element can exist in the cache without being accessed. The element expires at this limit and will no longer be returned from the cache. The default value is 0, which means no timeToIdle (TTI) eviction takes place (infinite lifetime).
^.^|integer
^.^|0

.^|timeToLiveSeconds
^.^|X
|The maximum number of seconds an element can exist in the cache regardless of use. The element expires at this limit and will no longer be returned from the cache. The default value is 0, which means no timeToLive (TTL) eviction takes place (infinite lifetime).
^.^|integer
^.^|0

.^|maxEntriesLocalHeap
^.^|X
|The maximum objects to be held in local heap memory (0 = no limit).
^.^|integer
^.^|1000

|===


[source, json]
.Configuration example
----
{
    "name" : "cache",
    "type" : "cache",
    "enabled" : true,
    "configuration" : {
        "name": "my-cache",
        "timeToIdleSeconds":0,
        "timeToLiveSeconds":0,
        "maxEntriesLocalHeap":1000
    }
}
----`;

  return attributes ?? base;
}
