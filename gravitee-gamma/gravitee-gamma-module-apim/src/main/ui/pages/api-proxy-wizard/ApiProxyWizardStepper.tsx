import { cn } from '@gravitee/graphene-core';
import { CheckCircle2, ClipboardCheck, BookOpen, Globe, Shield } from 'lucide-react';

import type { ApiProxyWizardStepId } from './apiProxyWizardModels';

export type WizardStep = {
    readonly id: ApiProxyWizardStepId;
    readonly label: string;
    readonly icon: React.ComponentType<{ className?: string }>;
};

export const SCRATCH_STEPS: readonly WizardStep[] = [
    { id: 'details', label: 'API Details', icon: BookOpen },
    { id: 'configure', label: 'Configure Proxy', icon: Globe },
    { id: 'secure', label: 'Secure', icon: Shield },
    { id: 'review', label: 'Review & Deploy', icon: ClipboardCheck },
] as const;

type Props = {
    readonly steps?: readonly WizardStep[];
    readonly currentStepIndex: number;
    readonly onSelectStep: (idx: number) => void;
};

export function ApiProxyWizardStepper({ steps = SCRATCH_STEPS, currentStepIndex, onSelectStep }: Props) {
    return (
        <div className="rounded-xl border bg-card p-1">
            <div className="flex items-center">
                {steps.map((ws, idx) => {
                    const active = idx === currentStepIndex;
                    const done = idx < currentStepIndex;
                    const clickable = idx <= currentStepIndex;
                    const Icon = ws.icon;

                    return (
                        <div key={ws.id} className="flex flex-1 items-center">
                            <button
                                type="button"
                                onClick={() => clickable && onSelectStep(idx)}
                                className={cn(
                                    'flex w-full items-center justify-center gap-2.5 rounded-lg px-3 py-2.5 text-sm transition-colors',
                                    active && 'bg-primary/10 text-primary font-medium',
                                    done && 'text-emerald-600 dark:text-emerald-400 hover:bg-muted/50 cursor-pointer',
                                    !active && !done && 'text-muted-foreground',
                                    clickable && !active && 'cursor-pointer',
                                )}
                                disabled={!clickable}
                            >
                                <span
                                    className={cn(
                                        'flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-medium',
                                        done && 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400',
                                        active && 'bg-primary text-primary-foreground',
                                        !active && !done && 'bg-muted text-muted-foreground',
                                    )}
                                >
                                    {done ? <CheckCircle2 className="size-3.5" /> : <Icon className="size-3.5" />}
                                </span>
                                <span className="hidden sm:inline">{ws.label}</span>
                            </button>

                            {idx < steps.length - 1 ? (
                                <div className={cn('h-px w-6 shrink-0', done ? 'bg-emerald-500/40' : 'bg-border')} />
                            ) : null}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}

