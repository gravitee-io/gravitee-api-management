import { CustomParameter, CustomParametersList, OrbImport } from '../../sdk/index.mjs';

export const slackOrb = new OrbImport('slack', 'circleci', 'slack', '4.10.1', undefined, {
  jobs: {},
  executors: {},
  commands: {
    notify: new CustomParametersList([
      new CustomParameter('channel', 'string'),
      new CustomParameter('branch_pattern', 'string'),
      new CustomParameter('event', 'string'),
      new CustomParameter('basic_fail_1', 'string'),
    ]),
  },
});
