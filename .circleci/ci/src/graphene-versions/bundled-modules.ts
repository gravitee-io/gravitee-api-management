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

/** unzip's exit code for 'no files matched' — the only failure that means the entry is absent. */
const UNZIP_NOTHING_MATCHED = 11;

function runUnzip(args: string[]): string | undefined {
  try {
    return execFileSync('unzip', args, { encoding: 'utf-8', maxBuffer: 16 * 1024 * 1024, stdio: ['ignore', 'pipe', 'ignore'] });
  } catch (error) {
    const failure = error as NodeJS.ErrnoException & { status?: number | null };

    // Without unzip every module would look like it ships no UI, and the run would fail as 'no
    // modules found' instead of naming the real cause.
    if (failure.code === 'ENOENT') {
      throw new Error('unzip is not on the PATH. This check reads each module manifest out of its plugin zip.');
    }

    if (failure.status === UNZIP_NOTHING_MATCHED) {
      return undefined;
    }

    // Anything else means the archive could not be read rather than that it lacks the entry: exit 9
    // for a corrupt or truncated zip, ENOBUFS when an entry outgrows maxBuffer. Reading those as
    // 'ships no UI' is how a damaged artifact passes the check clean.
    const cause = failure.status === undefined || failure.status === null ? failure.code : `exit ${failure.status}`;
    throw new Error(`unzip could not read ${path.basename(args[1])} (${cause})`);
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
    const outcome = readModule(path.join(pluginsDir, artifact), artifact, entries);
    if (outcome === undefined) {
      return;
    }
    if ('reason' in outcome) {
      unreadable.push(outcome);
    } else {
      modules.push(outcome);
    }
  });

  return { pluginsDir, artifacts, modules, unreadable };
}

/**
 * One artifact's outcome: its registrations, why it could not be read, or undefined when it ships
 * no UI at all and so registers nothing to compare.
 */
function readModule(zipFile: string, artifact: string, entries: ZipEntries): ModuleRegistrations | UnreadableModule | undefined {
  let manifest: string | undefined;
  try {
    manifest = entries.read(zipFile, MANIFEST_ENTRY);
  } catch (error) {
    return { artifact, reason: (error as Error).message };
  }

  if (manifest === undefined) {
    // A module shipping no UI registers nothing. One shipping a UI whose manifest moved is a
    // different animal: it registers versions this check cannot see, and staying quiet about it is
    // how a too-new module slips through.
    try {
      return entries.contains(zipFile, UI_DIRECTORY)
        ? { artifact, reason: `it ships a ${UI_DIRECTORY} directory but no ${MANIFEST_ENTRY}` }
        : undefined;
    } catch (error) {
      return { artifact, reason: (error as Error).message };
    }
  }

  try {
    return parseManifest(manifest, artifact);
  } catch (error) {
    return { artifact, reason: `its ${MANIFEST_ENTRY} could not be parsed (${(error as Error).message})` };
  }
}

function parseManifest(manifest: string, artifact: string): ModuleRegistrations {
  const parsed = JSON.parse(manifest) as { name?: string; shared?: { name: string; version: string; requiredVersion?: string }[] };
  const shared = parsed.shared ?? [];

  return {
    module: parsed.name ?? artifact,
    artifact,
    registered: Object.fromEntries(shared.map((entry) => [entry.name, entry.version])),
    // The range Module Federation evaluates at load. `strictVersion` is not in the manifest — it
    // lives in each module's own config — so this is as far as the shipped artifact describes the
    // module's constraint.
    required: Object.fromEntries(shared.filter((entry) => entry.requiredVersion !== undefined).map((e) => [e.name, e.requiredVersion!])),
  };
}
