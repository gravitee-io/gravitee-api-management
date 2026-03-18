import { useDroppable } from '@dnd-kit/core';
import { SortableContext, horizontalListSortingStrategy } from '@dnd-kit/sortable';
import { ChevronRight } from 'lucide-react';
import { cn } from '@baros/lib/utils';
import type { Step, Phase, StepKey } from '../types';
import { StepCard } from './StepCard';

interface PhaseRowProps {
  readonly label: string;
  readonly phase: Phase;
  readonly steps: Step[];
  readonly flowIndex: number;
  readonly selectedStepKey: StepKey | null;
  readonly onStepSelect: (key: StepKey) => void;
  readonly onStepRemove: (phase: Phase, stepIndex: number) => void;
  readonly dropState?: 'compatible' | 'incompatible' | null;
}

function Arrow() {
  return <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground/50" />;
}

export function PhaseRow({ label, phase, steps, flowIndex, selectedStepKey, onStepSelect, onStepRemove, dropState }: PhaseRowProps) {
  const droppableId = `phase-${flowIndex}-${phase}`;
  const { setNodeRef, isOver } = useDroppable({
    id: droppableId,
    data: { type: 'phase', phase, flowIndex },
  });

  const stepIds = steps.map((s) => s.id);

  const isSelected = (index: number) =>
    selectedStepKey?.phase === phase && selectedStepKey.index === index;

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
            'flex min-h-[52px] items-center gap-1 overflow-x-auto rounded-md border border-dashed p-3 transition-colors',
            isOver && dropState === 'compatible' && 'border-green-500 bg-green-500/5',
            isOver && dropState === 'incompatible' && 'border-destructive bg-destructive/5',
            !isOver && 'border-border/50 bg-muted/30',
          )}
        >
          {steps.length === 0 && (
            <div className="px-4 py-2 text-xs text-muted-foreground">
              Drop a policy here
            </div>
          )}
          {steps.map((step, index) => (
            <div key={step.id} className="flex items-center gap-1">
              {index > 0 && <Arrow />}
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
