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
import { AppStore, loadAndBundleSpec, RedocNormalizedOptions, type RedocRawOptions, SpecStore } from 'redoc';

// Redoc's library, rendered with Gamma's React. The standalone bundle the portal uses builds its stores itself and
// never disposes of them, so each document it renders leaves a window scroll listener behind.
export { Redoc } from 'redoc';

/**
 * Builds the store Redoc renders a document from, as Redoc does for the portal: a Swagger 2.0 document is converted
 * and references are bundled first. The store holds a window listener until it is disposed.
 */
export async function createRedocStore(spec: object, options: RedocRawOptions): Promise<AppStore> {
    const document = await loadAndBundleSpec(spec);
    // A store listens to the window before it reads the document, and one that fails to read it cannot be disposed:
    // reading the document on its own first keeps such a failure from leaving a listener behind.
    new SpecStore(document, undefined, new RedocNormalizedOptions(options));
    return new AppStore(document, undefined, options);
}
