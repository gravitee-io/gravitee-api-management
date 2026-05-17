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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Button, Card, Skeleton } from '@gravitee/graphene-core';
import { CheckIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { ApplicationCertificatesSection } from './ApplicationCertificatesSection';
import { ApplicationDetailsSection } from './ApplicationDetailsSection';
import { ApplicationLifecycleSection } from './ApplicationLifecycleSection';
import { ApplicationOAuthSection } from './ApplicationOAuthSection';
import { useDetailBasePath } from '../../../shared/hooks/useDetailBasePath';
import { useApplicationDetailContext } from '../../context/ApplicationDetailContext';
import { useApplicationGeneralMutations } from '../../hooks/useApplicationGeneralMutations';
import { useApplicationTypeConfiguration } from '../../hooks/useApplicationTypeConfiguration';
import type { ApplicationListItem } from '../../types/application';
import {
    buildUpdatePayload,
    formFromApplication,
    hasApplicationGeneralValidationErrors,
    isApplicationGeneralFormDirty,
    isApplicationGeneralReadOnly,
    validateApplicationGeneralForm,
    type ApplicationGeneralForm,
} from '../../utils/applicationGeneralMapper';

export function ApplicationGeneralContent({ application }: Readonly<{ application: ApplicationListItem }>) {
    const { applicationId } = useParams<{ applicationId: string }>();
    const navigate = useNavigate();
    const basePath = useDetailBasePath('applications', applicationId);
    const { permissionsReady } = useApplicationDetailContext();

    const canUpdate = useHasPermission({ anyOf: ['application-definition-u'] });
    const canDelete = useHasPermission({ anyOf: ['application-definition-d'] });

    const isSimple = application.type === 'SIMPLE';
    const isArchivedOrKubernetes = isApplicationGeneralReadOnly(application);
    const isFormDisabled = !permissionsReady || isArchivedOrKubernetes || !canUpdate;
    const showSubscribeToApis = permissionsReady && !isArchivedOrKubernetes && canUpdate;
    const canManageCertificates = permissionsReady && !isArchivedOrKubernetes && canUpdate;

    const { data: typeConfig } = useApplicationTypeConfiguration(applicationId, !isSimple);
    const { saveMutation } = useApplicationGeneralMutations(application, applicationId);

    const [form, setForm] = useState<ApplicationGeneralForm | null>(null);
    const [savedForm, setSavedForm] = useState<ApplicationGeneralForm | null>(null);
    const [saveError, setSaveError] = useState<string | null>(null);

    useEffect(() => {
        const seed = formFromApplication(application);
        setForm(seed);
        setSavedForm(seed);
        setSaveError(null);
    }, [application]);

    const validation = useMemo(() => (form ? validateApplicationGeneralForm(form) : {}), [form]);
    const isDirty = useMemo(() => form !== null && savedForm !== null && isApplicationGeneralFormDirty(form, savedForm), [form, savedForm]);
    const hasValidationErrors = hasApplicationGeneralValidationErrors(validation);
    const canSave = isDirty && !hasValidationErrors && !saveMutation.isPending;

    const setField = useCallback(<K extends keyof ApplicationGeneralForm>(key: K, value: ApplicationGeneralForm[K]) => {
        setForm(prev => (prev ? { ...prev, [key]: value } : prev));
        setSaveError(null);
    }, []);

    const handlePictureChange = useCallback((value: string | null) => {
        setForm(prev =>
            prev
                ? {
                      ...prev,
                      picture: value,
                      pictureRemoved: value === null,
                  }
                : prev,
        );
        setSaveError(null);
    }, []);

    const handleBackgroundChange = useCallback((value: string | null) => {
        setForm(prev =>
            prev
                ? {
                      ...prev,
                      background: value,
                      backgroundRemoved: value === null,
                  }
                : prev,
        );
        setSaveError(null);
    }, []);

    const copyToClipboard = (value: string) => {
        void navigator.clipboard.writeText(value);
    };

    const handleSave = () => {
        if (!form || isFormDisabled || !canSave) return;
        const payload = buildUpdatePayload(application, form, typeConfig);
        saveMutation.mutate(payload, {
            onSuccess: () => {
                setSavedForm(form);
                setSaveError(null);
            },
            onError: (e: unknown) => setSaveError(e instanceof Error ? e.message : 'Failed to save changes.'),
        });
    };

    if (!form || !applicationId) {
        return (
            <div className="space-y-5">
                <Skeleton className="h-10 w-64" />
                <Skeleton className="h-64 w-full" />
            </div>
        );
    }

    return (
        <div className="space-y-5">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">General</h1>
                    <p className="text-sm text-muted-foreground">
                        Application details, OAuth client configuration, certificates, and lifecycle.
                    </p>
                </div>
                {!isFormDisabled ? (
                    <Button type="button" size="sm" className="shrink-0" onClick={handleSave} disabled={!canSave}>
                        <CheckIcon className="size-4" aria-hidden />
                        {saveMutation.isPending ? 'Saving…' : 'Save changes'}
                    </Button>
                ) : null}
            </div>

            {saveError ? (
                <Card className="border-destructive/30 bg-destructive/5 p-4">
                    <p className="text-sm text-destructive">{saveError}</p>
                </Card>
            ) : null}

            <ApplicationDetailsSection
                application={application}
                form={form}
                validation={validation}
                isFormDisabled={isFormDisabled}
                showSubscribeToApis={showSubscribeToApis}
                subscriptionsPath={`${basePath}/subscriptions`}
                onFieldChange={setField}
                onPictureChange={handlePictureChange}
                onBackgroundChange={handleBackgroundChange}
            />

            <ApplicationOAuthSection
                isSimple={isSimple}
                form={form}
                typeConfig={typeConfig}
                isFormDisabled={isFormDisabled}
                onFieldChange={setField}
                onCopy={copyToClipboard}
            />

            <ApplicationCertificatesSection
                application={application}
                applicationId={applicationId}
                canManageCertificates={canManageCertificates}
            />

            <ApplicationLifecycleSection
                application={application}
                applicationId={applicationId}
                canDelete={permissionsReady && canDelete && !isArchivedOrKubernetes}
                onDeleteSuccess={() => navigate('../..', { relative: 'route' })}
            />
        </div>
    );
}
