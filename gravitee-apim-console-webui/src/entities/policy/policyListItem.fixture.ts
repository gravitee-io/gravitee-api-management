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
import { PolicyListItem } from './policyListItem';

export function fakePolicyListItem(attributes?: Partial<PolicyListItem>): PolicyListItem {
  const base: PolicyListItem = {
    category: 'transformation',
    description: 'Description of the JSON to JSON Transformation Gravitee Policy',
    icon:
      'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAuNzEgMTEyLjgyIj48ZGVmcz48c3R5bGU+LmNscy0xe2ZpbGw6Izg2YzNkMDt9LmNscy0ye2ZpbGw6I2ZmZjt9LmNscy0ze2ZvbnQtc2l6ZToxMnB4O2ZpbGw6IzFkMWQxYjtmb250LWZhbWlseTpNeXJpYWRQcm8tUmVndWxhciwgTXlyaWFkIFBybzt9LmNscy00e2xldHRlci1zcGFjaW5nOi0wLjAxZW07fTwvc3R5bGU+PC9kZWZzPjxnIGlkPSJBUEkiPjxwYXRoIGNsYXNzPSJjbHMtMSIgZD0iTTUwLjM1LDEzLjM3YTQzLDQzLDAsMSwwLDQzLjA1LDQzQTQzLDQzLDAsMCwwLDUwLjM1LDEzLjM3WiIvPjxwYXRoIGNsYXNzPSJjbHMtMiIgZD0iTTYzLjgsNzcuMDlhNC44Myw0LjgzLDAsMSwxLDMuNjktNy45Miw0Ljc0LDQuNzQsMCwwLDEsMS4xMywzLjA5QTQuODMsNC44MywwLDAsMSw2My44LDc3LjA5Wm0wLTcuOTNhMy4xMSwzLjExLDAsMSwwLDIuMzgsMS4xMkEzLjEsMy4xLDAsMCwwLDYzLjgsNjkuMTZaIi8+PHBhdGggY2xhc3M9ImNscy0yIiBkPSJNNTEuNSw0My42OGE0Ljg1LDQuODUsMCwwLDEtNC43Ny00LjE0LDUsNSwwLDAsMSwwLS42OSw0LjgzLDQuODMsMCwwLDEsOS42NSwwLDMuNDUsMy40NSwwLDAsMS0uMDcuNzRBNC44MSw0LjgxLDAsMCwxLDUxLjUsNDMuNjhabTAtNy45M2EzLjEsMy4xLDAsMCwwLTMuMSwzLjEsMy4yNSwzLjI1LDAsMCwwLDAsLjQ0LDMuMSwzLjEsMCwwLDAsNi4xNCwwLDIuNjYsMi42NiwwLDAsMCwwLS40NUEzLjExLDMuMTEsMCwwLDAsNTEuNSwzNS43NVoiLz48cGF0aCBjbGFzcz0iY2xzLTIiIGQ9Ik0zMy42NCw3MC4xNWE0LjgzLDQuODMsMCwwLDEtMS4zNy05LjQ2LDUsNSwwLDAsMSwxLjM3LS4xOSw0LjgyLDQuODIsMCwwLDEsNC44Miw0LjgyLDQuODcsNC44NywwLDAsMS0yLjcxLDQuMzVBNC43Myw0LjczLDAsMCwxLDMzLjY0LDcwLjE1Wm0wLTcuOTNhMywzLDAsMCwwLS44OS4xMywzLjEsMy4xLDAsMCwwLC44OSw2LjA4QTMsMywwLDAsMCwzNSw2OC4xMmEzLjExLDMuMTEsMCwwLDAtMS4zNS01LjlaIi8+PHBhdGggY2xhc3M9ImNscy0yIiBkPSJNMzIuMTgsNTYuMzVoLS4xMmEuODcuODcsMCwwLDEtLjczLTFBMjAuNDcsMjAuNDcsMCwwLDEsNDIsNDAuMzNhLjg2Ljg2LDAsMSwxLC44LDEuNTJBMTguNzcsMTguNzcsMCwwLDAsMzMsNTUuNjEuODYuODYsMCwwLDEsMzIuMTgsNTYuMzVaIi8+PHBhdGggY2xhc3M9ImNscy0yIiBkPSJNNTEuNSw3OC43OWEyMC4xNywyMC4xNywwLDAsMS0xMi4yNy00LjExLjg2Ljg2LDAsMCwxLS4xNy0xLjIxLjg1Ljg1LDAsMCwxLDEuMi0uMTZBMTguNTIsMTguNTIsMCwwLDAsNTEuNSw3Ny4wNywxOC4yMywxOC4yMywwLDAsMCw1NSw3Ni43M2EuODcuODcsMCwwLDEsLjMzLDEuN0EyMC44NywyMC44NywwLDAsMSw1MS41LDc4Ljc5WiIvPjxwYXRoIGNsYXNzPSJjbHMtMiIgZD0iTTcwLDY1LjQyYS44NC44NCwwLDAsMS0uMjcsMCwuODYuODYsMCwwLDEtLjU0LTEuMDksMTguNzEsMTguNzEsMCwwLDAtOC41NS0yMi4xNy44Ni44NiwwLDEsMSwuODUtMS41LDIwLjQyLDIwLjQyLDAsMCwxLDkuMzMsMjQuMjFBLjg3Ljg3LDAsMCwxLDcwLDY1LjQyWiIvPjwvZz48ZyBpZD0iSE9TVElORyI+PHRleHQgY2xhc3M9ImNscy0zIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSgxNy45MyAtNC45OCkiPkpTT04gPHRzcGFuIGNsYXNzPSJjbHMtNCIgeD0iMjkuMDYiIHk9IjAiPnQ8L3RzcGFuPjx0c3BhbiB4PSIzMi45NiIgeT0iMCI+byBKU09OPC90c3Bhbj48L3RleHQ+PC9nPjwvc3ZnPg==',
    id: 'json-to-json',
    name: 'JSON to JSON Transformation',
    onRequest: true,
    onResponse: true,
    schema:
      '{\n  "type" : "object",\n  "id" : "urn:jsonschema:io:gravitee:policy:json2json:configuration:JsonToJsonTransformationPolicyConfiguration",\n  "properties" : {\n    "scope" : {\n      "title": "Scope",\n      "description": "Execute policy on <strong>request</strong> or <strong>response</strong> phase.",\n      "type" : "string",\n      "default": "REQUEST",\n      "enum" : [ "REQUEST", "RESPONSE" ],\n      "deprecated": "true"\n    },\n    "overrideContentType" : {\n      "title": "Override the Content-Type",\n      "description": "Enforce the Content-Type: application/json",\n      "type" : "boolean",\n      "default": true\n    },\n    "specification" : {\n      "title": "JOLT specification",\n      "type" : "string",\n      "x-schema-form": {\n        "type": "codemirror",\n        "codemirrorOptions": {\n          "placeholder": "Place your JOLT specification here or drag\'n\'drop your JOLT specification file",\n          "lineWrapping": true,\n          "lineNumbers": true,\n          "allowDropFileTypes": true,\n          "autoCloseTags": true,\n          "mode": "javascript"\n        },\n        "expression-language": true\n      }\n    }\n  },\n  "required": [\n    "specification"\n  ]\n}\n',
    version: '1.6.0',
  };

  return {
    ...base,
    ...attributes,
  };
}
