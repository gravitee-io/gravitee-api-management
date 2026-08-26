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
import type { FlowMode, Step } from '@gravitee/graphene-policy-studio';

/**
 * A step as the v2 wire format stores it, in a flow's `pre` or `post` list. V2 API flows and the
 * organization's platform flows both use this format; each declares which of these fields it guarantees.
 */
export interface V2Step {
    readonly name?: string;
    readonly policy?: string;
    readonly description?: string;
    readonly configuration?: unknown;
    readonly enabled?: boolean;
    readonly condition?: string;
}

/** Converts a v2 `pre`/`post` step to the step the Policy Studio renders. */
export function toStudioStep(step: V2Step): Step {
    return {
        policy: step.policy,
        name: step.name,
        description: step.description,
        enabled: step.enabled,
        configuration: step.configuration,
        condition: step.condition,
    };
}

/**
 * The v2 formats store a `flowMode` where the studio reads a `FlowExecution`. The mode is narrowed on the
 * way out so callers whose own type requires it stay satisfied.
 */
export function toFlowExecution(flowMode: FlowMode | undefined): { readonly mode: FlowMode } {
    return { mode: flowMode ?? 'DEFAULT' };
}
