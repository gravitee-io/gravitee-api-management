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
import { toast } from '@gravitee/graphene-core';

import { extractErrorMessage } from './extractErrorMessage';

const SUCCESS_DURATION_MS = 3000;

/**
 * Transient feedback aligned with console Applications snackbar behavior.
 * API failures from confirm/action dialogs are toast-only (no inline dialog error prop).
 */
export const notify = {
    success(message: string) {
        toast.success(message, { duration: SUCCESS_DURATION_MS });
    },

    error(error: unknown, fallback?: string) {
        toast.error(extractErrorMessage(error, fallback ?? 'Something went wrong.'), { duration: Infinity });
    },

    warning(message: string) {
        toast.warning(message);
    },

    info(message: string) {
        toast.info(message, { duration: SUCCESS_DURATION_MS });
    },
};
