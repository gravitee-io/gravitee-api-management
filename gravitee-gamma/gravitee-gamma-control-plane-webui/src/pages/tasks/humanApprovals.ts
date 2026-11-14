/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useCallback, useEffect, useRef, useSyncExternalStore } from 'react';
import { useLocation } from 'react-router-dom';
import { create } from 'zustand';

import { AIM_MODULE_ID, HUMAN_APPROVALS_INBOX_ROUTE } from './tasks.mapping';
import type { HumanApproval } from './tasks.types';
import { useAuthStore } from '../../features/auth/auth.store';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import { useModulesStore } from '../../features/modules/modules.store';
import { reloadEnvironmentPermissions } from '../../features/permissions/permission-sync';
import { gammaApi } from '../../shared/api/api-client';
import { permissionService, useHasPermission } from '../../shared/permissions';

const HITL_FEATURE = 'hitl';
const PENDING = 'PENDING';

// READ lists the approvals; AIM's `decidable` answers none without UPDATE, so asking without it is a wasted read.
const HITL_DECISIONS_READ = 'environment-hitl_decisions-r';
const HITL_DECISIONS_UPDATE = 'environment-hitl_decisions-u';

/** One page of AIM's inbox: the task list points at the inbox, it does not replace it. */
const MAX_LISTED_APPROVALS = 25;

/** Without it a hung AIM keeps the cards on a skeleton, and every later read waiting on this one. */
const AIM_READ_TIMEOUT_MS = 10_000;

/**
 * `off`: nothing to count, so everything shows as if approvals did not exist. `loading` and `failed` are a number nobody
 * knows, never a zero. `refreshError`: a later read failed, so the rows may be out of date.
 */
export type HumanApprovals =
    | { readonly status: 'off' }
    | { readonly status: 'loading' }
    | { readonly status: 'failed'; readonly error: Error }
    | {
          readonly status: 'loaded';
          readonly environmentId: string;
          readonly items: readonly HumanApproval[];
          readonly totalCount: number;
          readonly refreshError?: Error;
      };

const OFF: HumanApprovals = { status: 'off' };
const LOADING: HumanApprovals = { status: 'loading' };
const PERMISSIONS_UNKNOWN: HumanApprovals = {
    status: 'failed',
    error: new Error('Your permissions in this environment could not be read.'),
};
const MODULES_UNKNOWN: HumanApprovals = {
    status: 'failed',
    error: new Error('The list of installed applications could not be read.'),
};

function readFromAim<T>(environmentId: string, path: string): Promise<T> {
    const signal = AbortSignal.timeout(AIM_READ_TIMEOUT_MS);
    return gammaApi
        .get<T>(`/environments/${encodeURIComponent(environmentId)}/modules/${AIM_MODULE_ID}/${path}`, { signal })
        .catch((error: unknown) => {
            throw signal.aborted ? new Error('AIM did not answer in time.') : error;
        });
}

function readHitlEnabled(environmentId: string): Promise<boolean> {
    return readFromAim<{ features: string[] }>(environmentId, '_settings').then(settings => settings.features.includes(HITL_FEATURE));
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
}

function isHumanApproval(value: unknown): value is HumanApproval {
    if (!isRecord(value) || !isRecord(value.caller) || !isRecord(value.rule)) {
        return false;
    }
    const { applicationName } = value.caller;
    return (
        typeof value.id === 'string' &&
        typeof value.status === 'string' &&
        typeof value.toolName === 'string' &&
        typeof value.caller.applicationId === 'string' &&
        (applicationName === undefined || applicationName === null || typeof applicationName === 'string') &&
        typeof value.rule.name === 'string' &&
        typeof value.createdAt === 'string' &&
        !Number.isNaN(Date.parse(value.createdAt))
    );
}

/** Checked here, where a failure is reported: rendering an answer that is not what it should be would take the shell down. */
function parseHumanApprovalPage(answer: unknown): { items: HumanApproval[]; totalCount: number } {
    if (isRecord(answer) && Array.isArray(answer.data) && answer.data.every(isHumanApproval) && isRecord(answer.pagination)) {
        const { totalCount } = answer.pagination;
        if (typeof totalCount === 'number' && Number.isInteger(totalCount) && totalCount >= 0) {
            return { items: answer.data, totalCount };
        }
    }
    throw new Error('AIM sent an answer that cannot be read.');
}

async function readDecidableApprovals(environmentId: string): Promise<HumanApprovals> {
    const query = new URLSearchParams({ status: PENDING, decidable: 'true', page: '1', perPage: String(MAX_LISTED_APPROVALS) });
    const { items, totalCount } = parseHumanApprovalPage(await readFromAim<unknown>(environmentId, `approvals?${query}`));
    // AIM filters on the status stored but shows one past its deadline as EXPIRED until its sweep settles it, a minute at most.
    const pending = items.filter(approval => approval.status === PENDING);
    // The total counts them too, and may already have settled some of them: never below zero.
    return { status: 'loaded', environmentId, items: pending, totalCount: Math.max(0, totalCount - (items.length - pending.length)) };
}

interface HumanApprovalsState {
    readonly approvals: HumanApprovals;
    /** Kept until sign-out, as it changes only when the installation is reconfigured and restarted. */
    readonly hitlEnabled: boolean | null;
    /** The read whose answer counts, compared by identity: any other answer is late and dropped. */
    readonly currentRead: symbol | null;
    /** An answer from after the current read started was asked for: one more read follows it. */
    readonly readAgain: boolean;
    /** `fromNow`: the answer must be read after this call, as after a decision; otherwise a running read is joined. */
    readonly read: (environmentId: string, fromNow?: boolean) => Promise<void>;
    /** Drops the approvals, for an environment that cannot be asked. */
    readonly close: () => void;
    readonly reset: () => void;
}

