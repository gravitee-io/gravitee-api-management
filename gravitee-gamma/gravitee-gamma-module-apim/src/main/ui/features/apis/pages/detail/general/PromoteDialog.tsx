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
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { useState } from 'react';

export interface PromoteTarget {
    id: string;
    name: string;
    promotionInProgress: boolean;
}

export type PromoteState = 'loading' | 'ready' | 'cloudNotConnected' | 'error';

const DEFAULT_COCKPIT_URL = 'https://cockpit.gravitee.io';

/**
 * Meet-Cockpit content, rendered inline inside the same <Dialog> root as every other state
 * (never as its own early-returned <Dialog>) — a different root element type between renders
 * makes React unmount/remount the dialog instead of swapping its content in place.
 */
function MeetCockpitContent({ cockpitURL }: Readonly<{ cockpitURL?: string }>) {
    return (
        <>
            <DialogHeader>
                <DialogTitle>Meet Gravitee Cloud</DialogTitle>
                <DialogDescription>
                    Gravitee Cloud is a centralized, multi-tenancy tool for monitoring all your Gravitee.io installations from one handy
                    interactive dashboard.
                </DialogDescription>
            </DialogHeader>
            <div className="py-2 text-sm">
                Create an account on{' '}
                <a href={cockpitURL ?? DEFAULT_COCKPIT_URL} target="_blank" rel="noreferrer" className="underline">
                    Gravitee Cloud
                </a>
                , register your current installation and start promoting your APIs across multiple environments.
            </div>
        </>
    );
}

function PromoteTargetsForm({
    targets,
    selectedIndex,
    onSelectedIndexChange,
    error,
}: Readonly<{
    targets: PromoteTarget[];
    selectedIndex: string;
    onSelectedIndexChange: (v: string) => void;
    error?: string | null;
}>) {
    const hasPromotionInProgress = targets.some(target => target.promotionInProgress);

    return (
        <div className="space-y-3 py-2">
            {hasPromotionInProgress && (
                <Alert>
                    <AlertTitle>Pending environments promotion</AlertTitle>
                    <AlertDescription>You will be able to promote them again after they have been accepted or rejected.</AlertDescription>
                </Alert>
            )}
            <Alert>
                <AlertTitle>Sharding tags</AlertTitle>
                <AlertDescription>
                    For your promotion to be accepted, your API deployment tags must exist in the targeted environment.
                </AlertDescription>
            </Alert>

            <div className="space-y-1">
                <Label htmlFor="promote-target">Environment</Label>
                <Select value={selectedIndex} onValueChange={onSelectedIndexChange}>
                    <SelectTrigger id="promote-target" aria-label="Environment">
                        <SelectValue placeholder="Select an environment" />
                    </SelectTrigger>
                    <SelectContent position="popper" style={{ width: 'var(--radix-select-trigger-width)', minWidth: 'unset' }}>
                        {targets.map((target, index) => (
                            <SelectItem key={`${target.id}-${index}`} value={String(index)} disabled={target.promotionInProgress}>
                                {target.name}
                                {target.promotionInProgress ? ' (pending)' : ''}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </div>

            <p className="text-xs text-muted-foreground">
                Members and groups are not transferred. The API is promoted as a stopped, private API in the target environment.
            </p>

            {error && <p className="text-sm text-destructive">{error}</p>}
        </div>
    );
}

export function PromoteDialog({
    open,
    onOpenChange,
    state,
    targets,
    cockpitURL,
    loadErrorMessage,
    onPromote,
    isPromoting,
    error,
}: Readonly<{
    open: boolean;
    onOpenChange: (v: boolean) => void;
    state: PromoteState;
    targets: PromoteTarget[];
    cockpitURL?: string;
    loadErrorMessage?: string;
    onPromote: (target: { targetEnvCockpitId: string; targetEnvName: string }) => void;
    isPromoting: boolean;
    error?: string | null;
}>) {
    // Selection is keyed by array index rather than target.id: Radix Select requires each
    // SelectItem's `value` to be unique, but a misconfigured Cockpit installation can report the
    // same cockpit environment id for two different environments (see APIM-14980 testing notes) —
    // an id collision would otherwise make Radix treat both options as "the same" selection.
    const [selectedIndex, setSelectedIndex] = useState('');
    const [prevOpen, setPrevOpen] = useState(open);

    if (prevOpen !== open) {
        setPrevOpen(open);
        if (open) {
            setSelectedIndex('');
        }
    }

    const firstAvailableIndex = targets.findIndex(target => !target.promotionInProgress);
    if (state === 'ready' && selectedIndex === '' && firstAvailableIndex !== -1) {
        setSelectedIndex(String(firstAvailableIndex));
    }

    function handlePromote() {
        const target = targets[Number(selectedIndex)];
        if (!target) return;
        onPromote({ targetEnvCockpitId: target.id, targetEnvName: target.name });
    }

    // One <Dialog>/<DialogContent> root for every state: swapping the root element type between
    // renders (e.g. returning a different component per state) makes React unmount/remount the
    // dialog instead of updating its content in place.
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent style={{ maxWidth: '32rem' }}>
                {state === 'cloudNotConnected' ? (
                    <MeetCockpitContent cockpitURL={cockpitURL} />
                ) : (
                    <DialogHeader>
                        <DialogTitle>Promote the API</DialogTitle>
                        <DialogDescription>Replicate this API to another environment via Gravitee Cloud.</DialogDescription>
                    </DialogHeader>
                )}

                {state === 'loading' && <p className="py-2 text-sm text-muted-foreground">Loading…</p>}

                {state === 'error' && (
                    <Alert variant="destructive">
                        <AlertTitle>Could not load promotion targets</AlertTitle>
                        <AlertDescription>{loadErrorMessage}</AlertDescription>
                    </Alert>
                )}

                {state === 'ready' && targets.length === 0 && (
                    <Alert variant="warning">
                        <AlertDescription>No environment is available to promote this API.</AlertDescription>
                    </Alert>
                )}

                {state === 'ready' && targets.length > 0 && (
                    <PromoteTargetsForm
                        targets={targets}
                        selectedIndex={selectedIndex}
                        onSelectedIndexChange={setSelectedIndex}
                        error={error}
                    />
                )}

                <DialogFooter>
                    <DialogClose asChild>
                        <Button type="button" variant="outline">
                            {state === 'cloudNotConnected' ? 'Ok' : 'Cancel'}
                        </Button>
                    </DialogClose>
                    {state === 'ready' && (
                        <Button type="button" disabled={targets.length === 0 || !selectedIndex || isPromoting} onClick={handlePromote}>
                            {isPromoting ? 'Promoting…' : 'Promote'}
                        </Button>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
