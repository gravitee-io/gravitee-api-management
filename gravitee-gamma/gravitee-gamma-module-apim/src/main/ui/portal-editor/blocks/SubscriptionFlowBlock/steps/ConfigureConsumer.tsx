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
import type { ConsumerFormState } from '../types';

import styles from '../SubscriptionFlowBlock.module.scss';

interface ConfigureConsumerProps {
    readonly value: ConsumerFormState;
    readonly onChange: (value: ConsumerFormState) => void;
}

function isValidUrl(value: string): boolean {
    try {
        const url = new URL(value);
        return url.protocol === 'https:' || url.protocol === 'http:';
    } catch {
        return false;
    }
}

export function ConfigureConsumer({ value, onChange }: ConfigureConsumerProps) {
    const handleCallbackChange = (callbackUrl: string) => {
        onChange({
            callbackUrl,
            isValid: isValidUrl(callbackUrl),
        });
    };

    return (
        <div className={styles.consumerForm}>
            <label className={styles.fieldLabel} htmlFor="subscription-callback-url">
                Callback URL
            </label>
            <input
                id="subscription-callback-url"
                type="url"
                className={styles.textInput}
                placeholder="https://example.com/webhooks"
                value={value.callbackUrl}
                onChange={event => handleCallbackChange(event.target.value)}
            />
            <p className={styles.fieldHint}>
                The URL where API events will be pushed. Must be a valid HTTP or HTTPS URL.
            </p>
        </div>
    );
}
