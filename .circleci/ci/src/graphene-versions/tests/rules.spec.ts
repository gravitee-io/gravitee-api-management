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
import { ApimPins, checkGrapheneVersions, Finding, MINORS_BEHIND_BEFORE_WARNING, ModuleRegistrations, RuleId } from '../rules';

const pins: ApimPins = {
  packages: {
    '@gravitee/graphene-charts': '3.14.0',
    '@gravitee/graphene-core': '3.14.0',
    '@gravitee/graphene-policy-studio': '3.14.0',
    '@gravitee/gamma-lib-observability': '1.39.2',
  },
  observabilityGraphenePeers: {
    '@gravitee/graphene-charts': '^3.7.0',
    '@gravitee/graphene-core': '^3.7.0',
  },
  workspacePins: [],
};

function module(name: string, shared: Record<string, string>): ModuleRegistrations {
  return { module: name, artifact: `gravitee-gamma-module-${name}-1.0.0.zip`, registered: shared };
}

function rulesFired(findings: { rule: RuleId }[]): RuleId[] {
  return findings.map((finding) => finding.rule);
}

function errorsIn(findings: Finding[]): Finding[] {
  return findings.filter((finding) => finding.severity === 'error');
}

describe('checkGrapheneVersions', () => {
  describe('a module registering graphene newer than the host pin', () => {
    it('reports it against graphene-core, the package the host shares with strictVersion', () => {
      const findings = checkGrapheneVersions(pins, [module('aim', { '@gravitee/graphene-core': '3.15.0' })]);

      expect(findings).toContainEqual(
        expect.objectContaining({
          severity: 'warning',
          rule: 'module-registers-newer-graphene',
          module: 'aim',
          package: '@gravitee/graphene-core',
          registered: '3.15.0',
          pinned: '3.14.0',
        }),
      );
    });

    // The host loads its own copy before it registers any remote, and MF only prefers a higher
    // version while none is loaded, so this is not known to break anything.
    it('does not fail the run, since the breakage it describes is unconfirmed', () => {
      const findings = checkGrapheneVersions(pins, [module('aim', { '@gravitee/graphene-core': '3.15.0' })]);

      expect(errorsIn(findings)).toEqual([]);
    });

    it('passes when the module registers exactly the pinned version', () => {
      const findings = checkGrapheneVersions(pins, [module('aim', { '@gravitee/graphene-core': '3.14.0' })]);

      expect(rulesFired(findings)).not.toContain('module-registers-newer-graphene');
    });

    it('compares as semver, not as strings', () => {
      // '3.9.0' > '3.14.0' lexicographically, but 3.9.0 is behind.
      const findings = checkGrapheneVersions(pins, [module('aim', { '@gravitee/graphene-core': '3.9.0' })]);

      expect(rulesFired(findings)).not.toContain('module-registers-newer-graphene');
    });
  });

  describe('a module lagging the host pin', () => {
    it(`warns past ${MINORS_BEHIND_BEFORE_WARNING} minors behind`, () => {
      const findings = checkGrapheneVersions(pins, [module('aim', { '@gravitee/graphene-core': '3.8.0' })]);

      expect(findings).toContainEqual(
        expect.objectContaining({
          severity: 'warning',
          rule: 'module-graphene-behind-host',
          module: 'aim',
          registered: '3.8.0',
          pinned: '3.14.0',
        }),
      );
    });

    it('stays quiet within the tolerated lag', () => {
      const findings = checkGrapheneVersions(pins, [module('aim', { '@gravitee/graphene-core': '3.12.0' })]);

      expect(rulesFired(findings)).not.toContain('module-graphene-behind-host');
    });

    it('warns when the module is a whole major behind', () => {
      const findings = checkGrapheneVersions(pins, [module('aim', { '@gravitee/graphene-core': '2.20.0' })]);

      expect(rulesFired(findings)).toContain('module-graphene-behind-host');
    });
  });

  describe('packages the host does not share', () => {
    it.each([['@gravitee/graphene-charts'], ['@gravitee/graphene-policy-studio']])(
      'reports %s without failing when it is newer',
      (name) => {
        const findings = checkGrapheneVersions(pins, [module('aim', { [name]: '4.0.0' })]);

        expect(findings).toContainEqual(
          expect.objectContaining({ severity: 'info', rule: 'unshared-package-differs-from-pin', package: name }),
        );
        expect(errorsIn(findings)).toEqual([]);
      },
    );

    it('accepts modules registering different majors of them, since each carries its own copy', () => {
      const findings = checkGrapheneVersions(pins, [
        module('aim', { '@gravitee/graphene-charts': '3.8.0', '@gravitee/graphene-policy-studio': '3.8.0' }),
        module('esm', { '@gravitee/graphene-charts': '4.1.0', '@gravitee/graphene-policy-studio': '4.1.0' }),
      ]);

      expect(errorsIn(findings)).toEqual([]);
    });
  });

  describe('observability peer ranges', () => {
    it('fails when the pinned graphene does not satisfy the peer range', () => {
      const stalePeers: ApimPins = { ...pins, observabilityGraphenePeers: { '@gravitee/graphene-core': '^4.0.0' } };

      const findings = checkGrapheneVersions(stalePeers, [module('aim', { '@gravitee/graphene-core': '3.14.0' })]);

      expect(findings).toContainEqual(
        expect.objectContaining({
          severity: 'error',
          rule: 'observability-peer-unsatisfied',
          package: '@gravitee/graphene-core',
          pinned: '3.14.0',
        }),
      );
    });

    it('passes when the pin satisfies every peer range', () => {
      const findings = checkGrapheneVersions(pins, [module('aim', { '@gravitee/graphene-core': '3.14.0' })]);

      expect(rulesFired(findings)).not.toContain('observability-peer-unsatisfied');
    });
  });

  describe('in-repo workspace pins', () => {
    it('reports an in-repo module pinning a graphene version other than the root one', () => {
      const drifted: ApimPins = {
        ...pins,
        workspacePins: [
          { file: 'gravitee-gamma/gravitee-gamma-module-apim/package.json', packages: { '@gravitee/graphene-core': '3.13.0' } },
        ],
      };

      const findings = checkGrapheneVersions(drifted, [module('aim', { '@gravitee/graphene-core': '3.14.0' })]);

      expect(findings).toContainEqual(
        expect.objectContaining({
          severity: 'warning',
          rule: 'workspace-pin-differs-from-root',
          package: '@gravitee/graphene-core',
          registered: '3.13.0',
          pinned: '3.14.0',
        }),
      );
    });

    it('passes when the in-repo pins agree with root', () => {
      const agreeing: ApimPins = {
        ...pins,
        workspacePins: [
          { file: 'gravitee-gamma/gravitee-gamma-module-apim/package.json', packages: { '@gravitee/graphene-core': '3.14.0' } },
        ],
      };

      const findings = checkGrapheneVersions(agreeing, [module('aim', { '@gravitee/graphene-core': '3.14.0' })]);

      expect(rulesFired(findings)).not.toContain('workspace-pin-differs-from-root');
    });
  });

  describe('a module whose manifest could not be read', () => {
    it('fails, rather than let a module it could not inspect pass as clean', () => {
      const findings = checkGrapheneVersions(
        pins,
        [module('aim', { '@gravitee/graphene-core': '3.14.0' })],
        [{ artifact: 'gravitee-gamma-module-esm-1.5.0.zip', reason: 'it ships a ui/ directory but no ui/mf-manifest.json' }],
      );

      expect(findings).toContainEqual(
        expect.objectContaining({
          severity: 'error',
          rule: 'module-manifest-unreadable',
          module: 'gravitee-gamma-module-esm-1.5.0.zip',
        }),
      );
    });

    it('fails even when every module it could read is clean', () => {
      const findings = checkGrapheneVersions(
        pins,
        [module('aim', { '@gravitee/graphene-core': '3.14.0' })],
        [{ artifact: 'gravitee-gamma-module-esm-1.5.0.zip', reason: 'it ships a ui/ directory but no ui/mf-manifest.json' }],
      );

      expect(errorsIn(findings)).toHaveLength(1);
    });
  });

  describe('inputs the check cannot evaluate', () => {
    it('fails loudly when no module registered anything, rather than passing silently', () => {
      const findings = checkGrapheneVersions(pins, []);

      expect(findings).toContainEqual(expect.objectContaining({ severity: 'error', rule: 'no-modules-found' }));
    });

    it('reports a root pin that is a range instead of an exact version', () => {
      const ranged: ApimPins = { ...pins, packages: { ...pins.packages, '@gravitee/graphene-core': '^3.14.0' } };

      const findings = checkGrapheneVersions(ranged, [module('aim', { '@gravitee/graphene-core': '3.14.0' })]);

      expect(findings).toContainEqual(
        expect.objectContaining({ severity: 'warning', rule: 'pin-is-not-exact', package: '@gravitee/graphene-core' }),
      );
    });

    it('accepts a module that registers only a subset of the graphene packages', () => {
      const findings = checkGrapheneVersions(pins, [module('edge', { '@gravitee/graphene-core': '3.14.0' })]);

      expect(errorsIn(findings)).toEqual([]);
    });

    it('accepts a module that registers no graphene package at all', () => {
      const findings = checkGrapheneVersions(pins, [module('authz', { '@gravitee/gamma-modules-sdk': '1.2.0' })]);

      expect(errorsIn(findings)).toEqual([]);
    });
  });

  it('passes on the versions master ships today', () => {
    const today = [
      module('aim', {
        '@gravitee/graphene-core': '3.8.0',
        '@gravitee/graphene-charts': '3.8.0',
        '@gravitee/graphene-policy-studio': '3.8.0',
      }),
      module('authz', { '@gravitee/graphene-core': '3.8.0' }),
      module('edge', { '@gravitee/graphene-core': '3.8.0' }),
      module('esm', { '@gravitee/graphene-core': '3.8.0', '@gravitee/graphene-policy-studio': '3.8.0' }),
    ];

    const findings = checkGrapheneVersions(pins, today);

    expect(findings.filter((finding) => finding.severity === 'error')).toEqual([]);
    expect(rulesFired(findings)).toContain('module-graphene-behind-host');
  });
});
