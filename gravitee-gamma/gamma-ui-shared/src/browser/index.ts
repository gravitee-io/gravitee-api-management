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

/** Triggers a browser download of `blob` as `fileName`. */
export function downloadBlob(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.download = fileName;
    anchor.href = url;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    // Some browsers (older Safari/Edge) start the download asynchronously; revoking in this
    // turn can cancel it. Yield so the click is processed first.
    setTimeout(() => URL.revokeObjectURL(url), 0);
}

/** Builds a `Blob` from text content and downloads it as `fileName`. */
export function downloadTextFile(content: string, fileName: string, mimeType: string): void {
    downloadBlob(new Blob([content], { type: mimeType }), fileName);
}
