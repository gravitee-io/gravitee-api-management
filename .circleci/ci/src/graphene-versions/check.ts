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
import * as path from 'path';
import { readApimPins } from './apim-pins';
import { readBundledModules } from './bundled-modules';
import { formatReport, hasErrors } from './report';
import { checkGrapheneVersions } from './rules';

/** Where 'Build backend' leaves the assembled distribution, and persists it to the workspace. */
const DEFAULT_DISTRIBUTION =
  'gravitee-apim-distribution/gravitee-apim-distribution-standalone/gravitee-apim-distribution-standalone-rest-api/target/distribution';

function optionOr(name: string, fallback: string): string {
  const index = process.argv.indexOf(`--${name}`);
  if (index === -1) {
    return fallback;
  }
  const value = process.argv[index + 1];
  if (value === undefined) {
    throw new Error(`--${name} needs a value.`);
  }
  return value;
}

function main(): number {
  const repoRoot = path.resolve(optionOr('repo-root', path.join(__dirname, '..', '..', '..', '..')));
  const distributionDir = path.resolve(repoRoot, optionOr('distribution', DEFAULT_DISTRIBUTION));

  const pins = readApimPins(repoRoot);
  const bundled = readBundledModules(distributionDir);
  const findings = checkGrapheneVersions(pins, bundled.modules, bundled.unreadable);

  console.log(formatReport({ pluginsDir: bundled.pluginsDir, artifacts: bundled.artifacts, pins, modules: bundled.modules }, findings));

  return hasErrors(findings) ? 1 : 0;
}

try {
  process.exitCode = main();
} catch (error) {
  console.error(`Graphene version consistency check could not run: ${error instanceof Error ? error.message : error}`);
  process.exitCode = 1;
}
