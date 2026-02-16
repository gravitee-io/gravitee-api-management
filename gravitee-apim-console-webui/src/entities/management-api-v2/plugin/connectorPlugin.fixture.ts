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
import { ConnectorPlugin } from './connectorPlugin';

export function fakeConnectorPlugin(modifier?: Partial<ConnectorPlugin>): ConnectorPlugin {
  const base: ConnectorPlugin = {
    id: 'http-proxy',
    name: 'HTTP Proxy',
    description: 'Web APIs can be accessed using the HTTP protocol. Defines endpoints, valid request and response formats.',
    icon: 'data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiB2aWV3Qm94PSIwIDAgMjQgMjQiIGZpdD0iIiBoZWlnaHQ9IjEwMCUiIHdpZHRoPSIxMDAlIj4KICAgIDxnPgogICAgICAgIDxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTEyIDFhMTEgMTEgMCAxIDAgMTEgMTFBMTEuMDEgMTEuMDEgMCAwIDAgMTIgMVptOS4wODcgMTAuMDQzaC00LjM4MWExOS4xMzIgMTkuMTMyIDAgMCAwLTEuNjI2LTcuNTg1IDkuMTM0IDkuMTM0IDAgMCAxIDYuMDA3IDcuNTg1Wk0xMiAyMS4wODdjLTEuMDUyIDAtMi42MTEtMy4yMDUtMi43OTMtOC4xM2g1LjU4NmMtLjE4MiA0LjkyNS0xLjc0IDguMTMtMi43OTMgOC4xM1pNOS4yMDcgMTEuMDQzYy4xODItNC45MjYgMS43NC04LjEzIDIuNzkzLTguMTMgMS4wNTIgMCAyLjYxMSAzLjIwNCAyLjc5MyA4LjEzSDkuMjA3Wk04LjkyIDMuNDU4YTE5LjEzMSAxOS4xMzEgMCAwIDAtMS42MjYgNy41ODVIMi45NmE5LjEzNyA5LjEzNyAwIDAgMSA1Ljk2LTcuNTg1Wm0tNS45NiA5LjQ5OGg0LjMzNGExOS4xMzIgMTkuMTMyIDAgMCAwIDEuNjI2IDcuNTg1IDkuMTM1IDkuMTM1IDAgMCAxLTUuOTYtNy41ODVabTEyLjEyIDcuNTg1YTE5LjEzMiAxOS4xMzIgMCAwIDAgMS42MjYtNy41ODVoNC4zOGE5LjEzNiA5LjEzNiAwIDAgMS02LjAwNiA3LjU4NVoiPjwvcGF0aD4KICAgIDwvZz4KPC9zdmc+Cg==',
    version: '4.0.0-SNAPSHOT',
    supportedApiType: 'PROXY',
    supportedModes: ['REQUEST_RESPONSE'],
    supportedListenerType: 'HTTP',
    availableFeatures: [],
    deployed: true,
  };

  return {
    ...base,
    ...modifier,
  };
}