const initialState = {
    approvals: LOADING,
    hitlEnabled: null,
    currentRead: null,
    readAgain: false,
};

/**
 * One result for the shell's badge, the page and the sheet, so that they can never show different counts: the badge has no
 * timer to catch up with a read made elsewhere.
 */
export const useHumanApprovalsStore = create<HumanApprovalsState>((set, get) => ({
    ...initialState,

    close: () => set({ approvals: LOADING, currentRead: null, readAgain: false }),

    reset: () => set(initialState),

    read: async (environmentId, fromNow = false) => {
        if (get().currentRead) {
            if (fromNow) {
                set({ readAgain: true });
            }
            return;
        }
        const read = Symbol('human approvals read');
        set({ currentRead: read });
        const isCurrent = () => get().currentRead === read;

        const settle = (approvals: HumanApprovals) => {
            if (!isCurrent()) {
                return;
            }
            const { readAgain } = get();
            set({ approvals, currentRead: null, readAgain: false });
            if (readAgain) {
                void get().read(environmentId);
            }
        };
        try {
            let { hitlEnabled } = get();
            if (hitlEnabled === null) {
                hitlEnabled = await readHitlEnabled(environmentId);
                set({ hitlEnabled });
            }
            settle(hitlEnabled ? await readDecidableApprovals(environmentId) : OFF);
        } catch (e) {
            const error = e instanceof Error ? e : new Error(String(e));
            const { approvals } = get();
            settle(approvals.status === 'loaded' ? { ...approvals, refreshError: error } : { status: 'failed', error });
        }
    },
}));

// What one user was shown is not the next one's to start from.
useAuthStore.subscribe((state, previous) => {
    if (state.user !== previous.user) {
        useHumanApprovalsStore.getState().reset();
    }
});

/** The status when the current environment cannot be asked yet or at all, `null` when it can. */
function useClosedStatus(environmentId: string): HumanApprovals | null {
    const listStatus = useModulesStore(s => s.listStatus);
    const hasAimModule = useModulesStore(s => s.modules.some(m => m.id === AIM_MODULE_ID));
    const mayDecide = useHasPermission({ allOf: [HITL_DECISIONS_READ, HITL_DECISIONS_UPDATE] });
    const permissionsStatus = useSyncExternalStore(
        listener => permissionService.subscribe(listener),
        () => permissionService.getLoadStatus('environment'),
    );
    if (listStatus === 'failed') {
        return MODULES_UNKNOWN;
    }
    if (listStatus === 'pending') {
        return LOADING;
    }
    if (!hasAimModule) {
        return OFF;
    }
    if (permissionsStatus === 'failed') {
        return PERMISSIONS_UNKNOWN;
    }
    if (permissionsStatus === 'pending' || !environmentId) {
        return LOADING;
    }
    return mayDecide ? null : OFF;
}

interface UseHumanApprovalsResult {
    readonly humanApprovals: HumanApprovals;
    readonly refreshing: boolean;
    readonly refresh: () => void;
}

/**
 * The approvals waiting for the signed-in user. Read when mounted, when the tab comes back, when the user leaves AIM's
 * approval pages, where they may have decided some, and on `refresh`, and on no timer: the badge is mounted on every page of
 * every signed-in user, and one read costs AIM several queries whatever the page size.
 */
export function useHumanApprovals(): UseHumanApprovalsResult {
    const environmentId = useEnvironmentStore(s => s.environmentId);
    const closedStatus = useClosedStatus(environmentId);
    const readableEnvironmentId = closedStatus ? null : environmentId;
    const read = useHumanApprovalsStore(s => s.read);
    const close = useHumanApprovalsStore(s => s.close);
    const requestModuleList = useModulesStore(s => s.requestList);
    const approvals = useHumanApprovalsStore(s => s.approvals);
    const refreshing = useHumanApprovalsStore(s => s.currentRead !== null);

    useEffect(() => {
        if (!readableEnvironmentId) {
            close();
            return;
        }
        const readIfVisible = () => {
            if (!document.hidden) {
                void read(readableEnvironmentId);
            }
        };
        void read(readableEnvironmentId);
        document.addEventListener('visibilitychange', readIfVisible);
        window.addEventListener('focus', readIfVisible);
        return () => {
            document.removeEventListener('visibilitychange', readIfVisible);
            window.removeEventListener('focus', readIfVisible);
        };
    }, [readableEnvironmentId, read, close]);

    const inAimApprovals = useLocation().pathname.includes(HUMAN_APPROVALS_INBOX_ROUTE);
    const wasInAimApprovals = useRef(inAimApprovals);
    useEffect(() => {
        if (wasInAimApprovals.current && !inAimApprovals && readableEnvironmentId) {
            void read(readableEnvironmentId, true);
        }
        wasInAimApprovals.current = inAimApprovals;
    }, [inAimApprovals, readableEnvironmentId, read]);

    const refresh = useCallback(() => {
        if (readableEnvironmentId) {
            void read(readableEnvironmentId, true);
        } else if (closedStatus === PERMISSIONS_UNKNOWN) {
            reloadEnvironmentPermissions();
        } else if (closedStatus === MODULES_UNKNOWN) {
            requestModuleList();
        }
    }, [readableEnvironmentId, closedStatus, read, requestModuleList]);

    return { humanApprovals: closedStatus ?? approvals, refreshing, refresh };
}
