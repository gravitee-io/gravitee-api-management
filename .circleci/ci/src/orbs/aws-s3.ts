import { orb, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export const awsS3 = new orb.OrbImport(
  'aws-s3',
  'circleci',
  'aws-s3',
  config.orbs.awsS3,
  'Syncs directories and S3 prefixes.\nhttps://docs.aws.amazon.com/cli/latest/reference/s3/sync.html',
  {
    jobs: {},
    executors: {},
    commands: {
      sync: new parameters.CustomParametersList([
        new parameters.CustomParameter(
          'arguments',
          'string',
          '',
          'Optional additional arguments to pass to the `aws sync` command (e.g. `--acl public-read`). Note: if passing a multi-line value to this parameter, include `\\` characters after each line, so the Bash shell can correctly interpret the entire command.',
        ),
        new parameters.CustomParameter('from', 'string'),
        new parameters.CustomParameter('to', 'string'),
      ]),
    },
  },
);
