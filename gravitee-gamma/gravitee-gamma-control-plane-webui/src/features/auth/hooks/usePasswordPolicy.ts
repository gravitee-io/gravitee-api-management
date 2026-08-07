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
import { useEffect, useState } from 'react';

import type { PasswordPolicy } from '../../../shared/password-policy';
import { getPasswordPolicy } from '../services/passwordPolicy.service';

const EMPTY_POLICY: PasswordPolicy = { rules: [] };

export function usePasswordPolicy() {
    const [policy, setPolicy] = useState<PasswordPolicy>(EMPTY_POLICY);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;

        getPasswordPolicy()
            .then(fetchedPolicy => {
                if (!cancelled) {
                    const rules = fetchedPolicy.rules ?? [];
                    if (rules.length === 0) {
                        setPolicy(EMPTY_POLICY);
                        setError('Unable to load password requirements. Please refresh the page and try again.');
                        return;
                    }

                    setPolicy({
                        description: fetchedPolicy.description,
                        pattern: fetchedPolicy.pattern,
                        rules,
                    });
                    setError(null);
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setPolicy(EMPTY_POLICY);
                    setError('Unable to load password requirements. Please refresh the page and try again.');
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setLoading(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, []);

    return { policy, loading, error };
}