// Entrypoints plugin configuration
// Extracted from apim master (GET https://apim-master-api.team-apim.gravitee.dev/management/v2/plugins/entrypoints)
// + added schema for each connector
// Last update : 09/06/2023
// To update the schemas, Just past output here:
export const entrypointsGetResponse: ConnectorPlugin[] = [
  {
    id: 'webhook',
    name: 'Webhook',
    description: 'A webhook is a lightweight API that powers one-way data sharing triggered by events.',
    icon: 'data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiB2aWV3Qm94PSIwIDAgMjQgMjQiIGZpdD0iIj4KICAgIDxnPgogICAgICAgIDxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTExLjY1MiAyLjUxMmE1LjAyNyA1LjAyNyAwIDAgMC0zLjk5OCAyLjU0M2MtMS4yMDcgMi4xMjYtLjcxMSA0Ljc5MyAxLjAzIDYuMzU4TDYuOTk4IDE0LjM3YTEuOTc5IDEuOTc5IDAgMCAwLTEuNzE4IDEuMDE3Yy0uNTUuOTc0LS4yMzggMi4yMDEuNzE4IDIuNzY2Ljk1Ny41NiAyLjE2Ny4yNDIgMi43MTgtLjczMWEyLjA3NSAyLjA3NSAwIDAgMCAwLTIuMDM1bDIuNjU1LTQuNjQxLS44NzUtLjUwOWMtMS40MzMtLjg0Mi0xLjkyLTIuNzA2LTEuMDkzLTQuMTY0YTIuOTY2IDIuOTY2IDAgMCAxIDQuMDkyLTEuMTEzYzEuNDMzLjg0MiAxLjkyIDIuNzA2IDEuMDkzIDQuMTY0bDEuNzUgMS4wMThjMS4zNzctMi40MjguNTQyLTUuNTYtMS44NDQtNi45NjJhNC45MTMgNC45MTMgMCAwIDAtMi44NDItLjY2OFptLjA5NCAzLjA1MmExLjk1NSAxLjk1NSAwIDAgMC0uNzUuMjU0Yy0uOTU3LjU2LTEuMjcgMS44MjQtLjcxOSAyLjc5OGExLjk3OSAxLjk3OSAwIDAgMCAxLjcxOCAxLjAxN2wyLjYyNCA0LjY0MS44NzUtLjUwOGEyLjk2NiAyLjk2NiAwIDAgMSA0LjA5MiAxLjExMmMuODI3IDEuNDU5LjM0IDMuMzIyLTEuMDk0IDQuMTY1YTIuOTY1IDIuOTY1IDAgMCAxLTQuMDkxLTEuMTEzbC0xLjc1IDEuMDE4YzEuMzc5IDIuNDI3IDQuNDU1IDMuMjc4IDYuODQxIDEuODc1czMuMjIxLTQuNTM0IDEuODQzLTYuOTYyYy0xLjIwNy0yLjEyNi0zLjcyOS0zLjA0LTUuOTM1LTIuMjg5bC0xLjY4Ny0yLjk1NmEyLjA3NCAyLjA3NCAwIDAgMCAwLTIuMDM1Yy0uNDEzLS43My0xLjE5NC0xLjExNi0xLjk2Ny0xLjAxN1ptLTQuNzQ4IDUuNzU0QzQuMjQgMTEuMzE4IDIgMTMuNTk4IDIgMTYuNDA0YzAgMi44MDYgMi4yNDEgNS4wODcgNC45OTggNS4wODcgMi40MTMgMCA0LjQ0LTEuNzUzIDQuOTA0LTQuMDdoMy4zNzNjLjM0OC42MDUuOTggMS4wMTggMS43MTggMS4wMTggMS4xMDUgMCAyLS45MSAyLTIuMDM1IDAtMS4xMjQtLjg5NS0yLjAzNC0yLTIuMDM0LS43MzggMC0xLjM3LjQxMy0xLjcxOCAxLjAxN0g5Ljk5NnYxLjAxN2MwIDEuNjgxLTEuMzQ3IDMuMDUyLTIuOTk4IDMuMDUyLTEuNjUyIDAtMi45OTktMS4zNy0yLjk5OS0zLjA1MiAwLTEuNjggMS4zNDctMy4wNTIgMi45OTktMy4wNTJ2LTIuMDM0WiI+PC9wYXRoPgogICAgPC9nPgo8L3N2Zz4K',
    version: '1.1.0-alpha.2',
    supportedApiType: 'MESSAGE',
    supportedModes: ['SUBSCRIBE'],
    supportedQos: ['AT_MOST_ONCE', 'NONE', 'AT_LEAST_ONCE', 'AUTO'],
    supportedListenerType: 'SUBSCRIPTION',
    availableFeatures: ['DLQ'],
    deployed: true,
    schema:
      '{\n  "$schema":"http://json-schema.org/draft-07/schema#",\n  "type":"object",\n  "definitions":{\n    "proxy":{\n      "type":"object",\n      "title":"Proxy Options",\n      "oneOf":[\n        {\n          "title":"No proxy",\n          "properties":{\n            "enabled":{\n              "const":false\n            },\n            "useSystemProxy":{\n              "const":false\n            }\n          },\n          "additionalProperties":false\n        },\n        {\n          "title":"Use proxy configured at system level",\n          "properties":{\n            "enabled":{\n              "const":true\n            },\n            "useSystemProxy":{\n              "const":true\n            }\n          },\n          "additionalProperties":false\n        },\n        {\n          "title":"Use proxy for client connections",\n          "properties":{\n            "enabled":{\n              "const":true\n            },\n            "useSystemProxy":{\n              "const":false\n            },\n            "type":{\n              "type":"string",\n              "title":"Proxy Type",\n              "description":"The type of the proxy",\n              "default":"HTTP",\n              "enum":[\n                "HTTP",\n                "SOCKS4",\n                "SOCKS5"\n              ]\n            },\n            "host":{\n              "type":"string",\n              "title":"Proxy host",\n              "description":"Proxy host to connect to"\n            },\n            "port":{\n              "type":"integer",\n              "title":"Proxy port",\n              "description":"Proxy port to connect to"\n            },\n            "username":{\n              "type":"string",\n              "title":"Proxy username",\n              "description":"Optional proxy username"\n            },\n            "password":{\n              "type":"string",\n              "title":"Proxy password",\n              "description":"Optional proxy password",\n              "format":"password"\n            }\n          },\n          "required":[\n            "host",\n            "port"\n          ],\n          "additionalProperties":false\n        }\n      ]\n    }\n  },\n  "properties":{\n    "http":{\n      "type":"object",\n      "title":"HTTP Options",\n      "properties":{\n        "connectTimeout":{\n          "type":"integer",\n          "title":"Connect timeout (ms)",\n          "description":"Maximum time to connect to the webhook in milliseconds.",\n          "default":3000\n        },\n        "readTimeout":{\n          "type":"integer",\n          "title":"Read timeout (ms)",\n          "description":"Maximum time given to the webhook to complete the request (including response) in milliseconds.",\n          "default":10000\n        },\n        "idleTimeout":{\n          "type":"integer",\n          "title":"Idle timeout (ms)",\n          "default":60000,\n          "gioConfig":{\n            "banner":{\n              "title":"Idle timeout",\n              "text":"Maximum time a connection will stay in the pool without being used in milliseconds. Once the timeout has elapsed, the unused connection will be closed, allowing to free the associated resources."\n            }\n          }\n        },\n        "maxConcurrentConnections":{\n          "type":"integer",\n          "title":"Max Concurrent Connections",\n          "description":"Maximum pool size for connections. (default: 5, maximum: 20)",\n          "default":5,\n          "maximum": 20,\n          "minimum": 1\n        }\n      }\n    },\n    "proxy":{\n      "$ref":"#/definitions/proxy"\n    }\n  }\n}',
  },
  {
    id: 'http-get',
    name: 'HTTP GET',
    description: 'Send an HTTP GET request including the data directly in the request URL and process the response from the HTTP server.',
    icon: 'data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiB2aWV3Qm94PSIwIDAgMjQgMjQiIGZpdD0iIiBoZWlnaHQ9IjEwMCUiIHdpZHRoPSIxMDAlIj4KICAgIDxnPgogICAgICAgIDxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTEyIDFhMTEgMTEgMCAxIDAgMTEgMTFBMTEuMDEgMTEuMDEgMCAwIDAgMTIgMVptOS4wODcgMTAuMDQzaC00LjM4MWExOS4xMzIgMTkuMTMyIDAgMCAwLTEuNjI2LTcuNTg1IDkuMTM0IDkuMTM0IDAgMCAxIDYuMDA3IDcuNTg1Wk0xMiAyMS4wODdjLTEuMDUyIDAtMi42MTEtMy4yMDUtMi43OTMtOC4xM2g1LjU4NmMtLjE4MiA0LjkyNS0xLjc0IDguMTMtMi43OTMgOC4xM1pNOS4yMDcgMTEuMDQzYy4xODItNC45MjYgMS43NC04LjEzIDIuNzkzLTguMTMgMS4wNTIgMCAyLjYxMSAzLjIwNCAyLjc5MyA4LjEzSDkuMjA3Wk04LjkyIDMuNDU4YTE5LjEzMSAxOS4xMzEgMCAwIDAtMS42MjYgNy41ODVIMi45NmE5LjEzNyA5LjEzNyAwIDAgMSA1Ljk2LTcuNTg1Wm0tNS45NiA5LjQ5OGg0LjMzNGExOS4xMzIgMTkuMTMyIDAgMCAwIDEuNjI2IDcuNTg1IDkuMTM1IDkuMTM1IDAgMCAxLTUuOTYtNy41ODVabTEyLjEyIDcuNTg1YTE5LjEzMiAxOS4xMzIgMCAwIDAgMS42MjYtNy41ODVoNC4zOGE5LjEzNiA5LjEzNiAwIDAgMS02LjAwNiA3LjU4NVoiPjwvcGF0aD4KICAgIDwvZz4KPC9zdmc+Cg==',
    version: '1.0.0-alpha.1',
    supportedApiType: 'MESSAGE',
    supportedModes: ['SUBSCRIBE'],
    supportedQos: ['AT_MOST_ONCE', 'AT_LEAST_ONCE', 'AUTO'],
    supportedListenerType: 'HTTP',
    availableFeatures: ['RESUME', 'LIMIT'],
    deployed: true,
    schema:
      '{\n    "$schema": "http://json-schema.org/draft-07/schema#",\n    "type": "object",\n    "properties": {\n        "messagesLimitCount": {\n            "type": "integer",\n            "title": "Limit messages count",\n            "description": "Maximum number of messages to retrieve.",\n            "default": 500\n        },\n        "messagesLimitDurationMs": {\n            "type": "number",\n            "title": "Limit messages duration (in ms)",\n            "default": 5000,\n            "gioConfig": {\n                "banner": {\n                    "title": "Limit messages duration",\n                    "text": "Maximum duration in milliseconds to wait to retrieve the expected number of messages (See Limit messages count). The effective number of retrieved messages could be less than expected it maximum duration is reached."\n                }\n            }\n        },\n        "headersInPayload": {\n            "type": "boolean",\n            "default": false,\n            "title": "Allow sending messages headers to client in payload",\n            "description": "Default is false.",\n            "gioConfig": {\n                "banner": {\n                    "title": "Allow sending messages headers to client in payload",\n                    "text": "Each header will be sent as extra field in payload. For JSON and XML, in a dedicated headers object. For plain text, following \'key=value\' format. Default is false."\n                }\n            }\n        },\n        "metadataInPayload": {\n            "type": "boolean",\n            "default": false,\n            "title": "Allow sending messages metadata to client in payload",\n            "description": "Default is false.",\n            "gioConfig": {\n                "banner": {\n                    "title": "Allow sending messages metadata to client in payload",\n                    "text": "Allow sending messages metadata to client in payload. Each metadata will be sent as extra field in the payload. For JSON and XML, in a dedicated metadata object. For plain text, following \'key=value\' format. Default is false."\n                }\n            }\n        }\n    },\n    "additionalProperties": false\n}',
  },
  {
    id: 'http-post',
    name: 'HTTP POST',
    description: 'Send an HTTP POST request including URL parameters in the request and process the response from the HTTP server.',
    icon: 'data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiB2aWV3Qm94PSIwIDAgMjQgMjQiIGZpdD0iIiBoZWlnaHQ9IjEwMCUiIHdpZHRoPSIxMDAlIj4KICAgIDxnPgogICAgICAgIDxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTEyIDFhMTEgMTEgMCAxIDAgMTEgMTFBMTEuMDEgMTEuMDEgMCAwIDAgMTIgMVptOS4wODcgMTAuMDQzaC00LjM4MWExOS4xMzIgMTkuMTMyIDAgMCAwLTEuNjI2LTcuNTg1IDkuMTM0IDkuMTM0IDAgMCAxIDYuMDA3IDcuNTg1Wk0xMiAyMS4wODdjLTEuMDUyIDAtMi42MTEtMy4yMDUtMi43OTMtOC4xM2g1LjU4NmMtLjE4MiA0LjkyNS0xLjc0IDguMTMtMi43OTMgOC4xM1pNOS4yMDcgMTEuMDQzYy4xODItNC45MjYgMS43NC04LjEzIDIuNzkzLTguMTMgMS4wNTIgMCAyLjYxMSAzLjIwNCAyLjc5MyA4LjEzSDkuMjA3Wk04LjkyIDMuNDU4YTE5LjEzMSAxOS4xMzEgMCAwIDAtMS42MjYgNy41ODVIMi45NmE5LjEzNyA5LjEzNyAwIDAgMSA1Ljk2LTcuNTg1Wm0tNS45NiA5LjQ5OGg0LjMzNGExOS4xMzIgMTkuMTMyIDAgMCAwIDEuNjI2IDcuNTg1IDkuMTM1IDkuMTM1IDAgMCAxLTUuOTYtNy41ODVabTEyLjEyIDcuNTg1YTE5LjEzMiAxOS4xMzIgMCAwIDAgMS42MjYtNy41ODVoNC4zOGE5LjEzNiA5LjEzNiAwIDAgMS02LjAwNiA3LjU4NVoiPjwvcGF0aD4KICAgIDwvZz4KPC9zdmc+Cg==',
    version: '1.0.0-alpha.1',
    supportedApiType: 'MESSAGE',
    supportedModes: ['PUBLISH'],
    supportedQos: ['NONE', 'AUTO'],
    supportedListenerType: 'HTTP',
    availableFeatures: [],
    deployed: true,
    schema:
      '{\n    "$schema": "http://json-schema.org/draft-07/schema#",\n    "type": "object",\n    "properties": {\n        "requestHeadersToMessage": {\n            "title": "Allow add request Headers to the generated message",\n            "type": "boolean",\n            "default": false,\n            "description": "Each headers from incoming request will be added to the generated message headers."\n        }\n    },\n    "additionalProperties": false\n}',
  },
  {
    id: 'websocket',
    name: 'Websocket',
    description: 'Send messages to a server and receive event-driven responses without having to poll the server for a reply.',
    icon: 'data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiB2aWV3Qm94PSIwIDAgMjQgMjQiIGZpdD0iIiBoZWlnaHQ9IjEwMCUiIHdpZHRoPSIxMDAlIj4KICAgIDxnPgogICAgICAgIDxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTE3LjAzNCAxNS43OTRoMi40ODNWOS44MzZMMTYuNzIgNy4wNGwtMS43NTYgMS43NTUgMi4wNyAyLjA2OXY0LjkzWm0yLjQ5IDEuMjQ0aC04LjY2MWwtMi4wNy0yLjA2OS44NzgtLjg3NyAxLjcxIDEuNzA5aDMuNTE4bC0zLjQ2Ni0zLjQ3Ljg4NS0uODg0IDMuNDY1IDMuNDYzdi0zLjUxNkwxNC4wOCA5LjY5MmwuODcxLS44N0wxMC42NDcgNC41SDJsMi40NzYgMi40NzV2LjAwNmg1LjEzNmwxLjgxNSAxLjgxNC0yLjY1MyAyLjY1Mi0xLjgxNS0xLjgxNFY4LjIyNUg0LjQ3NnYyLjQzNmw0LjI5OCA0LjI5NS0xLjc1IDEuNzQ4TDkuODIzIDE5LjVIMjJsLTIuNDc2LTIuNDYyWiI+PC9wYXRoPgogICAgPC9nPgo8L3N2Zz4K',
    version: '1.0.0-alpha.2',
    supportedApiType: 'MESSAGE',
    supportedModes: ['SUBSCRIBE', 'PUBLISH'],
    supportedQos: ['NONE', 'AUTO'],
    supportedListenerType: 'HTTP',
    availableFeatures: [],
    deployed: true,
    schema:
      '{\n    "$schema": "http://json-schema.org/draft-07/schema#",\n    "type": "object",\n    "properties": {\n        "publisher": {\n            "title": "Publisher configuration",\n            "type": "object",\n            "properties": {\n                "enabled": {\n                    "title": "Enable the publication capability",\n                    "description": "Allow to enable or disable the publication capability. By disabling it, you assume that the application will never be able to publish any message.",\n                    "type": "boolean",\n                    "default": true\n                }\n            },\n            "additionalProperties": false\n        },\n        "subscriber": {\n            "title": "Subscriber configuration",\n            "type": "object",\n            "properties": {\n                "enabled": {\n                    "title": "Enable the subscription capability",\n                    "description": "Allow to enable or disable the subscription capability. By disabling it, you assume that the application will never receive any message.",\n                    "type": "boolean",\n                    "default": true\n                }\n            },\n            "additionalProperties": false\n        }\n    },\n    "required": [],\n    "additionalProperties": false\n}',
  },
  {
    id: 'sse',
    name: 'Server-Sent Events',
    description: 'Designed to use the JavaScript EventSource API in order to subscribe to a stream of data in the browser.',
    icon: 'data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiB2aWV3Qm94PSIwIDAgMjQgMjQiIGZpdD0iIiBoZWlnaHQ9IjEwMCUiIHdpZHRoPSIxMDAlIj4KICAgIDxnPgogICAgICAgIDxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTExIDE4LjE3QTMuMDA4IDMuMDA4IDAgMCAwIDkuMTcgMjBINWExIDEgMCAxIDAgMCAyaDQuMTdhMy4wMDEgMy4wMDEgMCAwIDAgNS42NiAwSDE5YTEgMSAwIDEgMCAwLTJoLTQuMTdBMy4wMDkgMy4wMDkgMCAwIDAgMTMgMTguMTdWMTZhMSAxIDAgMSAwLTIgMHYyLjE3Wm0uMzAzIDIuMTEzLS4wMjguMDI4YTEgMSAwIDEgMCAuMDI4LS4wMjhaIj48L3BhdGg+CiAgICAgICAgPHBhdGggZmlsbD0iY3VycmVudENvbG9yIiBkPSJNOC42NjggMS45OTRBOSA5IDAgMCAxIDE3LjQ4IDhoLjUxOGE2LjAwMyA2LjAwMyAwIDAgMSA1LjcyNSA0LjE4OCA2IDYgMCAwIDEtMi4yNjkgNi43MiAxIDEgMCAxIDEtMS4xNS0xLjYzNkE0IDQgMCAwIDAgMTguMDAxIDEwaC0xLjI2YTEgMSAwIDAgMS0uOTctLjc1MUE3IDcgMCAxIDAgMy43NSAxNS42MjdhMSAxIDAgMCAxLTEuNDk4IDEuMzI2QTkgOSAwIDAgMSA4LjY2OCAxLjk5NFoiPjwvcGF0aD4KICAgIDwvZz4KPC9zdmc+Cg==',
    version: '3.1.0-alpha.1',
    supportedApiType: 'MESSAGE',
    supportedModes: ['SUBSCRIBE'],
    supportedQos: ['AT_MOST_ONCE', 'NONE', 'AT_LEAST_ONCE', 'AUTO'],
    supportedListenerType: 'HTTP',
    availableFeatures: [],
    deployed: true,
    schema:
      '{\n  "$schema": "http://json-schema.org/draft-07/schema#",\n  "type": "object",\n  "properties": {\n    "heartbeatIntervalInMs": {\n      "type": "integer",\n      "default": 5000,\n      "minimum": 2000,\n      "title": "Define the interval in which heartbeat are sent to client",\n      "description": "Interval must be higher or equal than 2000ms. Each heartbeat will be sent as extra empty comment \\":\\""\n    },\n    "metadataAsComment": {\n      "title": "Allow sending messages metadata to client as SSE comments",\n      "description": "Each metadata attribute will be sent as extra line following \\":key=value\\" format.",\n      "type": "boolean",\n      "default": false\n    },\n    "headersAsComment": {\n      "title": "Allow sending messages headers to client as SSE comments",\n      "description": "Each header will be sent as extra line following \\":key=value\\" format.",\n      "type": "boolean",\n      "default": false\n    }\n  },\n  "additionalProperties": false\n}',
  },
  {
    id: 'http-proxy',
    name: 'HTTP Proxy',
    description: 'Web APIs can be accessed using the HTTP protocol. Defines endpoints, valid request and response formats.',
    icon: 'data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiB2aWV3Qm94PSIwIDAgMjQgMjQiIGZpdD0iIiBoZWlnaHQ9IjEwMCUiIHdpZHRoPSIxMDAlIj4KICAgIDxnPgogICAgICAgIDxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTEyIDFhMTEgMTEgMCAxIDAgMTEgMTFBMTEuMDEgMTEuMDEgMCAwIDAgMTIgMVptOS4wODcgMTAuMDQzaC00LjM4MWExOS4xMzIgMTkuMTMyIDAgMCAwLTEuNjI2LTcuNTg1IDkuMTM0IDkuMTM0IDAgMCAxIDYuMDA3IDcuNTg1Wk0xMiAyMS4wODdjLTEuMDUyIDAtMi42MTEtMy4yMDUtMi43OTMtOC4xM2g1LjU4NmMtLjE4MiA0LjkyNS0xLjc0IDguMTMtMi43OTMgOC4xM1pNOS4yMDcgMTEuMDQzYy4xODItNC45MjYgMS43NC04LjEzIDIuNzkzLTguMTMgMS4wNTIgMCAyLjYxMSAzLjIwNCAyLjc5MyA4LjEzSDkuMjA3Wk04LjkyIDMuNDU4YTE5LjEzMSAxOS4xMzEgMCAwIDAtMS42MjYgNy41ODVIMi45NmE5LjEzNyA5LjEzNyAwIDAgMSA1Ljk2LTcuNTg1Wm0tNS45NiA5LjQ5OGg0LjMzNGExOS4xMzIgMTkuMTMyIDAgMCAwIDEuNjI2IDcuNTg1IDkuMTM1IDkuMTM1IDAgMCAxLTUuOTYtNy41ODVabTEyLjEyIDcuNTg1YTE5LjEzMiAxOS4xMzIgMCAwIDAgMS42MjYtNy41ODVoNC4zOGE5LjEzNiA5LjEzNiAwIDAgMS02LjAwNiA3LjU4NVoiPjwvcGF0aD4KICAgIDwvZz4KPC9zdmc+Cg==',
    version: '4.0.0-SNAPSHOT',
    supportedApiType: 'PROXY',
    supportedModes: ['REQUEST_RESPONSE'],
    supportedListenerType: 'HTTP',
    availableFeatures: [],
    deployed: true,
    schema:
      '{\n    "$schema": "http://json-schema.org/draft-07/schema#",\n    "type": "object",\n    "properties": {},\n    "additionalProperties": false\n}',
  },
];

export const getEntrypointConnectorSchema = (id: string) => {
  const entrypoint = entrypointsGetResponse.find(entrypoint => entrypoint.id === id);
  return entrypoint?.schema ? JSON.parse(entrypoint.schema) : null;
};
