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
// Every rule of this stylesheet is scoped under `.swagger-ui`, so it styles nothing else in Gamma.
import 'swagger-ui/dist/swagger-ui.css';

// rspack.config.ts resolves `swagger-ui` to the bundle carrying its own React: the default entry needs React 18.
export { default } from 'swagger-ui';
