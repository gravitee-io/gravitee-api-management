import { MoreVertical } from 'lucide-react';
import { cn } from '@baros/lib/utils';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@baros/components/ui/dropdown-menu';
import type { Flow } from '../types';

interface FlowListItemProps {
  readonly flow: Flow;
  readonly isSelected: boolean;
  readonly onSelect: () => void;
  readonly onRemove: () => void;
  readonly onToggle: () => void;
}

export function FlowListItem({ flow, isSelected, onSelect, onRemove, onToggle }: FlowListItemProps) {
  const flowName = flow.name ?? 'Unnamed flow';
  const selector = flow.selectors?.[0];
  const path = selector?.type === 'HTTP' ? selector.path : undefined;
  const methods = selector?.type === 'HTTP' ? selector.methods : undefined;

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onSelect}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onSelect();
        }
      }}
      className={cn(
        'flex w-full items-start gap-2 rounded-md border p-2 text-left text-sm transition-colors cursor-pointer',
        'hover:bg-accent',
        isSelected ? 'border-primary bg-accent' : 'border-transparent',
        !flow.enabled && 'opacity-40',
      )}
    >
      <div className="flex-1 min-w-0">
        <div className="truncate font-medium">{flowName}</div>
        {path && (
          <div className="mt-0.5 truncate text-xs text-muted-foreground">{path}</div>
        )}
        {methods && methods.length > 0 && (
          <div className="mt-1 flex flex-wrap gap-1">
            {methods.map((m) => (
              <span
                key={m}
                className="rounded bg-secondary px-1 py-0.5 text-[10px] font-medium text-secondary-foreground"
              >
                {m}
              </span>
            ))}
          </div>
        )}
      </div>

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            className="shrink-0 rounded p-0.5 hover:bg-muted"
            aria-label={`Options for ${flowName}`}
            onClick={(e) => e.stopPropagation()}
          >
            <MoreVertical className="h-4 w-4 text-muted-foreground" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onClick={onToggle}>
            {flow.enabled ? 'Disable' : 'Enable'}
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={onRemove} className="text-destructive">
            Delete
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
