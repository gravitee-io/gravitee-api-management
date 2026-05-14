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

/** Inline success feedback (same pattern as ApiEntrypointsPage save confirmation in gamma-module-apim). */
export function SuccessBanner({ message }: { readonly message: string }) {
    return (
        <div className="rounded-lg border border-success/30 bg-success/5 px-4 py-3" role="status">
            <p className="text-sm text-success">{message}</p>
        </div>
    );
}
