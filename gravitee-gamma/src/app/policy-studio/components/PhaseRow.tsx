import { useDroppable } from '@dnd-kit/core';
import { SortableContext, horizontalListSortingStrategy } from '@dnd-kit/sortable';
import { ChevronRight, Circle } from 'lucide-react';
import { cn } from '@baros/lib/utils';
import type { Step, Phase, StepKey, PolicyPlugin } from '../types';
import { StepCard } from './StepCard';
import { InsertPolicyPopover } from './InsertPolicyPopover';

interface PhaseRowProps {
  readonly label: string;
  readonly phase: Phase;
  readonly steps: Step[];
  readonly flowIndex: number;
  readonly selectedStepKey: StepKey | null;
  readonly compatiblePolicies?: PolicyPlugin[];
  readonly onStepSelect: (key: StepKey) => void;
  readonly onStepRemove: (phase: Phase, stepIndex: number) => void;
  readonly onInsertStep?: (phase: Phase, atIndex: number, policyId: string) => void;
  readonly dropState?: 'compatible' | 'incompatible' | null;
}

function Arrow() {
  return <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground/50" />;
}

function EndpointBadge({ label, variant }: { label: string; variant: 'start' | 'end' }) {
  return (
    <div className={cn(
      'flex shrink-0 items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium',
      variant === 'start'
        ? 'border-blue-200 bg-blue-50 text-blue-700 dark:border-blue-800 dark:bg-blue-950 dark:text-blue-300'
        : 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-300',
    )}>
      <Circle className="h-2 w-2 fill-current" />
      {label}
    </div>
  );
}

export function PhaseRow({ label, phase, steps, flowIndex, selectedStepKey, compatiblePolicies, onStepSelect, onStepRemove, onInsertStep, dropState }: PhaseRowProps) {
  const droppableId = `phase-${flowIndex}-${phase}`;
  const { setNodeRef, isOver } = useDroppable({
    id: droppableId,
    data: { type: 'phase', phase, flowIndex },
  });

  const stepIds = steps.map((s) => s.id);

  const isSelected = (index: number) =>
    selectedStepKey?.phase === phase && selectedStepKey.index === index;

  const startLabel = phase === 'request' ? 'Entrypoint' : 'Endpoint';
  const endLabel = phase === 'request' ? 'Endpoint' : 'Entrypoint';

  return (
    <div className="flex flex-col gap-2">
      <h3 className={cn(
        'text-xs font-semibold uppercase tracking-wider',
        phase === 'request' ? 'text-blue-600 dark:text-blue-400' : 'text-emerald-600 dark:text-emerald-400',
      )}>
        {label}
      </h3>
      <SortableContext items={stepIds} strategy={horizontalListSortingStrategy}>
        <div
          ref={setNodeRef}
          className={cn(
            'group/phase flex min-h-[52px] items-center gap-1 overflow-x-auto rounded-md border border-dashed p-3 transition-colors',
            isOver && dropState === 'compatible' && 'border-green-500 bg-green-500/5',
            isOver && dropState === 'incompatible' && 'border-destructive bg-destructive/5',
            !isOver && 'border-border/50 bg-muted/30',
          )}
        >
          <EndpointBadge label={startLabel} variant="start" />

          {steps.length === 0 && (
            <>
              <Arrow />
              <div className="px-4 py-2 text-xs text-muted-foreground">
                Drop a policy here
              </div>
            </>
          )}

          {steps.map((step, index) => (
            <div key={step.id} className="flex items-center gap-1">
              <Arrow />
              {onInsertStep && compatiblePolicies && (
                <InsertPolicyPopover
                  policies={compatiblePolicies}
                  onSelect={(policyId) => onInsertStep(phase, index, policyId)}
                />
              )}
              <StepCard
                step={step}
                phase={phase}
                index={index}
                flowIndex={flowIndex}
                isSelected={isSelected(index)}
                onSelect={onStepSelect}
                onRemove={onStepRemove}
              />
            </div>
          ))}

          <Arrow />
          <EndpointBadge label={endLabel} variant="end" />

          {isOver && dropState === 'incompatible' && (
            <div className="ml-2 text-xs text-destructive">
              Incompatible phase
            </div>
          )}
        </div>
      </SortableContext>
    </div>
  );
}
