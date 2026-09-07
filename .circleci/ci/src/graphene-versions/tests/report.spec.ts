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
import { formatReport, hasErrors, ReportContext } from '../report';
import { Finding } from '../rules';

const context: ReportContext = {
  pluginsDir: '/dist/plugins',
  artifacts: ['gravitee-gamma-module-aim-4.3.0-alpha.20.zip'],
  pins: {
    packages: { '@gravitee/graphene-core': '3.14.0' },
    observabilityGraphenePeers: { '@gravitee/graphene-core': '^3.7.0' },
    workspacePins: [],
  },
  modules: [
    {
      module: 'aim',
      artifact: 'gravitee-gamma-module-aim-4.3.0-alpha.20.zip',
      registered: { '@gravitee/graphene-core': '3.15.0' },
      required: {},
    },
  ],
};

const failure: Finding = {
  severity: 'error',
  rule: 'observability-peer-unsatisfied',
  package: '@gravitee/graphene-core',
  pinned: '3.14.0',
  message: '@gravitee/gamma-lib-observability 1.39.2 needs @gravitee/graphene-core ^3.16.0, but APIM pins 3.14.0.',
};

describe('formatReport', () => {
  it('names the package, the pin and the rule that fired', () => {
    // Rendered against an empty context, so nothing here can be satisfied by the input section.
    const report = formatReport({ ...context, modules: [] }, [failure]);

    expect(report).toContain('@gravitee/graphene-core');
    expect(report).toContain('3.14.0');
    expect(report).toContain('observability-peer-unsatisfied');
    expect(report).toContain(failure.message);
  });

  it('names the offending module and the version it registers, for a finding that carries them', () => {
    const behind: Finding = {
      severity: 'warning',
      rule: 'module-graphene-behind-host',
      module: 'aim',
      package: '@gravitee/graphene-core',
      registered: '3.8.0',
      pinned: '3.14.0',
      message: 'aim was built against 3.8.0 but will execute on 3.14.0.',
    };

    const report = formatReport({ ...context, modules: [] }, [behind]);

    expect(report).toContain('aim');
    expect(report).toContain('3.8.0');
    expect(report).toContain('3.14.0');
  });

  it('shows what every module registers, so a passing run is still readable', () => {
    const report = formatReport(context, []);

    expect(report).toContain('gravitee-gamma-module-aim-4.3.0-alpha.20.zip');
    expect(report).toContain('@gravitee/graphene-core');
  });

  it('counts each severity in the summary', () => {
    const report = formatReport(context, [failure, { ...failure, severity: 'warning' }, { ...failure, severity: 'info' }]);

    expect(report).toContain('1 error, 1 warning, 1 note');
  });

  it('says so when nothing fired', () => {
    expect(formatReport(context, [])).toContain('0 errors, 0 warnings, 0 notes');
  });

  it('lists errors before warnings', () => {
    const report = formatReport(context, [{ ...failure, severity: 'warning' }, failure]);

    expect(report.indexOf('ERROR')).toBeLessThan(report.indexOf('WARNING'));
  });
});

describe('hasErrors', () => {
  it('is true when any finding is an error', () => {
    expect(hasErrors([{ ...failure, severity: 'warning' }, failure])).toBe(true);
  });

  it('is false when only warnings and notes fired', () => {
    expect(
      hasErrors([
        { ...failure, severity: 'warning' },
        { ...failure, severity: 'info' },
      ]),
    ).toBe(false);
  });
});
