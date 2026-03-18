import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { GripVertical, X } from 'lucide-react';
import { cn } from '@baros/lib/utils';
import type { Step, StepKey, Phase, DragData } from '../types';

interface StepCardProps {
  readonly step: Step;
  readonly phase: Phase;
  readonly index: number;
  readonly flowIndex: number;
  readonly isSelected: boolean;
  readonly onSelect: (key: StepKey) => void;
  readonly onRemove: (phase: Phase, stepIndex: number) => void;
}

export function StepCard({ step, phase, index, flowIndex, isSelected, onSelect, onRemove }: StepCardProps) {
  const name = step.name ?? step.policy ?? 'Unknown policy';
  const dragData: DragData = { type: 'step', flowIndex, phase, index };

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: step.id,
    data: dragData,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={cn(
        'group flex items-center gap-1 rounded-md border bg-background px-2 py-1.5 text-sm shadow-sm transition-colors',
        'hover:border-primary/50',
        isSelected && 'border-primary ring-1 ring-primary/20',
        !step.enabled && 'opacity-40',
        isDragging && 'opacity-50',
      )}
    >
      <span
        className="cursor-grab touch-none text-muted-foreground hover:text-foreground"
        aria-label={`Drag to reorder ${name}`}
        {...attributes}
        {...listeners}
      >
        <GripVertical className="h-3.5 w-3.5" />
      </span>

      <button
        type="button"
        className="flex-1 truncate text-left"
        onClick={() => onSelect({ phase, index })}
      >
        {name}
      </button>

      <button
        type="button"
        className="shrink-0 rounded p-0.5 text-muted-foreground opacity-0 transition-opacity hover:text-destructive group-hover:opacity-100 focus-visible:opacity-100"
        onClick={() => onRemove(phase, index)}
        aria-label={`Remove ${name}`}
      >
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}
