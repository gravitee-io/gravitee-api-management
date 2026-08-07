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
import { managementApi } from '../../../shared/api/api-client';

const RECAPTCHA_HEADER = 'X-Recaptcha-Token';
const RECAPTCHA_SCRIPT_ID = 'gamma-recaptcha';

interface ConsoleReCaptchaConfig {
    enabled?: boolean;
    siteKey?: string;
}

interface GRecaptcha {
    ready: (callback: () => void) => void;
    execute: (siteKey: string, options: { action: string }) => Promise<string>;
}

declare global {
    interface Window {
        grecaptcha?: GRecaptcha;
    }
}

let cachedConfig: ConsoleReCaptchaConfig | null | undefined;
let scriptLoadPromise: Promise<void> | null = null;

async function fetchReCaptchaConfig(): Promise<ConsoleReCaptchaConfig | null> {
    if (cachedConfig !== undefined) {
        return cachedConfig;
    }

    try {
        const consoleConfig = await managementApi.get<{ reCaptcha?: ConsoleReCaptchaConfig }>('/console');
        const reCaptcha = consoleConfig.reCaptcha;
        cachedConfig = reCaptcha?.enabled && reCaptcha.siteKey ? reCaptcha : null;
    } catch {
        cachedConfig = null;
    }

    return cachedConfig;
}

function loadReCaptchaScript(siteKey: string): Promise<void> {
    if (scriptLoadPromise) {
        return scriptLoadPromise;
    }

    scriptLoadPromise = new Promise((resolve, reject) => {
        if (document.getElementById(RECAPTCHA_SCRIPT_ID)) {
            resolve();
            return;
        }

        const script = document.createElement('script');
        script.id = RECAPTCHA_SCRIPT_ID;
        script.src = `https://www.google.com/recaptcha/api.js?render=${encodeURIComponent(siteKey)}`;
        script.async = true;
        script.onload = () => {
            window.grecaptcha?.ready(() => resolve());
        };
        script.onerror = () => {
            scriptLoadPromise = null;
            reject(new Error('Failed to load reCAPTCHA'));
        };
        document.head.appendChild(script);
    });

    return scriptLoadPromise;
}

export async function resolveReCaptchaToken(action: string): Promise<string | null> {
    const config = await fetchReCaptchaConfig();
    if (!config?.enabled || !config.siteKey) {
        return null;
    }

    await loadReCaptchaScript(config.siteKey);
    if (!window.grecaptcha) {
        throw new Error('reCAPTCHA is not available');
    }

    return window.grecaptcha.execute(config.siteKey, { action });
}

export function getReCaptchaHeaderName(): string {
    return RECAPTCHA_HEADER;
}

export function resetReCaptchaConfigCacheForTests(): void {
    cachedConfig = undefined;
    scriptLoadPromise = null;
}
