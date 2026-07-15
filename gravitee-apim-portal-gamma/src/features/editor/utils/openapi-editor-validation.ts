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
import type { OpenApiSpecSource } from '../../portals/types';
import { parseOpenApiContent, type OpenApiValidationResult } from './parse-openapi-spec';
import { isRemoteSpecSourceType } from './spec-source-options';

type SourceType = OpenApiSpecSource['type'];

export interface OpenApiEditorValidationState {
    readonly showValidationStatus: boolean;
    readonly validationLoading: boolean;
    readonly validation: OpenApiValidationResult;
}

interface GetOpenApiEditorValidationStateParams {
    readonly sourceType: SourceType;
    readonly inlineContent: string;
    readonly specContent: string;
    readonly loadingSpec: boolean;
    readonly hasApiAncestor: boolean;
    readonly apiSpecResolved: boolean;
    readonly remoteSpecSynced: boolean;
}

const hiddenValidation: OpenApiEditorValidationState = {
    showValidationStatus: false,
    validationLoading: false,
    validation: { valid: false },
};

const loadingValidation: OpenApiEditorValidationState = {
    showValidationStatus: true,
    validationLoading: true,
    validation: { valid: false },
};

export function getOpenApiEditorValidationState({
    sourceType,
    inlineContent,
    specContent,
    loadingSpec,
    hasApiAncestor,
    apiSpecResolved,
    remoteSpecSynced,
}: GetOpenApiEditorValidationStateParams): OpenApiEditorValidationState {
    if (sourceType === 'INLINE') {
        if (!inlineContent.trim()) {
            return hiddenValidation;
        }

        return {
            showValidationStatus: true,
            validationLoading: false,
            validation: parseOpenApiContent(inlineContent),
        };
    }

    if (sourceType === 'API') {
        if (!hasApiAncestor) {
            return hiddenValidation;
        }

        if (loadingSpec) {
            return loadingValidation;
        }

        if (!apiSpecResolved || !specContent.trim()) {
            return hiddenValidation;
        }

        return {
            showValidationStatus: true,
            validationLoading: false,
            validation: parseOpenApiContent(specContent),
        };
    }

    if (isRemoteSpecSourceType(sourceType)) {
        if (loadingSpec) {
            return loadingValidation;
        }

        if (!remoteSpecSynced || !specContent.trim()) {
            return hiddenValidation;
        }

        return {
            showValidationStatus: true,
            validationLoading: false,
            validation: parseOpenApiContent(specContent),
        };
    }

    return hiddenValidation;
}
