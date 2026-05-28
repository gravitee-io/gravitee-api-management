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
import { Button, Dialog, DialogClose, DialogContent, DialogHeader, DialogTitle } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';

import { AddCertificateConfigureStep } from './AddCertificateConfigureStep';
import { AddCertificateConfirmStep } from './AddCertificateConfirmStep';
import { AddCertificateDialogFooter } from './AddCertificateDialogFooter';
import { AddCertificateStepIndicator } from './AddCertificateStepIndicator';
import { ADD_CERTIFICATE_DIALOG_WIDTH, type AddCertificateSubmit } from './addCertificateTypes';
import { AddCertificateUploadStep } from './AddCertificateUploadStep';
import { useAddCertificateWizard } from './useAddCertificateWizard';
import type { ClientCertificate } from '../../../types/applicationCertificate';

export type { AddCertificateSubmit } from './addCertificateTypes';

export function ApplicationAddCertificateDialog({
    open,
    onOpenChange,
    applicationId,
    certificates,
    onSubmit,
    isSubmitting,
    error,
}: Readonly<{
    open: boolean;
    onOpenChange: (open: boolean) => void;
    applicationId: string;
    certificates: ClientCertificate[];
    onSubmit: (payload: AddCertificateSubmit) => void;
    isSubmitting: boolean;
    error?: string | null;
}>) {
    const wizard = useAddCertificateWizard({ open, applicationId, certificates, onSubmit });

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent
                className="flex max-h-[min(90vh,48rem)] flex-col gap-0 overflow-hidden p-0"
                style={{
                    width: `min(calc(100vw - 2rem), ${ADD_CERTIFICATE_DIALOG_WIDTH})`,
                    maxWidth: `min(calc(100vw - 2rem), ${ADD_CERTIFICATE_DIALOG_WIDTH})`,
                }}
                showCloseButton={false}
            >
                <DialogHeader className="flex-row items-start justify-between gap-3 space-y-0 border-b border-border px-6 py-4">
                    <DialogTitle className="text-left">Add certificate</DialogTitle>
                    <DialogClose asChild>
                        <Button type="button" variant="ghost" size="icon" className="size-8 shrink-0" aria-label="Close">
                            <XIcon className="size-4" aria-hidden />
                        </Button>
                    </DialogClose>
                </DialogHeader>

                <div className="min-h-0 flex-1 overflow-y-auto px-6 py-4">
                    <AddCertificateStepIndicator stepIndex={wizard.stepIndex} />
                    <div className="mt-6">
                        {wizard.stepIndex === 0 ? (
                            <AddCertificateUploadStep
                                name={wizard.name}
                                certificate={wizard.certificate}
                                fileError={wizard.fileError}
                                validationError={wizard.validationError}
                                uploadTouched={wizard.uploadTouched}
                                isValidating={wizard.isValidating}
                                onNameChange={wizard.handleNameChange}
                                onCertificateChange={wizard.handleCertificateChange}
                                onFileSelected={file => void wizard.loadCertificateContent(file)}
                            />
                        ) : null}
                        {wizard.stepIndex === 1 ? (
                            <AddCertificateConfigureStep
                                endsAt={wizard.endsAt}
                                gracePeriodEnd={wizard.gracePeriodEnd}
                                hasActive={wizard.hasActive}
                                configureErrors={wizard.configureErrors}
                                onEndsAtChange={wizard.setEndsAt}
                                onGracePeriodEndChange={wizard.setGracePeriodEnd}
                            />
                        ) : null}
                        {wizard.stepIndex === 2 ? (
                            <AddCertificateConfirmStep
                                name={wizard.name}
                                endsAt={wizard.endsAt}
                                gracePeriodEnd={wizard.gracePeriodEnd}
                                hasActive={wizard.hasActive}
                            />
                        ) : null}
                    </div>
                    {error ? <p className="mt-4 text-sm text-destructive">{error}</p> : null}
                </div>

                <AddCertificateDialogFooter
                    stepIndex={wizard.stepIndex}
                    isValidating={wizard.isValidating}
                    isSubmitting={isSubmitting}
                    canContinueConfigure={wizard.canContinueConfigure}
                    onPrevious={wizard.goToPreviousStep}
                    onValidateAndContinue={() => void wizard.handleValidateAndContinue()}
                    onContinueToConfirm={wizard.goToConfirmStep}
                    onSubmit={wizard.handleSubmit}
                />
            </DialogContent>
        </Dialog>
    );
}
