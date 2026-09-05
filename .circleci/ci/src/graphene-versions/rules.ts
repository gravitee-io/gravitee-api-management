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
import * as semver from 'semver';

export const GRAPHENE_CORE = '@gravitee/graphene-core';
export const GRAPHENE_CHARTS = '@gravitee/graphene-charts';
export const GRAPHENE_POLICY_STUDIO = '@gravitee/graphene-policy-studio';
export const OBSERVABILITY = '@gravitee/gamma-lib-observability';

/**
 * The package the host shares as a singleton, and the one this job is really about.
 *
 * The host shares it with `strictVersion: true` and an exact `requiredVersion` inherited from the
 * root package.json, so a module registering a higher version is the case worth watching. Whether
 * it actually breaks anything is NOT established — see the note on the rule below — which is why
 * the finding is a warning rather than a failure.
 *
 * The host shares react, react-dom, react-router-dom and zustand on identical terms. They are left
 * out deliberately: every bundled module already registers all three of the first ones above the
 * pin, and the console renders, so failing on that shape would red-line every backend PR.
 */
const HOST_SINGLETON_PACKAGE = GRAPHENE_CORE;

/**
 * Packages the host does not put in the shared scope — its shared list is react, react-dom,
 * react-router-dom, zustand and graphene-core.
 *
 * The modules do share them with each other, as singletons, so there is still exactly one copy at
 * runtime; with no host copy loaded first, which one wins depends on the order the modules happen
 * to load in. Tolerated by decision: reported so the drift is visible, never failed.
 */
const UNSHARED_PACKAGES = [GRAPHENE_CHARTS, GRAPHENE_POLICY_STUDIO];

/** How far a module may lag APIM's graphene-core pin before the lag is worth reporting. */
export const MINORS_BEHIND_BEFORE_WARNING = 2;

export interface WorkspacePins {
  file: string;
  packages: Record<string, string>;
}

export interface ApimPins {
  /** Exact versions APIM pins, from the root package.json. */
  packages: Record<string, string>;
  /** The graphene peer ranges the pinned observability release declares, from the lockfile. */
  observabilityGraphenePeers: Record<string, string>;
  /** The same packages as pinned by in-repo Gamma projects, which must agree with the root. */
  workspacePins: WorkspacePins[];
}

export type Severity = 'error' | 'warning' | 'info';

export type RuleId =
  | 'module-registers-newer-graphene'
  | 'module-graphene-behind-host'
  | 'unshared-package-differs-from-pin'
  | 'observability-peer-unsatisfied'
  | 'workspace-pin-differs-from-root'
  | 'pin-is-not-exact'
  | 'module-manifest-unreadable'
  | 'no-modules-found';

export interface Finding {
  severity: Severity;
  rule: RuleId;
  message: string;
  module?: string;
  package?: string;
  registered?: string;
  pinned?: string;
}

/** A bundled zip that ships a UI this check could not read the shared scope out of. */
export interface UnreadableModule {
  artifact: string;
  reason: string;
}

/** What one bundled Gamma module declares in the `shared` array of its `ui/mf-manifest.json`. */
export interface ModuleRegistrations {
  module: string;
  artifact: string;
  /** Package name to the version the module registers into the shared scope. */
  registered: Record<string, string>;
}

export function checkGrapheneVersions(pins: ApimPins, modules: ModuleRegistrations[], unreadable: UnreadableModule[] = []): Finding[] {
  const findings: Finding[] = [];

  // A module whose registrations cannot be read is not a module that registers nothing. Passing on
  // one is how a too-new graphene slips through while the run still shows green.
  unreadable.forEach((module) =>
    findings.push({
      severity: 'error',
      rule: 'module-manifest-unreadable',
      module: module.artifact,
      message:
        `${module.artifact} could not be read: ${module.reason}. This check cannot tell what it registers into the ` +
        'shared scope, so it fails rather than skip the module quietly. If the module build moved the manifest, ' +
        'point MANIFEST_ENTRY at its new home.',
    }),
  );

  if (modules.length === 0) {
    findings.push({
      severity: 'error',
      rule: 'no-modules-found',
      message:
        'No bundled Gamma module was found to check. This check only means anything when it reads the modules the ' +
        'distribution actually ships, so it fails rather than pass on an empty input.',
    });
  }

  findings.push(...checkPinsAreExact(pins));
  modules.forEach((module) => findings.push(...checkModule(pins, module)));
  findings.push(...checkObservabilityPeers(pins));
  findings.push(...checkWorkspacePins(pins));

  return findings;
}

/**
 * The strict rule below reads the pin as the exact version the host demands. A range would mean the
 * host accepts a window, which this check does not model — so it refuses to guess.
 */
function checkPinsAreExact(pins: ApimPins): Finding[] {
  return [HOST_SINGLETON_PACKAGE]
    .filter((name) => pins.packages[name] !== undefined && semver.valid(pins.packages[name]) === null)
    .map((name) => ({
      severity: 'warning' as const,
      rule: 'pin-is-not-exact' as const,
      package: name,
      pinned: pins.packages[name],
      message:
        `APIM pins ${name} as '${pins.packages[name]}', which is a range rather than an exact version. ` +
        'This check compares module versions against an exact pin; pin an exact version, or teach this check to model ranges.',
    }));
}

