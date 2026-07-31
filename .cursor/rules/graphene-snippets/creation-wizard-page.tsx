// Snippet: Creation wizard / stepper page with PageFocused
//
// Use when building a multi-step creation flow, onboarding sequence,
// or any focused linear task (Create API, Create Agent, Import, etc.).
//
// Content width: Wrap in <PageFocused> to constrain and center within the
// container. The centering signals to the user that they are in a focused sub-task.
//
// Do NOT use for:
// - Regular settings/form pages (they use the default container as-is)
// - DataTable list pages (they fill the container naturally)
// - Wide two-column wizards (use the default container instead)
//
// Decision rule: If the user navigated INTO a single-column creation/wizard flow → <PageFocused>.

import { useState } from 'react';
import { Button, Card, CardContent, Field, Input, Label, PageFocused, Separator } from '@gravitee/graphene-core';
import { ArrowLeftIcon, ArrowRightIcon, RocketIcon } from '@gravitee/graphene-core/icons';

// Replace with your actual step definitions
const STEPS = ['Details', 'Configure', 'Review'] as const;

export function CreateResourceWizardPage() {
  const [step, setStep] = useState(0);
  const isLastStep = step === STEPS.length - 1;

  function handleNext() {
    if (isLastStep) {
      // Replace with your create mutation
      return;
    }
    setStep((s) => s + 1);
  }

  function handleBack() {
    if (step === 0) {
      // Replace with your navigation back
      return;
    }
    setStep((s) => s - 1);
  }

  return (
    <PageFocused>
      <div className="space-y-6">
        {/* Step indicator */}
        <nav aria-label="Progress" className="flex items-center justify-center gap-2">
          {STEPS.map((label, i) => (
            <div key={label} className="flex items-center gap-2">
              <div className="flex items-center gap-1.5">
                <div
                  className={`flex size-6 items-center justify-center rounded-full text-xs font-medium ${
                    i <= step ? 'bg-primary text-primary-foreground' : 'border border-border text-muted-foreground'
                  }`}
                >
                  {i + 1}
                </div>
                <span className={`text-sm ${i <= step ? 'font-medium' : 'text-muted-foreground'}`}>{label}</span>
              </div>
              {i < STEPS.length - 1 && <div className="h-px w-8 bg-border" />}
            </div>
          ))}
        </nav>

        {/* Step content */}
        <Card>
          <CardContent className="space-y-4 pt-6">
            {step === 0 && (
              <>
                <Field>
                  <Label>Resource name</Label>
                  <Input placeholder="Enter a name" />
                </Field>
                <Field>
                  <Label>Description</Label>
                  <Input placeholder="Optional description" />
                </Field>
              </>
            )}
            {step === 1 && (
              <Field>
                <Label>Configuration</Label>
                <Input placeholder="Enter configuration value" />
              </Field>
            )}
            {step === 2 && (
              <div className="space-y-2">
                <p className="text-sm font-medium">Review your configuration</p>
                <p className="text-sm text-muted-foreground">Confirm the details below before creating.</p>
              </div>
            )}
          </CardContent>
        </Card>

        <Separator />

        {/* Navigation buttons */}
        <div className="flex items-center justify-between">
          <Button variant="outline" onClick={handleBack}>
            <ArrowLeftIcon className="size-4" aria-hidden />
            {step === 0 ? 'Cancel' : 'Back'}
          </Button>
          <Button onClick={handleNext}>
            {isLastStep ? (
              <>
                <RocketIcon className="size-4" aria-hidden />
                Create
              </>
            ) : (
              <>
                Next
                <ArrowRightIcon className="size-4" aria-hidden />
              </>
            )}
          </Button>
        </div>
      </div>
    </PageFocused>
  );
}
