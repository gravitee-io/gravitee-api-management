import { render, type RenderOptions, type RenderResult } from '@testing-library/react';
import type { ReactElement } from 'react';
import { EnvironmentProvider } from '../../main/ui/app/lib/env/EnvironmentContext';

interface RenderWithProvidersOptions extends Omit<RenderOptions, 'wrapper'> {
    /**
     * Test-side override for the environment id surfaced via {@link EnvironmentProvider}.
     * Defaults to {@code 'DEFAULT'} so existing test expectations (`expect(...).toHaveBeenCalledWith('DEFAULT')`)
     * keep working without per-test boilerplate.
     */
    readonly environmentId?: string;
}

/**
 * Renders a React element wrapped in the providers the production federation entry
 * provides. Story G1 — every component that calls {@link useEnvironment} requires a
 * surrounding {@link EnvironmentProvider}; using this helper keeps tests free of the
 * wrapper boilerplate while pinning the env id explicitly per test where needed.
 */
export function renderWithProviders(
    ui: ReactElement,
    { environmentId = 'DEFAULT', ...options }: RenderWithProvidersOptions = {},
): RenderResult {
    return render(<EnvironmentProvider environmentId={environmentId}>{ui}</EnvironmentProvider>, options);
}
