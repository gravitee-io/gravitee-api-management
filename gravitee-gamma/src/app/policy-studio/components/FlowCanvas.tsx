import { Separator } from '@baros/components/ui/separator';
import type { Flow, Phase, StepKey, PolicyPlugin } from '../types';
import { PhaseRow } from './PhaseRow';

interface FlowCanvasProps {
  readonly flow: Flow | null;
  readonly flowIndex: number;
  readonly selectedStepKey: StepKey | null;
  readonly requestCompatiblePolicies?: PolicyPlugin[];
  readonly responseCompatiblePolicies?: PolicyPlugin[];
  readonly onStepSelect: (key: StepKey) => void;
  readonly onStepRemove: (phase: Phase, stepIndex: number) => void;
  readonly onInsertStep?: (phase: Phase, atIndex: number, policyId: string) => void;
  readonly requestDropState?: 'compatible' | 'incompatible' | null;
  readonly responseDropState?: 'compatible' | 'incompatible' | null;
}

export function FlowCanvas({ flow, flowIndex, selectedStepKey, requestCompatiblePolicies, responseCompatiblePolicies, onStepSelect, onStepRemove, onInsertStep, requestDropState, responseDropState }: FlowCanvasProps) {
  if (!flow) {
    return (
      <div className="flex flex-1 items-center justify-center text-muted-foreground">
        Select a flow to view its policies
      </div>
    );
  }

  const flowName = flow.name ?? 'Unnamed flow';

  return (
    <div className="flex flex-1 flex-col gap-6 overflow-auto p-6">
      <div className="flex items-center gap-2">
        <h2 className="text-lg font-semibold">{flowName}</h2>
        {!flow.enabled && (
          <span className="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">
            Disabled
          </span>
        )}
      </div>

      <PhaseRow
        label="Request"
        phase="request"
        steps={flow.request ?? []}
        flowIndex={flowIndex}
        selectedStepKey={selectedStepKey}
        onStepSelect={onStepSelect}
        onStepRemove={onStepRemove}
        compatiblePolicies={requestCompatiblePolicies}
        onInsertStep={onInsertStep}
        dropState={requestDropState}
      />

      <Separator />

      <PhaseRow
        label="Response"
        phase="response"
        steps={flow.response ?? []}
        flowIndex={flowIndex}
        selectedStepKey={selectedStepKey}
        onStepSelect={onStepSelect}
        onStepRemove={onStepRemove}
        compatiblePolicies={responseCompatiblePolicies}
        onInsertStep={onInsertStep}
        dropState={responseDropState}
      />
    </div>
  );
}
