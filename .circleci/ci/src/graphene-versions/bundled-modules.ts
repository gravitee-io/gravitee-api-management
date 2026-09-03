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
import { execFileSync } from 'child_process';
import * as fs from 'fs';
import * as path from 'path';
import { ModuleRegistrations, UnreadableModule } from './rules';

const MODULE_ZIP = /^gravitee-gamma-module-.*\.zip$/;
const UI_DIRECTORY = 'ui/';
const MANIFEST_ENTRY = 'ui/mf-manifest.json';

/**
 * The seam between the zips on disk and the comparison. `unzip` streams a single entry out of an
 * archive that runs to tens of megabytes, so nothing is ever extracted.
 */
export interface ZipEntries {
  /** The entry's content, or undefined when the archive does not carry it. */
  read(zipFile: string, entry: string): string | undefined;
  contains(zipFile: string, directory: string): boolean;
}

const unzipEntries: ZipEntries = {
  read(zipFile: string, entry: string): string | undefined {
    return runUnzip(['-p', zipFile, entry]);
  },
  contains(zipFile: string, directory: string): boolean {
    return runUnzip(['-l', zipFile, `${directory}*`]) !== undefined;
  },
};

function runUnzip(args: string[]): string | undefined {
  try {
    return execFileSync('unzip', args, { encoding: 'utf-8', maxBuffer: 16 * 1024 * 1024, stdio: ['ignore', 'pipe', 'ignore'] });
  } catch (error) {
    // Without unzip every module would look like it ships no UI, and the run would fail as 'no
    // modules found' instead of naming the real cause.
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
      throw new Error('unzip is not on the PATH. This check reads each module manifest out of its plugin zip.');
    }
    return undefined;
  }
}

export interface BundledModules {
  pluginsDir: string;
  /** Every gamma module zip the distribution bundles, whether or not it carries a UI. */
  artifacts: string[];
  modules: ModuleRegistrations[];
  /** Zips that ship a UI whose manifest could not be read — see the rule of the same name. */
  unreadable: UnreadableModule[];
}

/**
 * Reads what each bundled Gamma module registers into the Module Federation shared scope, straight
 * out of the zips the distribution ships. Each module's `plugin-assembly.xml` maps its built
 * `target/classes/ui` to `ui/`, so the manifest travels inside the plugin zip.
 */
export function readBundledModules(distributionDir: string, entries: ZipEntries = unzipEntries): BundledModules {
  const pluginsDir = path.join(distributionDir, 'plugins');
  if (!fs.existsSync(pluginsDir)) {
    throw new Error(
      `No plugins directory at ${pluginsDir}. This check reads the distribution built by 'Build backend'; ` +
        'attach its workspace, or point --distribution at the built distribution.',
    );
  }

  const artifacts = fs
    .readdirSync(pluginsDir)
    .filter((file) => MODULE_ZIP.test(file))
    .sort();

  const modules: ModuleRegistrations[] = [];
  const unreadable: UnreadableModule[] = [];

  artifacts.forEach((artifact) => {
    const zipFile = path.join(pluginsDir, artifact);
    const manifest = entries.read(zipFile, MANIFEST_ENTRY);

    if (manifest === undefined) {
      // A module that ships no UI at all registers nothing, so there is nothing to compare. One
      // that ships a UI whose manifest moved is a different animal: it registers versions this
      // check cannot see, and staying quiet about it is how a too-new module slips through.
      if (entries.contains(zipFile, UI_DIRECTORY)) {
        unreadable.push({ artifact, reason: `it ships a ${UI_DIRECTORY} directory but no ${MANIFEST_ENTRY}` });
      }
      return;
    }

    try {
      modules.push(parseManifest(manifest, artifact));
    } catch (error) {
      unreadable.push({ artifact, reason: `its ${MANIFEST_ENTRY} could not be parsed (${(error as Error).message})` });
    }
  });

  return { pluginsDir, artifacts, modules, unreadable };
}

function parseManifest(manifest: string, artifact: string): ModuleRegistrations {
  const parsed = JSON.parse(manifest) as { name?: string; shared?: { name: string; version: string }[] };

  return {
    module: parsed.name ?? artifact,
    artifact,
    registered: Object.fromEntries((parsed.shared ?? []).map((shared) => [shared.name, shared.version])),
  };
}
