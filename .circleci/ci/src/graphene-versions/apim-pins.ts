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
import * as fs from 'fs';
import * as path from 'path';
import { parse } from 'yaml';
import { ApimPins, GRAPHENE_CHARTS, GRAPHENE_CORE, GRAPHENE_POLICY_STUDIO, OBSERVABILITY, WorkspacePins } from './rules';

const TRACKED_PACKAGES = [GRAPHENE_CHARTS, GRAPHENE_CORE, GRAPHENE_POLICY_STUDIO, OBSERVABILITY];

export function readApimPins(repoRoot: string): ApimPins {
  const packages = readPinnedPackages(path.join(repoRoot, 'package.json'));

  const missing = TRACKED_PACKAGES.filter((name) => packages[name] === undefined);
  if (missing.length > 0) {
    throw new Error(`The root package.json no longer pins ${missing.join(', ')}. This check has nothing left to compare against.`);
  }

  return {
    packages,
    observabilityGraphenePeers: readObservabilityGraphenePeers(path.join(repoRoot, 'yarn.lock'), packages[OBSERVABILITY]),
    workspacePins: readWorkspacePins(repoRoot),
  };
}

function readPinnedPackages(packageJsonFile: string): Record<string, string> {
  const manifest = JSON.parse(fs.readFileSync(packageJsonFile, 'utf-8'));
  const declared: Record<string, string> = { ...manifest.dependencies, ...manifest.devDependencies };

  return Object.fromEntries(TRACKED_PACKAGES.filter((name) => declared[name] !== undefined).map((name) => [name, declared[name]]));
}

/**
 * Observability declares graphene as a peer and ships none of its own, so the range it needs is
 * only recorded in the lockfile entry for the version APIM pins.
 */
function readObservabilityGraphenePeers(lockFile: string, observabilityVersion: string): Record<string, string> {
  const descriptor = `${OBSERVABILITY}@npm:${observabilityVersion}`;
  const lock = parse(fs.readFileSync(lockFile, 'utf-8'));

  const entry = Object.entries(lock as Record<string, { peerDependencies?: Record<string, string> }>).find(([descriptors]) =>
    descriptors.split(', ').includes(descriptor),
  );
  if (!entry) {
    throw new Error(`yarn.lock has no entry for ${descriptor}. Run 'yarn install' so the lockfile matches the root package.json.`);
  }

  const peers = entry[1].peerDependencies ?? {};
  return Object.fromEntries(Object.entries(peers).filter(([name]) => TRACKED_PACKAGES.includes(name)));
}

/**
 * Every in-repo Gamma project that pins one of the tracked packages. The Gamma console host has no
 * package.json of its own, so these are the only in-repo pins besides the root.
 */
function readWorkspacePins(repoRoot: string): WorkspacePins[] {
  const gammaRoot = path.join(repoRoot, 'gravitee-gamma');

  return fs
    .readdirSync(gammaRoot, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => path.join('gravitee-gamma', entry.name, 'package.json'))
    .filter((file) => fs.existsSync(path.join(repoRoot, file)))
    .map((file) => ({ file, packages: readPinnedPackages(path.join(repoRoot, file)) }))
    .filter((workspace) => Object.keys(workspace.packages).length > 0);
}