function checkModule(pins: ApimPins, module: ModuleRegistrations): Finding[] {
  const findings: Finding[] = [];

  const core = module.registered[HOST_SINGLETON_PACKAGE];
  const corePin = pins.packages[HOST_SINGLETON_PACKAGE];
  const comparable = core !== undefined && semver.valid(core) !== null && corePin !== undefined && semver.valid(corePin) !== null;

  if (comparable && semver.gt(core, corePin)) {
    findings.push({
      severity: 'warning',
      rule: 'module-registers-newer-graphene',
      module: module.module,
      package: HOST_SINGLETON_PACKAGE,
      registered: core,
      pinned: corePin,
      message:
        `${module.artifact} registers ${HOST_SINGLETON_PACKAGE} ${core}, newer than APIM's pin of ${corePin}. ` +
        'Whether that breaks the console is UNCONFIRMED and this is deliberately not a failure. Module Federation ' +
        "resolves a singleton to the highest registered version only while none is loaded yet (runtime-core's " +
        'findSingletonVersionOrderByVersion), and the host loads its own copy in bootstrap.tsx before it registers ' +
        "any remote — which suggests the host's copy wins and nothing throws. Nobody has loaded the console against " +
        `a too-new module to find out. Raise ${HOST_SINGLETON_PACKAGE} to ${core} in the root package.json to clear ` +
        'this, or run that experiment and make this rule a failure with the evidence.',
    });
  }

  if (comparable && isFarBehind(core, corePin)) {
    findings.push({
      severity: 'warning',
      rule: 'module-graphene-behind-host',
      module: module.module,
      package: HOST_SINGLETON_PACKAGE,
      registered: core,
      pinned: corePin,
      message:
        `${module.artifact} was built against ${HOST_SINGLETON_PACKAGE} ${core} but will execute on APIM's ${corePin}: the host's ` +
        'copy wins the singleton. That is allowed, but the gap is wide enough to be worth a release of the module.',
    });
  }

  UNSHARED_PACKAGES.forEach((name) => {
    const registered = module.registered[name];
    const pinned = pins.packages[name];
    if (!registered || !semver.valid(registered) || !pinned || registered === pinned) {
      return;
    }

    findings.push({
      severity: 'info',
      rule: 'unshared-package-differs-from-pin',
      module: module.module,
      package: name,
      registered,
      pinned,
      message:
        `${module.artifact} registers ${name} ${registered} while APIM pins ${pinned}. The host does not share ${name}, ` +
        'but the modules share it with each other as a singleton, so one copy serves them all and which one wins ' +
        'depends on the order the modules load in. Tolerated by decision — reported so the drift stays visible.',
    });
  });

  return findings;
}

function isFarBehind(registered: string, pinned: string): boolean {
  if (semver.major(registered) < semver.major(pinned)) {
    return true;
  }
  return (
    semver.major(registered) === semver.major(pinned) && semver.minor(pinned) - semver.minor(registered) > MINORS_BEHIND_BEFORE_WARNING
  );
}

/**
 * `@gravitee/gamma-lib-observability` ships no graphene of its own — it declares graphene as a peer
 * and runs against whatever copy the shared scope hands it. Nothing installs a graphene on its
 * behalf, so a peer range APIM's pin does not satisfy is the one dependency that can go silently
 * unsatisfiable.
 */
function checkObservabilityPeers(pins: ApimPins): Finding[] {
  return Object.entries(pins.observabilityGraphenePeers).flatMap(([name, range]) => {
    const pinned = pins.packages[name];
    if (!pinned || !semver.valid(pinned) || semver.satisfies(pinned, range)) {
      return [];
    }

    return [
      {
        severity: 'error' as const,
        rule: 'observability-peer-unsatisfied' as const,
        package: name,
        pinned,
        message:
          `${OBSERVABILITY} ${pins.packages[OBSERVABILITY]} needs ${name} ${range}, but APIM ` +
          `pins ${pinned}. Observability bundles no graphene of its own, so it will run against a copy it does not support. ` +
          'Move the observability pin and the graphene pin together in the root package.json.',
      },
    ];
  });
}

/**
 * The Gamma console host has no package.json of its own and resolves from the workspace root, so
 * root is the pin that reaches the browser.
 *
 * A warning rather than a failure: the root package.json declares no `workspaces`, these in-repo
 * files are Nx project markers whose dependencies are never installed, and they already disagree
 * with the root on react, react-dom and react-router-dom. Failing here would block PRs on files the
 * repo has always let drift.
 */
function checkWorkspacePins(pins: ApimPins): Finding[] {
  return pins.workspacePins.flatMap((workspace) =>
    Object.entries(workspace.packages)
      .filter(([name, version]) => pins.packages[name] !== undefined && pins.packages[name] !== version)
      .map(([name, version]) => ({
        severity: 'warning' as const,
        rule: 'workspace-pin-differs-from-root' as const,
        package: name,
        registered: version,
        pinned: pins.packages[name],
        message:
          `${workspace.file} pins ${name} ${version} while the root package.json pins ${pins.packages[name]}. Nothing ` +
          'installs this file, so the build is unaffected — but the two drifting apart is how the in-repo module ends ' +
          'up documented against a version the host does not ship.',
      })),
  );
}
