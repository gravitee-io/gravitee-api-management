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
import { ResourceListItem } from './resourceListItem';

export function fakeResourceListItem(attributes?: Partial<ResourceListItem>): ResourceListItem {
  const base: ResourceListItem = {
    description:
      'The resource is used to maintain a cache and link it to the API lifecycle. It means that the cache is initialized when the API is starting and released when API is stopped.',
    icon: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAuNzEgMTEyLjgyIj48ZGVmcz48c3R5bGU+LmNscy0xe2ZvbnQtc2l6ZToxMnB4O2ZpbGw6IzFkMWQxYjtmb250LWZhbWlseTpNeXJpYWRQcm8tUmVndWxhciwgTXlyaWFkIFBybzt9LmNscy0ye2xldHRlci1zcGFjaW5nOi0wLjAxZW07fS5jbHMtM3tmaWxsOiM4NmMzZDA7fS5jbHMtNHtmaWxsOiNmZmY7c3Ryb2tlOiNmZmY7c3Ryb2tlLW1pdGVybGltaXQ6MTA7fTwvc3R5bGU+PC9kZWZzPjxnIGlkPSJIT1NUSU5HIj48dGV4dCBjbGFzcz0iY2xzLTEiIHRyYW5zZm9ybT0idHJhbnNsYXRlKDE3LjkzIC00Ljk4KSI+PHRzcGFuIGNsYXNzPSJjbHMtMiI+QzwvdHNwYW4+PHRzcGFuIHg9IjYuODgiIHk9IjAiPmFjaGU8L3RzcGFuPjwvdGV4dD48cmVjdCBjbGFzcz0iY2xzLTMiIHg9IjUiIHk9IjExIiB3aWR0aD0iOTAiIGhlaWdodD0iOTAiIGZpbGw9InRyYW5zcGFyZW50IiBzdHJva2Utd2lkdGg9IjUiLz48cGF0aCBjbGFzcz0iY2xzLTQiIGQ9Ik00Ny40NSw3Ny41OEEyMy4zLDIzLjMsMCwwLDEsNDIsNzYuOTNMNDIuNDYsNzVhMjEuMzQsMjEuMzQsMCwxLDAtMTEtMzQuODNsLTEuNS0xLjMyQTIzLjM0LDIzLjM0LDAsMSwxLDQ3LjQ1LDc3LjU4WiIvPjxwb2x5Z29uIGNsYXNzPSJjbHMtNCIgcG9pbnRzPSI0Ni4yOCA3MC44OSAzNC40MSA3Mi4zNyA0MS42MyA4MS45MSA0Ni4yOCA3MC44OSIvPjwvZz48L3N2Zz4K',
    id: 'cache',
    name: 'Cache',
    schema:
      '{\n  "type" : "object",\n  "id" : "urn:jsonschema:io:gravitee:resource:cache:configuration:CacheResourceConfiguration",\n  "properties" : {\n    "name" : {\n      "title": "Cache name",\n      "description": "The name of the cache.",\n      "type" : "string",\n      "default": "my-cache"\n    },\n    "timeToIdleSeconds" : {\n      "title": "Time to idle (in seconds)",\n      "type" : "integer",\n      "description": "The maximum number of seconds an element can exist in the cache without being accessed. The element expires at this limit and will no longer be returned from the cache. The default value is 0, which means no timeToIdle (TTI) eviction takes place (infinite lifetime).",\n      "default": 0,\n      "minimum": 0\n    },\n    "timeToLiveSeconds" : {\n      "title": "Time to live (in seconds)",\n      "type" : "integer",\n      "description": "The maximum number of seconds an element can exist in the cache regardless of use. The element expires at this limit and will no longer be returned from the cache. The default value is 0, which means no timeToLive (TTL) eviction takes place (infinite lifetime).",\n      "default": 0,\n      "minimum": 0\n    },\n    "maxEntriesLocalHeap" : {\n      "title": "Max entries on heap",\n      "description": "The maximum objects to be held in local heap memory (0 = no limit).",\n      "type" : "integer",\n      "default": 1000,\n      "minimum": 0\n    }\n  },\n  "required": [\n    "name",\n    "timeToIdleSeconds",\n    "timeToLiveSeconds",\n    "maxEntriesLocalHeap",\n    "timeToLiveSeconds"\n  ]\n}\n',
    version: '1.6.1',
  };

  return {
    ...base,
    ...attributes,
  };
}
