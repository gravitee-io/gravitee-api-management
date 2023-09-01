import { orb, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export const slack = new orb.OrbImport('slack', 'circleci', 'slack', config.orbs.slack, undefined, {
  jobs: {},
  executors: {},
  commands: {
    notify: new parameters.CustomParametersList([
      new parameters.CustomParameter('channel', 'string'),
      new parameters.CustomParameter('custom', 'string'),
      new parameters.CustomParameter('branch_pattern', 'string'),
      new parameters.CustomParameter('event', 'string'),
      new parameters.CustomParameter('template', 'string'),
    ]),
  },
});
