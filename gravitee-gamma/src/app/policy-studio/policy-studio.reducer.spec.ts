import { policyStudioReducer, initialState, PolicyStudioState } from './policy-studio.reducer';
import type { Flow, Step } from './types';

const step = (id: string, policy = 'test-policy'): Step => ({
  id,
  name: id,
  enabled: true,
  policy,
  configuration: {},
});

const flow = (name: string, request: Step[] = [], response: Step[] = []): Flow => ({
  name,
  enabled: true,
  selectors: [{ type: 'HTTP', path: '/', pathOperator: 'STARTS_WITH' }],
  request,
  response,
});

const stateWith = (overrides: Partial<PolicyStudioState>): PolicyStudioState => ({
  ...initialState,
  ...overrides,
});

describe('policyStudioReducer', () => {
  describe('SET_FLOWS', () => {
    it('replaces flows and resets isDirty', () => {
      const flows = [flow('A'), flow('B')];
      const state = stateWith({ isDirty: true });
      const result = policyStudioReducer(state, { type: 'SET_FLOWS', flows });
      expect(result.flows).toEqual(flows);
      expect(result.isDirty).toBe(false);
    });

    it('clamps selectedFlowIndex when new flows are shorter', () => {
      const state = stateWith({ flows: [flow('A'), flow('B'), flow('C')], selectedFlowIndex: 2 });
      const result = policyStudioReducer(state, { type: 'SET_FLOWS', flows: [flow('X')] });
      expect(result.selectedFlowIndex).toBe(0);
    });

    it('clamps to 0 when flows are empty', () => {
      const state = stateWith({ selectedFlowIndex: 3 });
      const result = policyStudioReducer(state, { type: 'SET_FLOWS', flows: [] });
      expect(result.selectedFlowIndex).toBe(0);
    });
  });

  describe('SELECT_FLOW', () => {
    const state = stateWith({ flows: [flow('A'), flow('B'), flow('C')] });

    it('selects a valid index', () => {
      const result = policyStudioReducer(state, { type: 'SELECT_FLOW', index: 1 });
      expect(result.selectedFlowIndex).toBe(1);
    });

    it('clamps negative index to 0', () => {
      const result = policyStudioReducer(state, { type: 'SELECT_FLOW', index: -5 });
      expect(result.selectedFlowIndex).toBe(0);
    });

    it('clamps overflow index to last', () => {
      const result = policyStudioReducer(state, { type: 'SELECT_FLOW', index: 100 });
      expect(result.selectedFlowIndex).toBe(2);
    });
  });

  describe('ADD_FLOW', () => {
    it('appends flow, selects it, marks dirty', () => {
      const state = stateWith({ flows: [flow('A')] });
      const newFlow = flow('B');
      const result = policyStudioReducer(state, { type: 'ADD_FLOW', flow: newFlow });
      expect(result.flows).toHaveLength(2);
      expect(result.flows[1]).toEqual(newFlow);
      expect(result.selectedFlowIndex).toBe(1);
      expect(result.isDirty).toBe(true);
    });
  });

  describe('REMOVE_FLOW', () => {
    it('removes the flow at index and marks dirty', () => {
      const state = stateWith({ flows: [flow('A'), flow('B'), flow('C')] });
      const result = policyStudioReducer(state, { type: 'REMOVE_FLOW', index: 1 });
      expect(result.flows.map((f) => f.name)).toEqual(['A', 'C']);
      expect(result.isDirty).toBe(true);
    });

    it('clamps selectedFlowIndex when removing the last flow', () => {
      const state = stateWith({ flows: [flow('A'), flow('B')], selectedFlowIndex: 1 });
      const result = policyStudioReducer(state, { type: 'REMOVE_FLOW', index: 1 });
      expect(result.selectedFlowIndex).toBe(0);
    });

    it('clamps to 0 when all flows are removed', () => {
      const state = stateWith({ flows: [flow('A')], selectedFlowIndex: 0 });
      const result = policyStudioReducer(state, { type: 'REMOVE_FLOW', index: 0 });
      expect(result.selectedFlowIndex).toBe(0);
      expect(result.flows).toHaveLength(0);
    });
  });

  describe('TOGGLE_FLOW_ENABLED', () => {
    it('toggles enabled flag and marks dirty', () => {
      const state = stateWith({ flows: [flow('A')] });
      const result = policyStudioReducer(state, { type: 'TOGGLE_FLOW_ENABLED', index: 0 });
      expect(result.flows[0].enabled).toBe(false);
      expect(result.isDirty).toBe(true);
    });

    it('does not mutate other flows', () => {
      const state = stateWith({ flows: [flow('A'), flow('B')] });
      const result = policyStudioReducer(state, { type: 'TOGGLE_FLOW_ENABLED', index: 0 });
      expect(result.flows[1].enabled).toBe(true);
    });
  });

  describe('ADD_STEP', () => {
    it('adds step to the request phase', () => {
      const state = stateWith({ flows: [flow('A')] });
      const s = step('s1');
      const result = policyStudioReducer(state, { type: 'ADD_STEP', flowIndex: 0, phase: 'request', step: s });
      expect(result.flows[0].request).toEqual([s]);
      expect(result.isDirty).toBe(true);
    });

    it('adds step to the response phase', () => {
      const state = stateWith({ flows: [flow('A')] });
      const s = step('s1');
      const result = policyStudioReducer(state, { type: 'ADD_STEP', flowIndex: 0, phase: 'response', step: s });
      expect(result.flows[0].response).toEqual([s]);
    });

    it('does not affect other flows', () => {
      const state = stateWith({ flows: [flow('A'), flow('B')] });
      const result = policyStudioReducer(state, { type: 'ADD_STEP', flowIndex: 0, phase: 'request', step: step('s1') });
      expect(result.flows[1].request).toEqual([]);
    });
  });

  describe('REMOVE_STEP', () => {
    it('removes step by index from the correct phase', () => {
      const s1 = step('s1');
      const s2 = step('s2');
      const state = stateWith({ flows: [flow('A', [s1, s2])] });
      const result = policyStudioReducer(state, { type: 'REMOVE_STEP', flowIndex: 0, phase: 'request', stepIndex: 0 });
      expect(result.flows[0].request).toEqual([s2]);
      expect(result.isDirty).toBe(true);
    });
  });

  describe('REORDER_STEP', () => {
    it('moves step from one position to another', () => {
      const s1 = step('s1');
      const s2 = step('s2');
      const s3 = step('s3');
      const state = stateWith({ flows: [flow('A', [s1, s2, s3])] });
      const result = policyStudioReducer(state, { type: 'REORDER_STEP', flowIndex: 0, phase: 'request', from: 0, to: 2 });
      expect(result.flows[0].request.map((s) => s.id)).toEqual(['s2', 's3', 's1']);
      expect(result.isDirty).toBe(true);
    });
  });

  describe('UPDATE_STEP_CONFIG', () => {
    it('updates configuration of the targeted step', () => {
      const s1 = step('s1');
      const state = stateWith({ flows: [flow('A', [s1])] });
      const config = { limit: 100 };
      const result = policyStudioReducer(state, {
        type: 'UPDATE_STEP_CONFIG', flowIndex: 0, phase: 'request', stepIndex: 0, configuration: config,
      });
      expect(result.flows[0].request[0].configuration).toEqual(config);
      expect(result.isDirty).toBe(true);
    });
  });

  describe('SAVE_START / SAVE_DONE / SAVE_ERROR', () => {
    it('SAVE_START sets saving to true', () => {
      const result = policyStudioReducer(initialState, { type: 'SAVE_START' });
      expect(result.saving).toBe(true);
    });

    it('SAVE_DONE resets saving and isDirty', () => {
      const state = stateWith({ saving: true, isDirty: true });
      const result = policyStudioReducer(state, { type: 'SAVE_DONE' });
      expect(result.saving).toBe(false);
      expect(result.isDirty).toBe(false);
    });

    it('SAVE_ERROR resets saving but keeps isDirty', () => {
      const state = stateWith({ saving: true, isDirty: true });
      const result = policyStudioReducer(state, { type: 'SAVE_ERROR' });
      expect(result.saving).toBe(false);
      expect(result.isDirty).toBe(true);
    });
  });

  it('returns same state for unknown action', () => {
    const state = stateWith({ flows: [flow('A')] });
    const result = policyStudioReducer(state, { type: 'UNKNOWN' } as never);
    expect(result).toBe(state);
  });
});
