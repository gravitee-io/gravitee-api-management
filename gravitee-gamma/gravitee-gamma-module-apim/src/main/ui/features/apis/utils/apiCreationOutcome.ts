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
import type { ApiProxyDraft } from '../types/apiCreation';

export interface ApiCreationOutcome {
    readonly askForReview: boolean;
    readonly deployImmediately: boolean;
}

/**
 * What happens right after the API is created. With API Review enabled the gateway refuses to start an
 * unreviewed API, so the deploy switch is replaced by the ask switch and an immediate deploy is never sent.
 */
export function resolveCreationOutcome(
    form: Pick<ApiProxyDraft, 'askForReview' | 'deployImmediately'>,
    apiReviewEnabled: boolean,
): ApiCreationOutcome {
    if (apiReviewEnabled) {
        return { askForReview: form.askForReview, deployImmediately: false };
    }
    return { askForReview: false, deployImmediately: form.deployImmediately };
}

export function creationButtonLabel(outcome: ApiCreationOutcome): string {
    if (outcome.askForReview) return 'Create & ask for review';
    if (outcome.deployImmediately) return 'Create & Deploy';
    return 'Create API';
}
