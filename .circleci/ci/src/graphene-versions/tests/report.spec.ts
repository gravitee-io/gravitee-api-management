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
    { module: 'aim', artifact: 'gravitee-gamma-module-aim-4.3.0-alpha.20.zip', registered: { '@gravitee/graphene-core': '3.15.0' } },
  ],
};

const failure: Finding = {
  severity: 'error',
  rule: 'module-registers-newer-graphene',
  module: 'aim',
  package: '@gravitee/graphene-core',
  registered: '3.15.0',
  pinned: '3.14.0',
  message: 'aim registers a newer graphene than APIM pins, so the console will not render.',
};

describe('formatReport', () => {
  it('names the offending module, the version it registers, the pin and the rule that fired', () => {
    const report = formatReport(context, [failure]);

    expect(report).toContain('aim');
    expect(report).toContain('3.15.0');
    expect(report).toContain('3.14.0');
    expect(report).toContain('module-registers-newer-graphene');
    expect(report).toContain(failure.message);
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
