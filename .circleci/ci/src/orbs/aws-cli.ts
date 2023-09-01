import { orb } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export const awsCli = new orb.OrbImport('aws-cli', 'circleci', 'aws-cli', config.orbs.awsCli);
