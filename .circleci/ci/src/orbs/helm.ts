import { orb, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export const helm = new orb.OrbImport('helm', 'circleci', 'helm', config.orbs.helm, undefined, {
  jobs: {},
  executors: {},
  commands: {
    'install-helm-client': new parameters.CustomParametersList([
      new parameters.CustomParameter('version', 'string', 'v3.8.2', 'the helm client version to install. e.g. v3.8.0'),
    ]),
  },
});
