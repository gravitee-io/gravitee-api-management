import { useState, useEffect } from 'react';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
} from '@baros/components/ui/sheet';
import { Button } from '@baros/components/ui/button';
import type { Step, Phase } from '../types';
import { usePolicySchema } from '../hooks/usePolicySchema';

interface StepConfigSheetProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly step: Step | null;
  readonly phase: Phase | null;
  readonly onSave: (configuration: Record<string, unknown>) => void;
}

export function StepConfigSheet({ open, onOpenChange, step, phase, onSave }: StepConfigSheetProps) {
  const { schema, loading, error, fetchSchema } = usePolicySchema();
  const [configJson, setConfigJson] = useState('');
  const [parseError, setParseError] = useState<string | null>(null);

  const stepId = step?.id ?? null;
  const stepPolicy = step?.policy ?? null;

  useEffect(() => {
    if (open && step && stepPolicy) {
      fetchSchema(stepPolicy);
      setConfigJson(JSON.stringify(step.configuration ?? {}, null, 2));
      setParseError(null);
    }
    // Depend on stable values, not the step object reference
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, stepId, stepPolicy, fetchSchema]);

  function handleSave() {
    try {
      const parsed = JSON.parse(configJson);
      setParseError(null);
      onSave(parsed);
      onOpenChange(false);
    } catch {
      setParseError('Invalid JSON');
    }
  }

  const policyName = step?.name ?? step?.policy ?? 'Unknown policy';

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[480px] sm:max-w-[480px] flex flex-col">
        <SheetHeader>
          <SheetTitle>{policyName}</SheetTitle>
          <SheetDescription>
            {phase && `${phase.charAt(0).toUpperCase()}${phase.slice(1)} phase`}
            {step?.description && ` — ${step.description}`}
          </SheetDescription>
        </SheetHeader>

        <div className="flex-1 overflow-auto space-y-4 py-4">
          {loading && (
            <div className="text-sm text-muted-foreground">Loading schema...</div>
          )}
          {error && (
            <div className="text-sm text-destructive">Failed to load schema: {error.message}</div>
          )}
          {schema && !loading && (
            <div className="space-y-3">
              <div>
                <h4 className="text-xs font-semibold text-muted-foreground mb-1">JSON Schema</h4>
                <pre className="rounded-md bg-muted p-3 text-xs overflow-auto max-h-40">
                  {JSON.stringify(schema, null, 2)}
                </pre>
              </div>
              <div>
                <h4 className="text-xs font-semibold text-muted-foreground mb-1">Configuration</h4>
                <textarea
                  value={configJson}
                  onChange={(e) => {
                    setConfigJson(e.target.value);
                    setParseError(null);
                  }}
                  className="h-48 w-full rounded-md border border-input bg-background p-3 font-mono text-xs focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  spellCheck={false}
                />
                {parseError && (
                  <div className="mt-1 text-xs text-destructive">{parseError}</div>
                )}
              </div>
            </div>
          )}
        </div>

        <SheetFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleSave} disabled={loading}>
            Apply
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
