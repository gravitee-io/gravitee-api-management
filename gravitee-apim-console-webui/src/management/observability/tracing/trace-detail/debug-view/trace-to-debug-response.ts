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
import { DebugResponse } from '../../../../api/debug-mode/models/DebugResponse';
import { PolicyScope, RequestPolicyDebugStep, ResponsePolicyDebugStep } from '../../../../api/debug-mode/models/DebugStep';
import { Trace, TraceEvent, TraceSpan } from '../../tracing.model';

type ParsedState = {
  headers: Record<string, string[]>;
  attributes: Record<string, boolean | number | string>;
};

type Stage = 'SECURITY' | 'PLATFORM' | 'PLAN' | 'API';

const ZERO_SPAN_ID_REGEX = /^0+$/;

/**
 * Projects an OTel trace onto the shape expected by {@code DebugModeResponseComponent} so that the debug-mode UI can render it.
 * Mapping strategy:
 *  - Root HTTP SERVER span (parent-less, has {@code http.method}) → request/response envelope.
 *  - Invoker span ({@code Invoker=endpoint-invoker}) → its HTTP CLIENT child provides {@code backendResponse} (url, method, status).
 *  - Policy spans ({@code gravitee.policy} attr) AND security spans ({@code Security} attr) become policy debug steps.
 *  - Stage is inferred from the parent flow span's name: security → SECURITY; plan-* → PLAN; organization-* → PLATFORM; else API.
 *  - Output state per step comes from the {@code gravitee.policy.post} event attributes (verbose tracing); when absent, output just
 *    carries method/path (request) or statusCode (response) so HTTP properties still render.
 */
export function traceToDebugResponse(trace: Trace | null): DebugResponse | null {
  if (!trace || !trace.spans || trace.spans.length === 0) return null;

  const allSpans = flattenSpans(trace.spans);
  const spanById = new Map<string, TraceSpan>();
  for (const s of allSpans) spanById.set(s.spanId, s);

  const rootHttpSpan = findRootHttpSpan(allSpans);
  const invokerSpan = allSpans.find(s => s.attributes['Invoker'] !== undefined);
  const backendHttpSpan = invokerSpan ? allSpans.find(s => s.parentSpanId === invokerSpan.spanId && s.attributes['http.url']) : undefined;

  const requestPolicySpans = allSpans
    .filter(s => isPolicyOrSecuritySpan(s) && phaseOf(s) === 'request')
    .sort((a, b) => parseStartTime(a.startTime) - parseStartTime(b.startTime));

  const responsePolicySpans = allSpans
    .filter(s => isPolicyOrSecuritySpan(s) && phaseOf(s) === 'response')
    .sort((a, b) => parseStartTime(a.startTime) - parseStartTime(b.startTime));

  const requestPreState = firstPreState(requestPolicySpans);

  const responsePreState = firstPreState(responsePolicySpans) ?? emptyState();

  const requestMethod = rootHttpSpan?.attributes['http.method'];
  const requestPath = rootHttpSpan?.attributes['http.target'] ?? rootHttpSpan?.attributes['http.route'] ?? '/';
  const requestStatusCode = numberOrUndefined(rootHttpSpan?.attributes['http.status_code']);
  const backendStatusCode = numberOrUndefined(backendHttpSpan?.attributes['http.status_code']);

  // Walk the policies in order, carrying forward the last known headers/attributes state. If a span doesn't emit a post event (e.g.
  // security spans don't expose the request context), reuse the previous step's state so the debug-mode diff shows "no change" for
  // that step instead of wrongly reporting every header/attribute as removed.
  let requestRunningState: ParsedState = requestPreState;
  const requestPolicyDebugSteps: RequestPolicyDebugStep[] = requestPolicySpans.map(span => {
    const post = span.events?.find(e => e.name === 'gravitee.policy.post');
    const nextState = post ? parsePolicyEvent(post, 'ON_REQUEST') : requestRunningState;
    const step = toPolicyDebugStep(span, 'ON_REQUEST', spanById, { method: requestMethod, path: requestPath }, nextState);
    requestRunningState = nextState;
    return step;
  });
  const requestFinalState = requestRunningState;

  let responseRunningState: ParsedState = responsePreState;
  const responsePolicyDebugSteps: ResponsePolicyDebugStep[] = responsePolicySpans.map(span => {
    const post = span.events?.find(e => e.name === 'gravitee.policy.post');
    const nextState = post ? parsePolicyEvent(post, 'ON_RESPONSE') : responseRunningState;
    const step = toPolicyDebugStep(span, 'ON_RESPONSE', spanById, { statusCode: backendStatusCode }, nextState);
    responseRunningState = nextState;
    return step;
  });
  const responseFinalState = responseRunningState;

  return {
    isLoading: false,
    executionMode: 'v4-emulation-engine',

    request: {
      method: requestMethod,
      path: requestPath,
      headers: requestPreState.headers,
    },

    preprocessorStep: {
      attributes: requestPreState.attributes,
      headers: requestPreState.headers,
    },

    requestPolicyDebugSteps,
    requestDebugSteps: {
      input: {
        id: 'request-input',
        status: undefined,
        duration: 0,
        stage: undefined,
        output: {
          method: requestMethod,
          path: requestPath,
          headers: requestPreState.headers,
          attributes: requestPreState.attributes,
        },
      },
      output: {
        id: 'request-output',
        status: undefined,
        duration: requestPolicyDebugSteps.reduce((acc, s) => acc + s.duration, 0),
        stage: undefined,
        output: {
          method: requestMethod,
          path: requestPath,
          headers: requestFinalState.headers,
          attributes: requestFinalState.attributes,
        },
      },
    },

    backendResponse: {
      statusCode: backendStatusCode,
      method: backendHttpSpan?.attributes['http.method'],
      path: backendHttpSpan?.attributes['http.url'],
      headers: {},
    },

    responsePolicyDebugSteps,
    responseDebugSteps: {
      input: {
        id: 'response-input',
        status: undefined,
        duration: 0,
        stage: undefined,
        output: {
          statusCode: backendStatusCode,
          headers: responsePreState.headers,
          attributes: responsePreState.attributes,
        },
      },
      output: {
        id: 'response-output',
        status: undefined,
        duration: responsePolicyDebugSteps.reduce((acc, s) => acc + s.duration, 0),
        stage: undefined,
        output: {
          statusCode: requestStatusCode,
          headers: responseFinalState.headers,
          attributes: responseFinalState.attributes,
        },
      },
    },

    response: {
      statusCode: requestStatusCode,
      method: requestMethod,
      path: requestPath,
      headers: responseFinalState.headers,
    },
  };
}

