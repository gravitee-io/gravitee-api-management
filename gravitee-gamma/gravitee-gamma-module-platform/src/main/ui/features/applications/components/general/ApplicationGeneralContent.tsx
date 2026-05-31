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
import { copyTextToClipboardWithNotifyHandler } from '../../../../shared/copyToClipboard';
import { notify } from '../../../../shared/notify';
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
    const needsTypeConfig = !isSimple;
    const isArchivedOrKubernetes = isApplicationGeneralReadOnly(application);
    const isFormDisabled = !permissionsReady || isArchivedOrKubernetes || !canUpdate;
    const showSubscribeToApis = permissionsReady && !isArchivedOrKubernetes && canUpdate;
    const canManageCertificates = permissionsReady && !isArchivedOrKubernetes && canUpdate;

    const {
        data: typeConfig,
        isError: isTypeConfigError,
        error: typeConfigError,
    } = useApplicationTypeConfiguration(applicationId, needsTypeConfig);
    const { saveMutation, deleteMutation, addCertificateWithGraceMutation, updateCertificateMutation, isMutating } =
        useApplicationGeneralMutations(application, applicationId, {
            onDeleteSuccess: () => navigate('../..', { relative: 'route' }),
        });

    const [form, setForm] = useState<ApplicationGeneralForm | null>(null);
    const [savedForm, setSavedForm] = useState<ApplicationGeneralForm | null>(null);
    const [metadataFieldKey, setMetadataFieldKey] = useState(0);
    const [metadataHasDuplicateKeys, setMetadataHasDuplicateKeys] = useState(false);

    useEffect(() => {
        const seed = formFromApplication(application);
        setForm(seed);
        setSavedForm(seed);
        setMetadataHasDuplicateKeys(false);
        setMetadataFieldKey(key => key + 1);
    }, [application.id, application.updated_at]);

    const validation = useMemo(
        () =>
            form
                ? validateApplicationGeneralForm(form, {
                      isOAuthApplication: !isSimple,
                      typeConfig,
                      metadataHasDuplicateKeys,
                  })
                : {},
        [form, isSimple, typeConfig, metadataHasDuplicateKeys],
    );
    const isDirty = useMemo(() => form !== null && savedForm !== null && isApplicationGeneralFormDirty(form, savedForm), [form, savedForm]);
    const hasValidationErrors = hasApplicationGeneralValidationErrors(validation);
    const canSave = isDirty && !hasValidationErrors && !isMutating;

    const setField = useCallback(<K extends keyof ApplicationGeneralForm>(key: K, value: ApplicationGeneralForm[K]) => {
        setForm(prev => (prev ? { ...prev, [key]: value } : prev));
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
    }, []);

    const copyToClipboard = (value: string) => {
        copyTextToClipboardWithNotifyHandler(value, 'Copied to clipboard');
    };

    const handleDiscard = () => {
        if (savedForm) {
            setForm(savedForm);
            setMetadataHasDuplicateKeys(false);
            setMetadataFieldKey(key => key + 1);
        }
    };

    const handleSave = () => {
        if (!form || isFormDisabled || !canSave) return;
        const payload = buildUpdatePayload(application, form, typeConfig);
        saveMutation.mutate(payload, {
            onSuccess: () => {
                setSavedForm(form);
                notify.success('Application details successfully updated!');
            },
            onError: error => notify.error(error, 'Failed to save changes.'),
        });
    };

    const pageSkeleton = (
        <div className="space-y-5">
            <Skeleton className="h-10 w-64" />
            <Skeleton className="h-64 w-full" />
        </div>
    );

    if (!applicationId) {
        return pageSkeleton;
    }

    if (needsTypeConfig && isTypeConfigError) {
        return (
            <div className="space-y-5">
                <Card className="border-destructive/30 bg-destructive/5 p-4">
                    <p className="text-sm text-destructive">
                        {typeConfigError instanceof Error ? typeConfigError.message : 'Failed to load application type configuration.'}
                    </p>
                </Card>
            </div>
        );
    }

    // Console waits for application + type config before rendering the form.
    if (!form || (needsTypeConfig && !typeConfig)) {
        return pageSkeleton;
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
                {isDirty && !isFormDisabled ? (
                    <div className="flex shrink-0 items-center gap-2">
                        <Button type="button" variant="outline" size="sm" onClick={handleDiscard} disabled={saveMutation.isPending}>
                            Discard
                        </Button>
                        <Button type="button" size="sm" onClick={handleSave} disabled={!canSave}>
                            <CheckIcon className="size-4" aria-hidden />
                            {saveMutation.isPending ? 'Saving…' : 'Save changes'}
                        </Button>
                    </div>
                ) : null}
            </div>

            <ApplicationDetailsSection
                application={application}
                form={form}
                validation={validation}
                isFormDisabled={isFormDisabled || isMutating}
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
                validation={validation}
                metadataFieldKey={metadataFieldKey}
                isFormDisabled={isFormDisabled || isMutating}
                onFieldChange={setField}
                onMetadataDuplicateKeysChange={setMetadataHasDuplicateKeys}
                onCopy={copyToClipboard}
            />

            <ApplicationCertificatesSection
                applicationId={applicationId}
                canManageCertificates={canManageCertificates}
                isMutating={isMutating}
                addCertificateWithGraceMutation={addCertificateWithGraceMutation}
                updateCertificateMutation={updateCertificateMutation}
            />

            <ApplicationLifecycleSection
                application={application}
                canDelete={permissionsReady && canDelete && !isArchivedOrKubernetes}
                isMutating={isMutating}
                deleteMutation={deleteMutation}
            />
        </div>
    );
}
