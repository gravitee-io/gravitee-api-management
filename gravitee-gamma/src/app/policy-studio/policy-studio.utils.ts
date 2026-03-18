import type { Flow, Step, Phase, HttpMethod, PolicyPlugin } from './types';

export function newStep(policyId: string, policies?: PolicyPlugin[]): Step {
  const policy = policies?.find((p) => p.id === policyId);
  return {
    id: crypto.randomUUID(),
    name: policy?.name ?? policyId,
    enabled: true,
    policy: policyId,
    configuration: {},
  };
}

export function newFlow(name: string, path: string, methods?: HttpMethod[]): Flow {
  return {
    name,
    enabled: true,
    selectors: [
      {
        type: 'HTTP',
        path: path || '/',
        pathOperator: 'STARTS_WITH',
        methods,
      },
    ],
    request: [],
    response: [],
  };
}

export function reorder<T>(list: readonly T[], from: number, to: number): T[] {
  const result = [...list];
  const [removed] = result.splice(from, 1);
  result.splice(to, 0, removed);
  return result;
}

export function updateFlowPhase(flow: Flow, phase: Phase, updater: (steps: Step[]) => Step[]): Flow {
  return {
    ...flow,
    [phase]: updater([...(flow[phase] ?? [])]),
  };
}

export function isPhaseCompatible(
  policy: PolicyPlugin | undefined,
  phase: Phase,
  apiType: string = 'HTTP_PROXY',
): boolean {
  if (!policy?.flowPhaseCompatibility) return true;
  const allowed = policy.flowPhaseCompatibility[apiType];
  if (!allowed) return true;
  return allowed.includes(phase);
}

export function safeFlows(flows: unknown): Flow[] {
  if (!Array.isArray(flows)) return [];
  return flows.map((f) => ({
    name: f?.name ?? undefined,
    enabled: f?.enabled ?? true,
    selectors: Array.isArray(f?.selectors) ? f.selectors : undefined,
    request: Array.isArray(f?.request) ? f.request.map(safeStep) : [],
    response: Array.isArray(f?.response) ? f.response.map(safeStep) : [],
  }));
}

function safeStep(s: unknown, index: number): Step {
  const raw = s as Record<string, unknown> | null;
  return {
    id: (raw?.id as string) ?? `step-recovery-${index}`,
    name: (raw?.name as string) ?? undefined,
    enabled: typeof raw?.enabled === 'boolean' ? raw.enabled : true,
    policy: (raw?.policy as string) ?? 'unknown',
    configuration: (raw?.configuration as Record<string, unknown>) ?? {},
    condition: (raw?.condition as string) ?? undefined,
  };
}