function findRootHttpSpan(spans: TraceSpan[]): TraceSpan | undefined {
  return spans.find(
    s =>
      s.attributes['http.method'] &&
      s.attributes['Invoker'] === undefined &&
      (!s.parentSpanId || ZERO_SPAN_ID_REGEX.test(s.parentSpanId)),
  );
}

function isPolicyOrSecuritySpan(span: TraceSpan): boolean {
  return span.attributes['gravitee.policy'] !== undefined || isSecuritySpan(span);
}

function isSecuritySpan(span: TraceSpan): boolean {
  return span.attributes['Security'] !== undefined || span.attributes['security'] !== undefined;
}

function phaseOf(span: TraceSpan): 'request' | 'response' | undefined {
  const phase = span.attributes['gravitee.execution.phase'];
  return phase === 'request' || phase === 'response' ? phase : undefined;
}

function toPolicyDebugStep<Scope extends PolicyScope>(
  span: TraceSpan,
  scope: Scope,
  spanById: Map<string, TraceSpan>,
  extras: Record<string, string | number | undefined>,
  state: ParsedState,
): RequestPolicyDebugStep & ResponsePolicyDebugStep {
  // When the span itself carries custom attributes (e.g. a security decision surfaced on the span), merge them on top of the running
  // state so they appear in the inspector — but don't overwrite headers/attributes the state already contains.
  const spanAttributes = extractSpanAttributes(span);
  const mergedAttributes = { ...state.attributes, ...spanAttributes };

  return {
    id: span.spanId,
    policyId: policyIdOf(span),
    policyInstanceId: span.spanId,
    status: statusOf(span),
    scope,
    duration: Math.max(0, Math.floor(span.durationNanos)),
    stage: stageOf(span, spanById),
    output: {
      ...extras,
      headers: state.headers,
      attributes: mergedAttributes,
    } as RequestPolicyDebugStep['output'] & ResponsePolicyDebugStep['output'],
  };
}

