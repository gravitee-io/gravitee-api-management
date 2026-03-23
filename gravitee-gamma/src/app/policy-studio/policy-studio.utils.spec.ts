import { newStep, newFlow, updateFlowPhase, isPhaseCompatible, safeFlows } from './policy-studio.utils';
import type { Flow, Step, PolicyPlugin } from './types';

const originalCrypto = globalThis.crypto;

beforeAll(() => {
  Object.defineProperty(globalThis, 'crypto', {
    value: { randomUUID: () => Math.random().toString(36).slice(2) },
    configurable: true,
  });
});

afterAll(() => {
  Object.defineProperty(globalThis, 'crypto', {
    value: originalCrypto,
    configurable: true,
  });
});

describe('newStep', () => {
  it('creates a step with the given policyId', () => {
    const s = newStep('rate-limiting', 'Rate Limiting');
    expect(s.policy).toBe('rate-limiting');
    expect(s.name).toBe('Rate Limiting');
    expect(s.enabled).toBe(true);
    expect(s.configuration).toEqual({});
    expect(s.id).toBeDefined();
  });

  it('falls back to policyId as name when policyName is omitted', () => {
    const s = newStep('jwt');
    expect(s.name).toBe('jwt');
  });

  it('generates unique ids', () => {
    const a = newStep('p');
    const b = newStep('p');
    expect(a.id).not.toBe(b.id);
  });
});

describe('newFlow', () => {
  it('creates a flow with HTTP selector', () => {
    const f = newFlow('My Flow', '/api/v1', ['GET', 'POST']);
    expect(f.name).toBe('My Flow');
    expect(f.enabled).toBe(true);
    expect(f.request).toEqual([]);
    expect(f.response).toEqual([]);
    expect(f.selectors).toEqual([
      { type: 'HTTP', path: '/api/v1', pathOperator: 'STARTS_WITH', methods: ['GET', 'POST'] },
    ]);
  });

  it('defaults path to / when empty', () => {
    const f = newFlow('F', '');
    expect(f.selectors![0].path).toBe('/');
  });
});

describe('updateFlowPhase', () => {
  const step: Step = { id: 's1', name: 's1', enabled: true, policy: 'p', configuration: {} };
  const base: Flow = { name: 'F', enabled: true, request: [step], response: [] };

  it('updates request phase via updater', () => {
    const result = updateFlowPhase(base, 'request', (steps) => steps.filter((s) => s.id !== 's1'));
    expect(result.request).toEqual([]);
    expect(result.response).toEqual([]);
  });

  it('updates response phase via updater', () => {
    const newS: Step = { id: 's2', name: 's2', enabled: true, policy: 'p', configuration: {} };
    const result = updateFlowPhase(base, 'response', (steps) => [...steps, newS]);
    expect(result.response).toEqual([newS]);
    expect(result.request).toEqual([step]);
  });

  it('handles undefined phase array gracefully', () => {
    const broken = { name: 'F', enabled: true } as unknown as Flow;
    const result = updateFlowPhase(broken, 'request', (steps) => [...steps, step]);
    expect(result.request).toEqual([step]);
  });
});

describe('isPhaseCompatible', () => {
  const requestOnly: PolicyPlugin = {
    id: 'p1',
    name: 'P1',
    flowPhaseCompatibility: { HTTP_PROXY: ['request'] },
  };

  const bothPhases: PolicyPlugin = {
    id: 'p2',
    name: 'P2',
    flowPhaseCompatibility: { HTTP_PROXY: ['request', 'response'] },
  };

  it('returns true when no policy provided', () => {
    expect(isPhaseCompatible(undefined, 'request')).toBe(true);
  });

  it('returns true when policy has no flowPhaseCompatibility', () => {
    const p: PolicyPlugin = { id: 'x', name: 'X' };
    expect(isPhaseCompatible(p, 'response')).toBe(true);
  });

  it('rejects incompatible phase', () => {
    expect(isPhaseCompatible(requestOnly, 'response', 'PROXY')).toBe(false);
  });

  it('accepts compatible phase', () => {
    expect(isPhaseCompatible(requestOnly, 'request', 'PROXY')).toBe(true);
  });

  it('maps PROXY to HTTP_PROXY', () => {
    expect(isPhaseCompatible(bothPhases, 'response', 'PROXY')).toBe(true);
  });

  it('maps MESSAGE apiType', () => {
    const messagePolicy: PolicyPlugin = {
      id: 'p3',
      name: 'P3',
      flowPhaseCompatibility: { MESSAGE: ['request'] },
    };
    expect(isPhaseCompatible(messagePolicy, 'request', 'MESSAGE')).toBe(true);
    expect(isPhaseCompatible(messagePolicy, 'response', 'MESSAGE')).toBe(false);
  });

  it('falls back to HTTP_PROXY for unknown apiType', () => {
    expect(isPhaseCompatible(requestOnly, 'request', 'UNKNOWN_TYPE')).toBe(true);
    expect(isPhaseCompatible(requestOnly, 'response', 'UNKNOWN_TYPE')).toBe(false);
  });
});

describe('safeFlows', () => {
  it('returns empty array for non-array input', () => {
    expect(safeFlows(null)).toEqual([]);
    expect(safeFlows(undefined)).toEqual([]);
    expect(safeFlows('string')).toEqual([]);
    expect(safeFlows(42)).toEqual([]);
  });

  it('sanitizes a valid flow', () => {
    const input = [{
      name: 'F1',
      enabled: true,
      selectors: [{ type: 'HTTP', path: '/' }],
      request: [{ id: 's1', name: 'S1', enabled: true, policy: 'p', configuration: { key: 'val' } }],
      response: [],
    }];
    const result = safeFlows(input);
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe('F1');
    expect(result[0].request[0].id).toBe('s1');
    expect(result[0].request[0].configuration).toEqual({ key: 'val' });
  });

  it('recovers from missing fields', () => {
    const input = [{ request: [{}] }];
    const result = safeFlows(input);
    expect(result[0].enabled).toBe(true);
    expect(result[0].request[0].id).toBe('step-recovery-0');
    expect(result[0].request[0].policy).toBe('unknown');
    expect(result[0].request[0].enabled).toBe(true);
    expect(result[0].request[0].configuration).toEqual({});
  });

  it('replaces non-array request/response with empty arrays', () => {
    const input = [{ request: 'broken', response: 42 }];
    const result = safeFlows(input);
    expect(result[0].request).toEqual([]);
    expect(result[0].response).toEqual([]);
  });

  it('preserves step condition when present', () => {
    const input = [{
      request: [{ id: 's1', policy: 'p', configuration: {}, condition: '#context.ok' }],
      response: [],
    }];
    const result = safeFlows(input);
    expect(result[0].request[0].condition).toBe('#context.ok');
  });
});
