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
import {
    Alert,
    AlertAction,
    AlertDescription,
    AlertTitle,
    Button,
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
} from '@gravitee/graphene-core';
import { TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useId, useState } from 'react';

/** Classic console caps the deployment label at 32 characters. */
const DEPLOYMENT_LABEL_MAX_LENGTH = 32;

export function DeployBanner({ subject, onDeploy, isPending }: { subject: string; onDeploy: () => void; isPending: boolean }) {
    return (
        // A persistent status banner, not an interruption — override Alert's assertive `role="alert"`.
        <Alert variant="warning" role="region" aria-label="Deploy status" className="rounded-none border-0 border-b px-6">
            <TriangleAlertIcon aria-hidden="true" />
            <AlertTitle>This {subject} is out of sync</AlertTitle>
            <AlertDescription>Your latest changes are not live yet. Deploy to push them to the gateway.</AlertDescription>
            <AlertAction align="center">
                <Button size="sm" onClick={onDeploy} disabled={isPending}>
                    {isPending ? 'Deploying…' : `Deploy ${subject}`}
                </Button>
            </AlertAction>
        </Alert>
    );
}

/**
 * Confirms a deployment. With `askForLabel`, it also collects an optional deployment label, as the classic console
 * deploy dialog does; leave it off where the deployment endpoint takes no label.
 */
export function DeployConfirmDialog({
    subject,
    askForLabel = false,
    open,
    isPending,
    onConfirm,
    onOpenChange,
}: {
    subject: string;
    askForLabel?: boolean;
    open: boolean;
    isPending: boolean;
    onConfirm: (label: string) => void;
    onOpenChange: (open: boolean) => void;
}) {
    const labelId = useId();
    const [label, setLabel] = useState('');

    const handleConfirm = () => onConfirm(label.trim());

    return (
        <Dialog
            open={open}
            onOpenChange={next => {
                if (!next) setLabel('');
                onOpenChange(next);
            }}
        >
            <DialogContent className="max-w-md">
                <DialogHeader>
                    <DialogTitle>Deploy your {subject}</DialogTitle>
                    <DialogDescription>
                        {askForLabel
                            ? 'Provide a label to identify your deployment.'
                            : 'Your latest changes will be pushed to the gateway.'}
                    </DialogDescription>
                </DialogHeader>
                {askForLabel ? (
                    <div className="space-y-2">
                        <Label htmlFor={labelId}>Deployment label (optional)</Label>
                        <Input
                            id={labelId}
                            maxLength={DEPLOYMENT_LABEL_MAX_LENGTH}
                            placeholder="e.g. hotfix-cors, v2.1-release"
                            value={label}
                            onChange={e => setLabel(e.target.value)}
                            onKeyDown={e => e.key === 'Enter' && handleConfirm()}
                        />
                        <p className="text-right text-xs text-muted-foreground">
                            {label.length}/{DEPLOYMENT_LABEL_MAX_LENGTH}
                        </p>
                    </div>
                ) : null}
                <DialogFooter>
                    <DialogClose asChild>
                        <Button variant="outline" size="sm">
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button size="sm" onClick={handleConfirm} disabled={isPending}>
                        {isPending ? 'Deploying…' : 'Deploy'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
