import { useState, useEffect } from 'react';
import Form from '@rjsf/core';
import validator from '@rjsf/validator-ajv8';
import type { IChangeEvent } from '@rjsf/core';
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
  const [formData, setFormData] = useState<Record<string, unknown>>({});

  const stepId = step?.id ?? null;
  const stepPolicy = step?.policy ?? null;

  useEffect(() => {
    if (open && step && stepPolicy) {
      fetchSchema(stepPolicy);
      setFormData(step.configuration ?? {});
    }
  }, [open, stepId, stepPolicy, fetchSchema]);

  function handleChange(e: IChangeEvent) {
    setFormData(e.formData);
  }

  function handleSave() {
    onSave(formData);
    onOpenChange(false);
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

        <div className="flex-1 overflow-auto py-4">
          {loading && (
            <div className="text-sm text-muted-foreground">Loading schema...</div>
          )}
          {error && (
            <div className="text-sm text-destructive">Failed to load schema: {error.message}</div>
          )}
          {schema && !loading && (
            <div className="rjsf-sheet">
              <Form
                schema={schema}
                formData={formData}
                validator={validator}
                onChange={handleChange}
                liveValidate
                showErrorList={false}
              >
                {/* Hide default submit button — we use our own in SheetFooter */}
                <></>
              </Form>
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
