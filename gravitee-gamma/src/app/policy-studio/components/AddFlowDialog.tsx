import { useState } from 'react';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetFooter,
} from '@baros/components/ui/sheet';
import { Button } from '@baros/components/ui/button';
import type { Flow, HttpMethod } from '../types';
import { newFlow } from '../policy-studio.utils';

interface AddFlowDialogProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly onAdd: (flow: Flow) => void;
}

const HTTP_METHODS: HttpMethod[] = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'];

export function AddFlowDialog({ open, onOpenChange, onAdd }: AddFlowDialogProps) {
  const [name, setName] = useState('');
  const [path, setPath] = useState('/');
  const [methods, setMethods] = useState<HttpMethod[]>([]);

  function resetForm() {
    setName('');
    setPath('/');
    setMethods([]);
  }

  function handleOpenChange(isOpen: boolean) {
    if (!isOpen) resetForm();
    onOpenChange(isOpen);
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const flowName = name.trim() || 'New flow';
    onAdd(newFlow(flowName, path, methods.length > 0 ? methods : undefined));
    resetForm();
    onOpenChange(false);
  }

  function toggleMethod(method: HttpMethod) {
    setMethods((prev) =>
      prev.includes(method) ? prev.filter((m) => m !== method) : [...prev, method],
    );
  }

  return (
    <Sheet open={open} onOpenChange={handleOpenChange}>
      <SheetContent side="right" className="w-[400px] sm:max-w-[400px] flex flex-col">
        <SheetHeader>
          <SheetTitle>Add Flow</SheetTitle>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="flex flex-1 flex-col gap-4 py-4">
          <div className="space-y-1.5">
            <label htmlFor="flow-name" className="text-sm font-medium">
              Name
            </label>
            <input
              id="flow-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. All requests"
              className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            />
          </div>

          <div className="space-y-1.5">
            <label htmlFor="flow-path" className="text-sm font-medium">
              Path
            </label>
            <input
              id="flow-path"
              type="text"
              value={path}
              onChange={(e) => setPath(e.target.value)}
              placeholder="/"
              className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm font-mono placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            />
          </div>

          <div className="space-y-1.5">
            <span className="text-sm font-medium">Methods</span>
            <div className="flex flex-wrap gap-1.5">
              {HTTP_METHODS.map((method) => (
                <button
                  key={method}
                  type="button"
                  onClick={() => toggleMethod(method)}
                  className={`rounded-md border px-2 py-1 text-xs font-medium transition-colors ${
                    methods.includes(method)
                      ? 'border-primary bg-primary text-primary-foreground'
                      : 'border-input bg-background hover:bg-accent'
                  }`}
                >
                  {method}
                </button>
              ))}
            </div>
            {methods.length === 0 && (
              <p className="text-xs text-muted-foreground">All methods if none selected</p>
            )}
          </div>

          <div className="flex-1" />

          <SheetFooter>
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit">Add</Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  );
}
