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
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';

import { policyStudioKeys } from './usePolicyStudioData';
import { listPolicies } from '../services/policyStudioService';
import type { ApiImportFormat, ApiImportSubmission } from '../types';

export type ImportSourceMode = 'local' | 'remote';

const OAS_VALIDATION_POLICY_ID = 'oas-validation';
const REST_TO_SOAP_POLICY_ID = 'rest-to-soap';

export function isValidHttpUrl(value: string): boolean {
    try {
        const url = new URL(value.trim());
        return url.protocol === 'http:' || url.protocol === 'https:';
    } catch {
        return false;
    }
}

export interface UseImportSourceOptionsResult {
    sourceMode: ImportSourceMode;
    fileName: string | null;
    fileText: string | null;
    parseError: string | null;
    remoteUrl: string;
    setRemoteUrl: (value: string) => void;
    withDocumentation: boolean;
    setWithDocumentation: (value: boolean) => void;
    withOASValidationPolicy: boolean;
    setWithOASValidationPolicy: (value: boolean) => void;
    withRestToSoap: boolean;
    hasOasValidationPolicy: boolean;
    hasRestToSoapPolicy: boolean;
    handleSourceModeChange: (next: ImportSourceMode) => void;
    handleRestToSoapChange: (checked: boolean) => void;
    handleFile: (file: File) => Promise<void>;
    canSubmit: boolean;
    buildSubmission: () => ApiImportSubmission;
    reset: () => void;
    fileAccept: string;
    fileHint: string;
    urlLabel: string;
    urlPlaceholder: string;
}

/**
 * Owns all state/logic for the "Configure file source" + "Options" sections shared by the
 * update-existing-API sheet (`ImportApiSheet`) and the create-new-API-via-import page. `format`
 * drives which fields/defaults apply; `policiesEnabled` gates the `listPolicies()` query (e.g. only
 * while a sheet is actually open).
 */
