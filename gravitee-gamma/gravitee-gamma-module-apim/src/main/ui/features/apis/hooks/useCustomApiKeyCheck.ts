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
import { useQuery } from '@tanstack/react-query';

import { verifyCustomApiKey } from '../services/subscriptions';
import type { SubscriptionContext } from '../types/subscription';
import { customApiKeyFormatError } from '../utils/customApiKey';

export interface CustomApiKeyCheck {
    error: string | null;
    checking: boolean;
    /** True when a non-empty key is missing, unverified, or already in use. */
    blocked: boolean;
}

export function useCustomApiKeyCheck(ctx: SubscriptionContext, applicationId: string | undefined, value: string): CustomApiKeyCheck {
    const env = useEnvironment();
    const formatError = customApiKeyFormatError(value);
    const canVerify = Boolean(env?.id && applicationId && value !== '' && !formatError);
    const verify = useQuery({
        queryKey: ['verify-custom-api-key', env?.id ?? '', ctx.type, ctx.entityId, applicationId ?? '', value],
        queryFn: () => verifyCustomApiKey(env!.id, ctx, { applicationId: applicationId!, apiKey: value }),
        enabled: canVerify,
        retry: false,
    });

    const checking = canVerify && (verify.isPending || verify.isFetching);
    const unavailable = canVerify && verify.isError;
    const notUnique = verify.data?.ok === false;
    const error =
        formatError ??
        (notUnique ? 'Key already exists with given value' : null) ??
        (unavailable ? 'Could not verify this API key.' : null);
    const blocked = value !== '' && (formatError !== null || checking || unavailable || notUnique);

    return { error, checking, blocked };
}
