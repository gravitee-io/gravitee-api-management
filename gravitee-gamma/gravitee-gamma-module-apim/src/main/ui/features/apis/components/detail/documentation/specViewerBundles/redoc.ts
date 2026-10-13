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
// The standalone bundle, as in the portal: the `redoc` package itself needs styled-components and mobx, which the
// repository does not install. Its styles are injected by the bundle, under generated class names. It exports `init`
// by name, with no default export.
import * as Redoc from 'redoc/bundles/redoc.standalone.js';

export default Redoc;