export function useImportSourceOptions(format: ApiImportFormat, policiesEnabled = true): UseImportSourceOptionsResult {
    const [sourceMode, setSourceMode] = useState<ImportSourceMode>('local');
    const [fileName, setFileName] = useState<string | null>(null);
    const [fileText, setFileText] = useState<string | null>(null);
    const [definition, setDefinition] = useState<unknown>(null);
    const [parseError, setParseError] = useState<string | null>(null);
    const [remoteUrl, setRemoteUrl] = useState('');
    const [withDocumentation, setWithDocumentation] = useState(format === 'openapi');
    const [withOASValidationPolicy, setWithOASValidationPolicy] = useState(false);
    const [withRestToSoap, setWithRestToSoap] = useState(false);

    const { data: policies } = useQuery({
        queryKey: policyStudioKeys.policies(),
        queryFn: listPolicies,
        enabled: policiesEnabled && (format === 'openapi' || format === 'wsdl'),
    });
    const hasOasValidationPolicy = (policies ?? []).some(p => p.id === OAS_VALIDATION_POLICY_ID);
    const hasRestToSoapPolicy = (policies ?? []).some(p => p.id === REST_TO_SOAP_POLICY_ID);

    // Seed withOASValidationPolicy's default once per (format, policy-availability) pair —
    // mirrors classic console, which defaults the toggle on only when the policy is installed.
    const [seededOasKey, setSeededOasKey] = useState<string | null>(null);
    const oasKey = `openapi:${hasOasValidationPolicy}`;
    if (format === 'openapi' && oasKey !== seededOasKey) {
        setSeededOasKey(oasKey);
        setWithOASValidationPolicy(hasOasValidationPolicy);
    }

    // Seed WSDL's options once per (rest-to-soap-availability, oas-availability) pair — mirrors classic
    // console: REST-to-SOAP defaults on only if installed, and enabling it defaults documentation + OAS
    // validation on too.
    const [seededWsdlKey, setSeededWsdlKey] = useState<string | null>(null);
    const wsdlKey = `wsdl:${hasRestToSoapPolicy}:${hasOasValidationPolicy}`;
    if (format === 'wsdl' && wsdlKey !== seededWsdlKey) {
        setSeededWsdlKey(wsdlKey);
        setWithRestToSoap(hasRestToSoapPolicy);
        setWithDocumentation(hasRestToSoapPolicy);
        setWithOASValidationPolicy(hasRestToSoapPolicy && hasOasValidationPolicy);
    }

    const resetSourceState = () => {
        setSourceMode('local');
        setFileName(null);
        setFileText(null);
        setDefinition(null);
        setParseError(null);
        setRemoteUrl('');
    };

    // Resets source + options to the defaults for the current `format` — callable directly by
    // consumers (e.g. when a sheet reopens) in addition to firing automatically on format change below.
    const reset = () => {
        resetSourceState();
        setWithDocumentation(format === 'openapi');
        setWithOASValidationPolicy(false);
        setWithRestToSoap(false);
        setSeededOasKey(null);
        setSeededWsdlKey(null);
    };

    // Reset whenever `format` changes — mirrors the "seeded key" render-time pattern used above,
    // avoiding a useEffect for a plain prop-change reaction.
    const [prevFormat, setPrevFormat] = useState(format);
    if (prevFormat !== format) {
        setPrevFormat(format);
        reset();
    }

    const handleSourceModeChange = (next: ImportSourceMode) => {
        setSourceMode(next);
        setFileName(null);
        setFileText(null);
        setDefinition(null);
        setParseError(null);
        setRemoteUrl('');
    };

    const handleRestToSoapChange = (checked: boolean) => {
        setWithRestToSoap(checked);
        if (checked) {
            setWithDocumentation(true);
            if (hasOasValidationPolicy) setWithOASValidationPolicy(true);
        }
    };

    const handleFile = async (file: File) => {
        setParseError(null);
        setFileName(file.name);
        const text = await file.text();
        if (format === 'gravitee') {
            try {
                setDefinition(JSON.parse(text) as unknown);
                setFileText(text);
            } catch {
                setParseError('Invalid JSON. Please upload a valid Gravitee API definition file.');
                setDefinition(null);
                setFileText(null);
            }
        } else {
            setDefinition(null);
            setFileText(text);
        }
    };

    const fileAccept =
        format === 'gravitee'
            ? '.json,application/json'
            : format === 'openapi'
              ? '.json,.yml,.yaml,application/json,text/yaml,text/x-yaml'
              : '.wsdl,.xml,application/xml,text/xml';
    const fileHint = format === 'gravitee' ? 'Gravitee JSON' : format === 'openapi' ? 'OpenAPI JSON or YAML' : 'WSDL XML';
    const urlLabel = format === 'gravitee' ? 'Definition URL' : format === 'openapi' ? 'Specification URL' : 'WSDL URL';
    const urlPlaceholder =
        format === 'gravitee'
            ? 'https://example.com/api-definition.json'
            : format === 'openapi'
              ? 'https://example.com/openapi.yaml'
              : 'https://example.com/service.wsdl';

    const canSubmit =
        sourceMode === 'remote'
            ? isValidHttpUrl(remoteUrl)
            : format === 'gravitee'
              ? definition !== null && !parseError
              : fileText !== null && fileText.trim() !== '';

    const buildSubmission = (): ApiImportSubmission => {
        if (format === 'gravitee') {
            return sourceMode === 'remote'
                ? { format: 'gravitee', source: 'remote', url: remoteUrl.trim() }
                : { format: 'gravitee', source: 'local', definition };
        }
        const payload = sourceMode === 'remote' ? remoteUrl.trim() : (fileText as string);
        if (format === 'wsdl') {
            return {
                format: 'wsdl',
                descriptor: {
                    payload,
                    type: sourceMode === 'remote' ? 'URL' : 'INLINE',
                    withDocumentation,
                    ...(hasOasValidationPolicy ? { withOASValidationPolicy } : {}),
                    ...(withRestToSoap ? { withPolicies: [REST_TO_SOAP_POLICY_ID] } : {}),
                },
            };
        }
        return {
            format: 'openapi',
            descriptor: {
                payload,
                withDocumentation,
                ...(hasOasValidationPolicy ? { withOASValidationPolicy } : {}),
            },
        };
    };

    return {
        sourceMode,
        fileName,
        fileText,
        parseError,
        remoteUrl,
        setRemoteUrl,
        withDocumentation,
        setWithDocumentation,
        withOASValidationPolicy,
        setWithOASValidationPolicy,
        withRestToSoap,
        hasOasValidationPolicy,
        hasRestToSoapPolicy,
        handleSourceModeChange,
        handleRestToSoapChange,
        handleFile,
        canSubmit,
        buildSubmission,
        reset,
        fileAccept,
        fileHint,
        urlLabel,
        urlPlaceholder,
    };
}
