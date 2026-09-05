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
import { ApimPins, Finding, ModuleRegistrations, Severity } from './rules';

const PREAMBLE = [
  'The Gamma console and every bundled Gamma module register @gravitee/graphene-core into the Module',
  'Federation shared scope as a singleton, so one copy serves them all at runtime. This job reads what',
  'each bundled module registers and compares it to the versions APIM pins in its root package.json.',
  '',
  'It FAILS only when it could not do that reading, or when the pinned gamma-lib-observability needs a',
  'graphene the pin does not provide. A module registering a graphene NEWER than the pin is reported as',
  'a warning, not a failure: whether that breaks the console is unconfirmed, and the runtime suggests it',
  'does not. See the rule text when it fires.',
];

const SEVERITY_ORDER: Severity[] = ['error', 'warning', 'info'];
const SEVERITY_LABEL: Record<Severity, string> = { error: 'ERROR', warning: 'WARNING', info: 'NOTE' };

export interface ReportContext {
  pluginsDir: string;
  artifacts: string[];
  pins: ApimPins;
  modules: ModuleRegistrations[];
}

export function hasErrors(findings: Finding[]): boolean {
  return findings.some((finding) => finding.severity === 'error');
}

export function formatReport(context: ReportContext, findings: Finding[]): string {
  return [
    'Graphene version consistency',
    '============================',
    '',
    ...PREAMBLE,
    '',
    ...inputSection(context),
    '',
    ...findingsSection(findings),
    '',
    summary(findings),
  ].join('\n');
}

function inputSection(context: ReportContext): string[] {
  const lines = [
    `Read from ${context.pluginsDir}: ${count(context.artifacts.length, 'gamma module zip')}, ` +
      `${context.modules.length} carrying a ui/mf-manifest.json.`,
    '',
    'APIM pins (root package.json):',
    ...Object.entries(context.pins.packages).map(([name, version]) => `  ${name.padEnd(34)} ${version}`),
  ];

  context.pins.workspacePins.forEach((workspace) => {
    lines.push('', `${workspace.file}:`, ...Object.entries(workspace.packages).map(([name, version]) => `  ${name.padEnd(34)} ${version}`));
  });

  context.modules.forEach((module) => {
    const graphene = Object.entries(module.registered).filter(([name]) => name.startsWith('@gravitee/graphene-'));
    lines.push(
      '',
      `${module.artifact} registers:`,
      ...(graphene.length > 0
        ? graphene.map(([name, version]) => `  ${name.padEnd(34)} ${version}`)
        : ['  no graphene package in its shared scope']),
    );
  });

  return lines;
}

function findingsSection(findings: Finding[]): string[] {
  if (findings.length === 0) {
    return ['Every bundled module is at or behind the pins. Nothing to report.'];
  }

  return SEVERITY_ORDER.flatMap((severity) =>
    findings
      .filter((finding) => finding.severity === severity)
      .flatMap((finding) => [`${SEVERITY_LABEL[severity]} [${finding.rule}]${subject(finding)}`, ...wrap(finding.message), '']),
  );
}

function subject(finding: Finding): string {
  const parts = [finding.module, finding.package].filter((part) => part !== undefined);
  return parts.length > 0 ? ` ${parts.join(' ')}` : '';
}

function summary(findings: Finding[]): string {
  const counted = SEVERITY_ORDER.map((severity) => findings.filter((finding) => finding.severity === severity).length);
  return `Result: ${count(counted[0], 'error')}, ${count(counted[1], 'warning')}, ${count(counted[2], 'note')}`;
}

function count(quantity: number, noun: string): string {
  return `${quantity} ${noun}${quantity === 1 ? '' : 's'}`;
}

function wrap(message: string, width = 116): string[] {
  return message.split(' ').reduce<string[]>((lines, word) => {
    const current = lines[lines.length - 1];
    if (current !== undefined && `${current} ${word}`.length <= width) {
      lines[lines.length - 1] = `${current} ${word}`;
      return lines;
    }
    return [...lines, `  ${word}`];
  }, []);
}
