import { Button, cn } from '@gravitee/graphene-core';
import { ArrowLeft, ArrowRight, BookOpen, ExternalLink } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import { resolveModulePath } from '../../config/routes';
import type { ApiProxyWizardModel, SecurityModel } from './apiProxyWizardModels';
import { buildCreateAndDeployCommand, slugify } from './apiProxyWizardModels';
import { ApiProxyWizardStepper, SCRATCH_STEPS } from './ApiProxyWizardStepper';
import { ApiDetailsStep } from './steps/ApiDetailsStep';
import { ConfigureProxyStep } from './steps/ConfigureProxyStep';
import { ReviewDeployStep } from './steps/ReviewDeployStep';
import { SecureStep } from './steps/SecureStep';

const DOCS_BASE = 'https://documentation.gravitee.io/apim';

type Props = {
    readonly onExitToPicker: () => void;
};

export function ApiProxyWizardScratch({ onExitToPicker }: Props) {
    const [step, setStep] = useState(0);
    const location = useLocation();
    const navigate = useNavigate();

    const { modulePrefix } = useMemo(() => resolveModulePath(location.pathname), [location.pathname]);
    const exitToApis = useMemo(() => (modulePrefix ? `/${modulePrefix}/apis` : '/apis'), [modulePrefix]);

    const [model, setModel] = useState<ApiProxyWizardModel>({
        details: { name: '', version: '1.0.0', description: '' },
        proxy: {
            entrypoints: { type: 'context_path', contextPath: '' },
            upstreamUrl: '',
        },
        security: { type: 'keyless' },
        deployment: { deployImmediately: true },
    });

    const stepValid = useMemo(() => validateScratch(model), [model]);
    const isFinalStep = step === SCRATCH_STEPS.length - 1;
    const canGoNext = stepValid[step] ?? true;

    const next = () => {
        if (!canGoNext) return;
        if (step === 0) {
            const slug = slugify(model.details.name);
            if (model.proxy.entrypoints.type === 'context_path' && !model.proxy.entrypoints.contextPath.trim()) {
                setModel(m => ({ ...m, proxy: { ...m.proxy, entrypoints: { type: 'context_path', contextPath: slug ? `/${slug}` : '' } } }));
            }
        }
        setStep(s => Math.min(s + 1, SCRATCH_STEPS.length - 1));
    };

    const previous = () => {
        if (step === 0) {
            onExitToPicker();
            return;
        }
        setStep(s => Math.max(s - 1, 0));
    };

    const deploy = () => {
        const command = buildCreateAndDeployCommand(model);
        // TODO (next iteration): call APIM REST API / Gamma service to create & deploy.
        // eslint-disable-next-line no-console
        console.log('Create & deploy command', command);
        navigate(exitToApis);
    };

    return (
        <div className={cn('flex flex-col gap-6 p-6')}>
            <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <div className="space-y-2">
                    <h1 className="font-semibold text-xl">Create API Proxy</h1>
                    <p className="text-muted-foreground">Set up a secure, managed proxy in four simple steps.</p>
                </div>
                <a
                    href={DOCS_BASE}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1.5 self-start text-xs text-primary hover:text-primary/80 hover:underline underline-offset-2"
                >
                    <BookOpen className="size-3.5" aria-hidden />
                    Gravitee API Management documentation
                    <ExternalLink className="size-3" aria-hidden />
                </a>
            </header>

            <ApiProxyWizardStepper currentStepIndex={step} onSelectStep={idx => idx <= step && setStep(idx)} />

            {step === 0 ? (
                <ApiDetailsStep
                    value={model.details}
                    onChange={patch => setModel(m => ({ ...m, details: { ...m.details, ...patch } }))}
                />
            ) : null}

            {step === 1 ? (
                <ConfigureProxyStep
                    value={model.proxy}
                    onChange={patch => setModel(m => ({ ...m, proxy: { ...m.proxy, ...patch } }))}
                />
            ) : null}

            {step === 2 ? (
                <SecureStep value={model.security} onChange={(security: SecurityModel) => setModel(m => ({ ...m, security }))} />
            ) : null}

            {step === 3 ? (
                <ReviewDeployStep
                    model={model}
                    onEditStep={idx => setStep(idx)}
                    onToggleDeployImmediately={deployImmediately =>
                        setModel(m => ({ ...m, deployment: { ...m.deployment, deployImmediately } }))
                    }
                    onDeploy={deploy}
                />
            ) : null}

            <div className="h-px bg-border" />

            <div className="flex items-center justify-between">
                <Button type="button" variant="outline" onClick={previous}>
                    <ArrowLeft className="mr-2 size-4" aria-hidden />
                    {step === 0 ? 'Back to templates' : 'Previous'}
                </Button>

                {!isFinalStep ? (
                    <div className="flex items-center gap-3">
                        <span className="text-xs text-muted-foreground">
                            Step {step + 1} of {SCRATCH_STEPS.length}
                        </span>
                        <Button type="button" onClick={next} disabled={!canGoNext}>
                            {step === 0 ? 'Validate my API details' : step === 1 ? 'Validate my entrypoints' : 'Next'}
                            <ArrowRight className="ml-2 size-4" aria-hidden />
                        </Button>
                    </div>
                ) : null}
            </div>
        </div>
    );
}

function validateScratch(model: ApiProxyWizardModel): readonly boolean[] {
    const step1Valid =
        model.details.name.trim().length > 0 && model.details.version.trim().length > 0 && model.details.description.trim().length > 0;

    const entrypointsValid =
        model.proxy.entrypoints.type === 'virtual_hosts'
            ? model.proxy.entrypoints.virtualHosts.every(v => v.host.trim().length > 0 && v.path.trim().startsWith('/'))
            : model.proxy.entrypoints.contextPath.trim().length > 0 && model.proxy.entrypoints.contextPath.trim().startsWith('/');

    const step2Valid = model.proxy.upstreamUrl.trim().length > 0 && entrypointsValid;

    const step3Valid = validateSecurity(model.security);

    return [step1Valid, step2Valid, step3Valid, true] as const;
}

function validateSecurity(security: SecurityModel): boolean {
    switch (security.type) {
        case 'keyless':
            return true;
        case 'api_key':
            return security.planName.trim().length > 0;
        case 'jwt':
            return (
                security.planName.trim().length > 0 &&
                security.signature.trim().length > 0 &&
                security.jwksResolver.trim().length > 0 &&
                security.resolverParameter.trim().length > 0
            );
        case 'oauth2':
            return security.planName.trim().length > 0 && security.resource.trim().length > 0;
        case 'mtls':
            return security.planName.trim().length > 0;
    }
}

