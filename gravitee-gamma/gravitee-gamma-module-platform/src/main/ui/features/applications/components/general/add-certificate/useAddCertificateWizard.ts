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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { INVALID_CERTIFICATE_MESSAGE, type AddCertificateSubmit } from './addCertificateTypes';
import { validateApplicationCertificate } from '../../../services/applicationDetail';
import type { ClientCertificate } from '../../../types/applicationCertificate';
import { findActiveCertificate, isDateInRange, readFileAsText } from '../certificateUtils';

function startOfToday(): Date {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return today;
}

function parseGracePeriodDefault(activeCert: ClientCertificate | undefined): Date | undefined {
    if (!activeCert?.certificateExpiration) return undefined;
    const parsed = new Date(activeCert.certificateExpiration);
    return Number.isNaN(parsed.getTime()) ? undefined : parsed;
}

export function useAddCertificateWizard({
    open,
    applicationId,
    certificates,
    onSubmit,
}: Readonly<{
    open: boolean;
    applicationId: string;
    certificates: ClientCertificate[];
    onSubmit: (payload: AddCertificateSubmit) => void;
}>) {
    const env = useEnvironment();
    const [stepIndex, setStepIndex] = useState(0);
    const [name, setName] = useState('');
    const [certificate, setCertificate] = useState('');
    const [endsAt, setEndsAt] = useState<Date | undefined>();
    const [gracePeriodEnd, setGracePeriodEnd] = useState<Date | undefined>();
    const [fileError, setFileError] = useState<string | null>(null);
    const [validationError, setValidationError] = useState<string | null>(null);
    const [uploadTouched, setUploadTouched] = useState(false);
    const [isValidating, setIsValidating] = useState(false);
    const [isUploadValidated, setIsUploadValidated] = useState(false);
    const [validatedSnapshot, setValidatedSnapshot] = useState<{ name: string; certificate: string } | null>(null);
    const [maxEndsAt, setMaxEndsAt] = useState<Date | undefined>();

    const minDateRef = useRef(startOfToday());
    const activeCert = findActiveCertificate(certificates);
    const hasActive = Boolean(activeCert);
    const maxGracePeriod = parseGracePeriodDefault(activeCert);

    const resetForm = useCallback(() => {
        setStepIndex(0);
        setName('');
        setCertificate('');
        setEndsAt(undefined);
        setGracePeriodEnd(undefined);
        setFileError(null);
        setValidationError(null);
        setUploadTouched(false);
        setIsUploadValidated(false);
        setValidatedSnapshot(null);
        setMaxEndsAt(undefined);
        minDateRef.current = startOfToday();
    }, []);

    const wasOpenRef = useRef(false);

    useEffect(() => {
        if (!open) {
            if (wasOpenRef.current) {
                resetForm();
            }
            wasOpenRef.current = false;
            return;
        }

        if (!wasOpenRef.current) {
            const activeOnOpen = findActiveCertificate(certificates);
            minDateRef.current = startOfToday();
            setStepIndex(0);
            setName('');
            setCertificate('');
            setEndsAt(undefined);
            setGracePeriodEnd(parseGracePeriodDefault(activeOnOpen));
            setFileError(null);
            setValidationError(null);
            setUploadTouched(false);
            setIsUploadValidated(false);
            setValidatedSnapshot(null);
            setMaxEndsAt(undefined);
        }
        wasOpenRef.current = true;
    }, [open, certificates, resetForm]);

    const invalidateUploadValidation = useCallback(() => {
        setIsUploadValidated(false);
        setValidatedSnapshot(null);
        setMaxEndsAt(undefined);
        setEndsAt(undefined);
        setValidationError(null);
        setStepIndex(previous => (previous > 0 ? 0 : previous));
    }, []);

    const handleNameChange = useCallback(
        (value: string) => {
            setName(value);
            if (validatedSnapshot && value.trim() !== validatedSnapshot.name) {
                invalidateUploadValidation();
            }
        },
        [invalidateUploadValidation, validatedSnapshot],
    );

    const handleCertificateChange = useCallback(
        (value: string) => {
            setCertificate(value);
            setValidationError(null);
            if (validatedSnapshot && value.trim() !== validatedSnapshot.certificate) {
                invalidateUploadValidation();
            }
        },
        [invalidateUploadValidation, validatedSnapshot],
    );

    const loadCertificateContent = useCallback(
        async (file: File) => {
            setFileError(null);
            try {
                const content = await readFileAsText(file);
                handleCertificateChange(content);
            } catch {
                setFileError('Failed to read certificate file.');
            }
        },
        [handleCertificateChange],
    );

    const handleValidateAndContinue = useCallback(async () => {
        setUploadTouched(true);
        if (!env || !name.trim() || !certificate.trim()) return;

        setValidationError(null);
        setIsValidating(true);
        try {
            const response = await validateApplicationCertificate(env.id, applicationId, certificate.trim());
            const expiration = new Date(response.certificateExpiration);
            if (Number.isNaN(expiration.getTime())) {
                setValidationError(INVALID_CERTIFICATE_MESSAGE);
                return;
            }

            const trimmedName = name.trim();
            const trimmedCertificate = certificate.trim();
            setMaxEndsAt(expiration);
            setEndsAt(expiration);
            setIsUploadValidated(true);
            setValidatedSnapshot({ name: trimmedName, certificate: trimmedCertificate });
            setStepIndex(1);
        } catch {
            setValidationError(INVALID_CERTIFICATE_MESSAGE);
        } finally {
            setIsValidating(false);
        }
    }, [applicationId, certificate, env, name]);

    const configureErrors = useMemo(() => {
        const errors: { endsAt?: string; gracePeriodEnd?: string } = {};
        const minDate = minDateRef.current;

        if (endsAt && !isDateInRange(endsAt, minDate, maxEndsAt)) {
            errors.endsAt = maxEndsAt
                ? 'Expiration date must be between now and the certificate expiration.'
                : 'Expiration date must not be in the past.';
        }

        if (hasActive) {
            if (!gracePeriodEnd) {
                errors.gracePeriodEnd = 'Grace period end date is required when rotating certificates.';
            } else if (!isDateInRange(gracePeriodEnd, minDate, maxGracePeriod)) {
                errors.gracePeriodEnd = maxGracePeriod
                    ? "Grace period end date must not be after the active certificate's expiration."
                    : 'Grace period end date must not be in the past.';
            }
        }

        return errors;
    }, [endsAt, gracePeriodEnd, hasActive, maxEndsAt, maxGracePeriod]);

    const canContinueConfigure = isUploadValidated && Object.keys(configureErrors).length === 0;

    const handleSubmit = useCallback(() => {
        if (!isUploadValidated || !name.trim() || !certificate.trim()) return;
        onSubmit({
            name: name.trim(),
            certificate: certificate.trim(),
            endsAt: endsAt?.toISOString(),
            ...(hasActive && gracePeriodEnd && activeCert
                ? { gracePeriodEnd: gracePeriodEnd.toISOString(), activeCertificateId: activeCert.id }
                : {}),
        });
    }, [activeCert, certificate, endsAt, gracePeriodEnd, hasActive, isUploadValidated, name, onSubmit]);

    const goToPreviousStep = useCallback(() => {
        setStepIndex(previous => Math.max(previous - 1, 0));
    }, []);

    const goToConfirmStep = useCallback(() => {
        if (!canContinueConfigure) return;
        setStepIndex(2);
    }, [canContinueConfigure]);

    return {
        stepIndex,
        name,
        certificate,
        endsAt,
        gracePeriodEnd,
        fileError,
        validationError,
        uploadTouched,
        isValidating,
        isUploadValidated,
        hasActive,
        maxEndsAt,
        maxGracePeriod,
        minDate: minDateRef.current,
        configureErrors,
        canContinueConfigure,
        handleNameChange,
        handleCertificateChange,
        loadCertificateContent,
        handleValidateAndContinue,
        setEndsAt,
        setGracePeriodEnd,
        handleSubmit,
        goToPreviousStep,
        goToConfirmStep,
    };
}