const SPAN_ATTR_BLACKLIST = new Set([
  'gravitee.policy',
  'gravitee.policy.trigger.executed',
  'gravitee.execution.phase',
  'otel.status_code',
  'Invoker',
]);

function extractSpanAttributes(span: TraceSpan): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [key, value] of Object.entries(span.attributes ?? {})) {
    if (SPAN_ATTR_BLACKLIST.has(key)) continue;
    if (key.startsWith('http.request.header.') || key.startsWith('http.response.header.')) continue;
    out[key] = value;
  }
  return out;
}

function policyIdOf(span: TraceSpan): string {
  const policy = span.attributes['gravitee.policy'];
  if (policy) return policy;
  const security = span.attributes['Security'] ?? span.attributes['security'];
  return security ? `security-${security}` : 'policy';
}

function stageOf(span: TraceSpan, spanById: Map<string, TraceSpan>): Stage {
  if (isSecuritySpan(span)) return 'SECURITY';
  const parent = span.parentSpanId ? spanById.get(span.parentSpanId) : undefined;
  const flow = parent?.attributes['flow'];
  if (flow) {
    if (flow.startsWith('plan-')) return 'PLAN';
    if (flow.startsWith('organization-')) return 'PLATFORM';
  }
  return 'API';
}

function parsePolicyEvent(event: TraceEvent, scope: PolicyScope): ParsedState {
  const headers: Record<string, string[]> = {};
  const attributes: Record<string, boolean | number | string> = {};
  const headerPrefix = scope.startsWith('ON_REQUEST') ? 'http.request.header.' : 'http.response.header.';

  for (const [key, value] of Object.entries(event.attributes ?? {})) {
    if (key.startsWith(headerPrefix)) {
      headers[key.slice(headerPrefix.length)] = [value];
    } else if (key.startsWith('http.request.header.') || key.startsWith('http.response.header.')) {
      // Header for the other phase — skip.
    } else if (!key.startsWith('gravitee.policy') && !key.startsWith('gravitee.execution') && key !== 'otel.status_code') {
      // Keep the full attribute key (including the `gravitee.attribute.` prefix) so it matches what the trace actually carries.
      attributes[key] = value;
    }
  }
  return { headers, attributes };
}

function firstPreState(policies: TraceSpan[]): ParsedState {
  for (const span of policies) {
    const pre = span.events?.find(e => e.name === 'gravitee.policy.pre');
    if (pre) {
      const scope: PolicyScope = phaseOf(span) === 'response' ? 'ON_RESPONSE' : 'ON_REQUEST';
      return parsePolicyEvent(pre, scope);
    }
  }
  return emptyState();
}

function emptyState(): ParsedState {
  return { headers: {}, attributes: {} };
}

function statusOf(span: TraceSpan): 'COMPLETED' | 'ERROR' | 'SKIPPED' {
  const code = span.attributes['otel.status_code'];
  if (code === 'ERROR') return 'ERROR';
  if (span.attributes['gravitee.policy.trigger.executed'] === 'false') return 'SKIPPED';
  return 'COMPLETED';
}

function numberOrUndefined(value: string | undefined): number | undefined {
  if (value === undefined) return undefined;
  const n = Number(value);
  return Number.isFinite(n) ? n : undefined;
}

function parseStartTime(startTime: string | number): number {
  if (typeof startTime === 'number') return startTime * 1_000_000_000;
  const d = new Date(startTime);
  if (!isNaN(d.getTime())) return d.getTime() * 1_000_000;
  const num = parseFloat(startTime);
  if (!isNaN(num)) return num * 1_000_000_000;
  return 0;
}

function flattenSpans(spans: TraceSpan[]): TraceSpan[] {
  const out: TraceSpan[] = [];
  const walk = (list: TraceSpan[]) => {
    for (const s of list) {
      out.push(s);
      if (s.children && s.children.length > 0) walk(s.children);
    }
  };
  walk(spans);
  return out;
}
