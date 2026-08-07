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
import { config } from '../../config';

/**
 * Which parts of the build a set of changed files should wake up.
 *
 * These predicates are the single place where "this change can only affect X" is expressed.
 * They live apart from any one workflow because several of them need the same answers, and
 * because narrowing or widening a predicate is a deliberate change to CI coverage that
 * deserves to be reviewed on its own.
 */
export function shouldBuildAll(changedFiles: string[]): boolean {
  const baseDepsIdentifiers = ['.circleci', '.gitignore', '.prettierrc', 'gravitee-apim-distribution/gravitee-apim-distribution-e2e'];
  // The root pom is matched exactly. These identifiers are substrings, so listing 'pom.xml' here
  // meant that touching any pom in the repository — a plugin version bump included — rebuilt and
  // retested the whole product. Module poms are covered by the per-module predicates below.
  return changedFiles.some((file) => file === 'pom.xml' || baseDepsIdentifiers.some((identifier) => file.includes(identifier)));
}

export function shouldBuildAllFront(changedFiles: string[]): boolean {
  const frontDepsIdentifiers = ['package.json', 'nx.json', 'yarn.lock'];
  return shouldBuildAll(changedFiles) || changedFiles.some((file) => frontDepsIdentifiers.some((identifier) => file.includes(identifier)));
}

export function shouldBuildHelm(changedFiles: string[]): boolean {
  const helmDepsIdentifiers = ['helm'];
  return shouldBuildAll(changedFiles) || changedFiles.some((file) => helmDepsIdentifiers.some((identifier) => file.includes(identifier)));
}

export function shouldBuildWebuiLibs(changedFiles: string[]): boolean {
  return shouldBuildAllFront(changedFiles) || changedFiles.some((file) => file.includes('gravitee-apim-webui-libs'));
}

export function shouldBuildConsole(changedFiles: string[]): boolean {
  return (
    shouldBuildAllFront(changedFiles) ||
    changedFiles.some((file) => file.includes(config.components.console.workdir)) ||
    changedFiles.some((file) => file.includes('gravitee-apim-webui-libs'))
  );
}

export function shouldBuildPortalNext(changedFiles: string[]): boolean {
  return (
    shouldBuildAllFront(changedFiles) ||
    changedFiles.some((file) => file.includes(config.components.portal.next.project)) ||
    changedFiles.some((file) => file.includes('gravitee-apim-webui-libs'))
  );
}

export function shouldBuildPortal(changedFiles: string[]): boolean {
  return (
    shouldBuildAllFront(changedFiles) ||
    changedFiles.some((file) => file.includes(config.components.portal.workdir) && !file.includes(config.components.portal.next.project))
  );
}

export function shouldBuildGammaUI(changedFiles: string[]): boolean {
  return shouldBuildAllFront(changedFiles) || changedFiles.some((file) => file.includes(config.components.gamma.rootDir));
}

export function shouldBuildBackend(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = [
    'pom.xml',
    'gravitee-apim-bom',
    'gravitee-apim-common',
    'gravitee-apim-definition',
    'gravitee-apim-distribution',
    'gravitee-apim-gateway',
    'gravitee-apim-distribution/gravitee-apim-distribution-integration-tests',
    'gravitee-apim-parent',
    'gravitee-apim-plugin',
    'gravitee-apim-reporter',
    'gravitee-apim-repository',
    'gravitee-apim-rest-api',
    'gravitee-gamma',
  ];
  return (
    shouldBuildAll(changedFiles) || changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

export function shouldTestAllBackend(changedFiles: string[]): boolean {
  // Same reasoning as shouldBuildAll: only the root pom is a trunk-wide change. The modules that
  // genuinely affect everything — the bom, the parent, common, definition, repository — are listed
  // explicitly right below.
  const mavenProjectsIdentifiers = [
    'gravitee-apim-bom',
    'gravitee-apim-common',
    'gravitee-apim-definition',
    'gravitee-apim-parent',
    'gravitee-apim-repository',
  ];
  // No need to test the root pom here: shouldBuildAll, called just above, already does.
  return (
    shouldBuildAll(changedFiles) || changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

export function shouldTestDefinition(changedFiles: string[]): boolean {
  return shouldTestAllBackend(changedFiles) || changedFiles.some((file) => file.includes('gravitee-apim-definition'));
}

export function shouldTestIntegrationTests(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = [
    'gravitee-apim-bom',
    'gravitee-apim-common',
    'gravitee-apim-definition',
    'gravitee-apim-gateway',
    // Anything in the distribution, the plugin version catalog included: a bundled plugin bump is
    // exactly what these tests exist to verify.
    'gravitee-apim-distribution',
    'gravitee-apim-parent',
    'gravitee-apim-plugin',
    'gravitee-apim-reporter',
  ];
  return (
    shouldTestAllBackend(changedFiles) ||
    changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

export function shouldTestGateway(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = ['gravitee-apim-definition', 'gravitee-apim-repository', 'gravitee-apim-gateway'];
  return (
    shouldTestAllBackend(changedFiles) ||
    changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

export function shouldTestReporter(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = ['gravitee-apim-reporter'];
  return (
    shouldTestAllBackend(changedFiles) ||
    changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

export function shouldTestRepository(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = ['gravitee-apim-definition', 'gravitee-apim-repository'];
  return (
    shouldTestAllBackend(changedFiles) ||
    changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

export function shouldTestPlugin(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = ['gravitee-apim-definition', 'gravitee-apim-plugin'];
  return (
    shouldTestAllBackend(changedFiles) ||
    changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}

export function shouldTestRestApi(changedFiles: string[]): boolean {
  const mavenProjectsIdentifiers = ['gravitee-apim-definition', 'gravitee-apim-repository', 'gravitee-apim-rest-api'];
  return (
    shouldTestAllBackend(changedFiles) ||
    changedFiles.some((file) => mavenProjectsIdentifiers.some((identifier) => file.includes(identifier)))
  );
}
