import type { Flow, Phase, Step } from './types';
import { reorder, updateFlowPhase } from './policy-studio.utils';

export interface PolicyStudioState {
  readonly flows: Flow[];
  readonly selectedFlowIndex: number;
  readonly isDirty: boolean;
  readonly saving: boolean;
}

export type PolicyStudioAction =
  | { type: 'SET_FLOWS'; flows: Flow[] }
  | { type: 'SELECT_FLOW'; index: number }
  | { type: 'ADD_FLOW'; flow: Flow }
  | { type: 'REMOVE_FLOW'; index: number }
  | { type: 'TOGGLE_FLOW_ENABLED'; index: number }
  | { type: 'ADD_STEP'; flowIndex: number; phase: Phase; step: Step }
  | { type: 'REMOVE_STEP'; flowIndex: number; phase: Phase; stepIndex: number }
  | { type: 'REORDER_STEP'; flowIndex: number; phase: Phase; from: number; to: number }
  | { type: 'UPDATE_STEP_CONFIG'; flowIndex: number; phase: Phase; stepIndex: number; configuration: Record<string, unknown> }
  | { type: 'SAVE_START' }
  | { type: 'SAVE_DONE' }
  | { type: 'SAVE_ERROR' };

export const initialState: PolicyStudioState = {
  flows: [],
  selectedFlowIndex: 0,
  isDirty: false,
  saving: false,
};

export function policyStudioReducer(state: PolicyStudioState, action: PolicyStudioAction): PolicyStudioState {
  switch (action.type) {
    case 'SET_FLOWS':
      return { ...state, flows: action.flows, isDirty: false, selectedFlowIndex: 0 };

    case 'SELECT_FLOW':
      return { ...state, selectedFlowIndex: action.index };

    case 'ADD_FLOW':
      return {
        ...state,
        flows: [...state.flows, action.flow],
        selectedFlowIndex: state.flows.length,
        isDirty: true,
      };

    case 'REMOVE_FLOW': {
      const flows = state.flows.filter((_, i) => i !== action.index);
      const selectedFlowIndex = Math.min(state.selectedFlowIndex, Math.max(0, flows.length - 1));
      return { ...state, flows, selectedFlowIndex, isDirty: true };
    }

    case 'TOGGLE_FLOW_ENABLED':
      return {
        ...state,
        flows: state.flows.map((f, i) =>
          i === action.index ? { ...f, enabled: !f.enabled } : f,
        ),
        isDirty: true,
      };

    case 'ADD_STEP':
      return {
        ...state,
        flows: state.flows.map((f, i) =>
          i === action.flowIndex
            ? updateFlowPhase(f, action.phase, (steps) => [...steps, action.step])
            : f,
        ),
        isDirty: true,
      };

    case 'REMOVE_STEP':
      return {
        ...state,
        flows: state.flows.map((f, i) =>
          i === action.flowIndex
            ? updateFlowPhase(f, action.phase, (steps) => steps.filter((_, si) => si !== action.stepIndex))
            : f,
        ),
        isDirty: true,
      };

    case 'REORDER_STEP':
      return {
        ...state,
        flows: state.flows.map((f, i) =>
          i === action.flowIndex
            ? updateFlowPhase(f, action.phase, (steps) => reorder(steps, action.from, action.to))
            : f,
        ),
        isDirty: true,
      };

    case 'UPDATE_STEP_CONFIG':
      return {
        ...state,
        flows: state.flows.map((f, i) =>
          i === action.flowIndex
            ? updateFlowPhase(f, action.phase, (steps) =>
                steps.map((s, si) =>
                  si === action.stepIndex ? { ...s, configuration: action.configuration } : s,
                ),
              )
            : f,
        ),
        isDirty: true,
      };

    case 'SAVE_START':
      return { ...state, saving: true };

    case 'SAVE_DONE':
      return { ...state, saving: false, isDirty: false };

    case 'SAVE_ERROR':
      return { ...state, saving: false };

    default:
      return state;
  }
}
