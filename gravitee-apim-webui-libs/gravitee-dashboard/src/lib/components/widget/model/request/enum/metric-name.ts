/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
export type HttpMetricName =
  | 'HTTP_REQUESTS'
  | 'HTTP_ERRORS'
  | 'HTTP_ERROR_RATE'
  | 'HTTP_RPS'
  | 'HTTP_REQUEST_CONTENT_LENGTH'
  | 'HTTP_RESPONSE_CONTENT_LENGTH'
  | 'HTTP_ENDPOINT_RESPONSE_TIME'
  | 'HTTP_GATEWAY_RESPONSE_TIME'
  | 'HTTP_GATEWAY_LATENCY';

export type LlmMetricName =
  | 'LLM_PROMPT_TOKEN_SENT'
  | 'LLM_PROMPT_TOKEN_RECEIVED'
  | 'LLM_PROMPT_TOKEN_SENT_COST'
  | 'LLM_PROMPT_TOKEN_RECEIVED_COST'
  | 'LLM_PROMPT_TOTAL_TOKEN'
  | 'LLM_PROMPT_TOKEN_TOTAL_COST';

export type MessageMetricName = 'MESSAGE_PAYLOAD_SIZE' | 'MESSAGES' | 'MESSAGE_ERRORS' | 'MESSAGE_GATEWAY_LATENCY' | 'MESSAGE_RPS';

export type MetricName = HttpMetricName | LlmMetricName | MessageMetricName;
