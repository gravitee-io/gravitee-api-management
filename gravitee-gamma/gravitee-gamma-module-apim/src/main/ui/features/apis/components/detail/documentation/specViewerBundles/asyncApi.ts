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
// Every rule of this stylesheet is scoped under `.aui-root`, so it styles nothing else in Gamma.
import '@asyncapi/react-component/styles/default.min.css';

// The browser build, which bundles the AsyncAPI parser but renders with Gamma's React. The package's default entry
// imports the parser's Node.js modules (fs, stream), which do not exist in a browser.
export { default } from '@asyncapi/react-component/browser/index.js';
