/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Alert, AlertDescription, Button, Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@gravitee/graphene-core';

export type PlanDialogAction = 'publish' | 'deprecate' | 'close' | 'delete';

const ACTION_CONFIG: Record<PlanDialogAction, { title: string; body: string; confirmLabel: string; destructive: boolean }> = {
    publish: {
        title: 'Publish plan?',
        body: 'This will make the plan available for subscriptions.',
        confirmLabel: 'Publish',
        destructive: false,
    },
    deprecate: {
        title: 'Deprecate plan?',
        body: 'Existing subscriptions continue. No new subscriptions will be accepted.',
        confirmLabel: 'Deprecate',
        destructive: false,
    },
    close: {
        title: 'Close plan?',
        body: 'All active subscriptions will be closed. This cannot be undone.',
        confirmLabel: 'Close',
        destructive: true,
    },
    delete: {
        title: 'Delete plan?',
        body: 'This permanently removes the plan. It cannot have active subscribers.',
        confirmLabel: 'Delete',
        destructive: true,
    },
};

interface PlanActionConfirmDialogProps {
    open: boolean;
    action: PlanDialogAction | null;
    isPending: boolean;
    error: string | null;
    onConfirm: () => void;
    onClose: () => void;
}

export function PlanActionConfirmDialog({ open, action, isPending, error, onConfirm, onClose }: Readonly<PlanActionConfirmDialogProps>) {
    if (!action) return null;
    const { title, body, confirmLabel, destructive } = ACTION_CONFIG[action];

    return (
        <Dialog open={open} onOpenChange={open => !open && onClose()}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                </DialogHeader>
                <p className="text-sm text-muted-foreground">{body}</p>
                {error && (
                    <Alert variant="destructive">
                        <AlertDescription>{error}</AlertDescription>
                    </Alert>
                )}
                <DialogFooter>
                    <Button type="button" variant="outline" onClick={onClose} disabled={isPending}>
                        Cancel
                    </Button>
                    <Button type="button" variant={destructive ? 'destructive' : 'default'} onClick={onConfirm} disabled={isPending}>
                        {isPending ? `${confirmLabel}…` : confirmLabel}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
