/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
/**
 * @asciidoctor/core 4, which @gravitee/ui-particles-angular loads to render policy documentation,
 * carries two things a browser bundle cannot take at face value:
 *
 *  - `new URL('../../data', import.meta.url)`, which webpack treats as an asset reference and then
 *    fails to resolve, even though the code only reaches it under Node;
 *  - `import('node:fs' | 'node:fs/promises' | 'node:async_hooks' | 'node:path')`, sitting behind
 *    runtime guards that no bundler follows.
 *
 * The esbuild builder has `externalDependencies` for this; the webpack one has no equivalent, hence
 * this config. Both settings are scoped to what asciidoctor needs and change nothing else.
 */
module.exports = config => {
  config.module = config.module ?? {};
  config.module.parser = {
    ...config.module.parser,
    javascript: { ...config.module.parser?.javascript, url: false },
  };

  // Declared as externals rather than through IgnorePlugin: @angular-builders/custom-webpack
  // carries its own copy of webpack, and a plugin instantiated from the root one is applied to a
  // compiler built by the other, whose hooks it does not recognise.
  const existing = Array.isArray(config.externals) ? config.externals : config.externals ? [config.externals] : [];
  config.externals = [
    ...existing,
    ({ request }, callback) => (/^node:/.test(request) ? callback(null, `commonjs ${request}`) : callback()),
  ];

  return config;
};
