import { useState } from 'react';
import { Plus } from 'lucide-react';
import { Button } from '@baros/components/ui/button';
import { ScrollArea } from '@baros/components/ui/scroll-area';
import { Separator } from '@baros/components/ui/separator';
import type { Flow } from '../types';
import { FlowListItem } from './FlowListItem';
import { AddFlowDialog } from './AddFlowDialog';

interface FlowsSidebarProps {
  readonly flows: Flow[];
  readonly selectedIndex: number;
  readonly onSelect: (index: number) => void;
  readonly onAdd: (flow: Flow) => void;
  readonly onRemove: (index: number) => void;
  readonly onToggle: (index: number) => void;
}

export function FlowsSidebar({ flows, selectedIndex, onSelect, onAdd, onRemove, onToggle }: FlowsSidebarProps) {
  const [dialogOpen, setDialogOpen] = useState(false);

  return (
    <div className="flex w-60 shrink-0 flex-col border-r">
      <div className="flex items-center justify-between px-3 py-2">
        <h2 className="text-sm font-semibold">Flows</h2>
        <Button variant="ghost" size="icon" onClick={() => setDialogOpen(true)} aria-label="Add flow">
          <Plus className="h-4 w-4" />
        </Button>
      </div>
      <Separator />
      <ScrollArea className="flex-1">
        <div className="flex flex-col gap-1 p-2">
          {flows.length === 0 && (
            <div className="px-2 py-8 text-center text-xs text-muted-foreground">
              No flows yet. Click + to create one.
            </div>
          )}
          {flows.map((flow, index) => (
            <FlowListItem
              key={`flow-${index}`}
              flow={flow}
              isSelected={index === selectedIndex}
              onSelect={() => onSelect(index)}
              onRemove={() => onRemove(index)}
              onToggle={() => onToggle(index)}
            />
          ))}
        </div>
      </ScrollArea>

      <AddFlowDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        onAdd={onAdd}
      />
    </div>
  );
}
