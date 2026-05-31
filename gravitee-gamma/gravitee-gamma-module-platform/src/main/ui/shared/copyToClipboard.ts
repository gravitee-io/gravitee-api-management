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
import { notify } from './notify';

const DEFAULT_COPY_ERROR_MESSAGE = 'Unable to copy to clipboard';

export async function copyTextToClipboard(text: string): Promise<void> {
    if (!navigator.clipboard?.writeText) {
        throw new Error('Clipboard API unavailable');
    }
    await navigator.clipboard.writeText(text);
}

/** Copies text and shows toast feedback only after a successful write. Rejects when copy fails. */
export async function copyTextToClipboardWithNotify(
    text: string,
    successMessage: string,
    errorMessage: string = DEFAULT_COPY_ERROR_MESSAGE,
): Promise<void> {
    try {
        await copyTextToClipboard(text);
        notify.info(successMessage);
    } catch (error) {
        notify.error(errorMessage);
        throw error;
    }
}

/** Fire-and-forget variant for click handlers (void return, no unhandled rejection). */
export function copyTextToClipboardWithNotifyHandler(
    text: string,
    successMessage: string,
    errorMessage: string = DEFAULT_COPY_ERROR_MESSAGE,
): void {
    void copyTextToClipboardWithNotify(text, successMessage, errorMessage);
}
