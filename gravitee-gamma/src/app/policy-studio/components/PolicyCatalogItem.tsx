import { useDraggable } from '@dnd-kit/core';
import { cn } from '@baros/lib/utils';
import type { PolicyPlugin, DragData } from '../types';

interface PolicyCatalogItemProps {
  readonly policy: PolicyPlugin;
}

export function PolicyCatalogItem({ policy }: PolicyCatalogItemProps) {
  const dragData: DragData = { type: 'policy', policyId: policy.id };

  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: `catalog-${policy.id}`,
    data: dragData,
  });

  const name = policy.name || policy.id;

  return (
    <div
      ref={setNodeRef}
      className={cn(
        'cursor-grab touch-none rounded-md border border-transparent px-3 py-2 text-sm transition-colors',
        'hover:border-border hover:bg-accent',
        isDragging && 'opacity-50',
      )}
      {...attributes}
      {...listeners}
    >
      <div className="font-medium">{name}</div>
      {policy.description && (
        <div className="mt-0.5 truncate text-xs text-muted-foreground">
          {policy.description}
        </div>
      )}
    </div>
  );
}
