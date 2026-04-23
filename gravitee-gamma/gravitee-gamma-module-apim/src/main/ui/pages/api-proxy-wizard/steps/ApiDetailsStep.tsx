import { Alert, AlertDescription, Card, CardContent, cn } from '@gravitee/graphene-core';
import { BookOpen, Info } from 'lucide-react';

import type { ApiDetailsModel } from '../apiProxyWizardModels';

type Props = {
    readonly value: ApiDetailsModel;
    readonly onChange: (patch: Partial<ApiDetailsModel>) => void;
};

export function ApiDetailsStep({ value, onChange }: Props) {
    return (
        <div className="space-y-5">
            <Alert className="rounded-xl border p-5 bg-muted/30">
                <Info className="size-4 shrink-0 text-blue-500 mt-0.5" aria-hidden />
                <AlertDescription className="text-muted-foreground text-sm">
                    Provide some details on your API. This information will be visible on the Developer Portal once published.
                </AlertDescription>
            </Alert>

            <Card className="rounded-xl border">
                <CardContent className="p-6 space-y-5">
                    <div className="flex items-center gap-2">
                        <BookOpen className="size-4 text-primary" aria-hidden />
                        <div className="text-base font-semibold">API Details</div>
                    </div>

                    <div className="text-xs text-muted-foreground">
                        Required fields are marked with <span className="text-destructive">*</span>.
                    </div>

                    <div className="grid gap-4 sm:grid-cols-2">
                        <div className="space-y-2">
                            <label className="text-sm font-medium">
                                API Name <span className="text-destructive">*</span>
                            </label>
                            <input
                                value={value.name}
                                onChange={e => onChange({ name: e.target.value })}
                                placeholder="e.g. Payments API"
                                maxLength={50}
                                className={cn(
                                    'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none',
                                    'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                )}
                            />
                            <p className="text-xs text-muted-foreground">Public name shown to consumers in the Developer Portal.</p>
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-medium">
                                Version <span className="text-destructive">*</span>
                            </label>
                            <input
                                value={value.version}
                                onChange={e => onChange({ version: e.target.value })}
                                placeholder="1.0.0"
                                maxLength={32}
                                className={cn(
                                    'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none',
                                    'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                )}
                            />
                            <p className="text-xs text-muted-foreground">
                                For example <code className="text-[11px]">1.1</code>, <code className="text-[11px]">1.1.1</code>.
                            </p>
                        </div>
                    </div>

                    <div className="space-y-2">
                        <label className="text-sm font-medium">
                            Description <span className="text-destructive">*</span>
                        </label>
                        <textarea
                            value={value.description}
                            onChange={e => onChange({ description: e.target.value })}
                            placeholder="Describe how your API works and what it does."
                            rows={4}
                            maxLength={250}
                            className={cn(
                                'w-full rounded-md border bg-background px-3 py-2 text-sm outline-none',
                                'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                            )}
                        />
                        <div className="flex items-center justify-between text-xs text-muted-foreground">
                            <span>Helps consumers understand what your API offers.</span>
                            <span>{value.description.length}/250</span>
                        </div>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}

