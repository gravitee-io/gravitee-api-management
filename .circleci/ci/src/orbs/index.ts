import { artifactory } from './artifactory';
import { awsCli } from './aws-cli';
import { awsS3 } from './aws-s3';
import { github } from './github';
import { helm } from './helm';
import { keeper } from './keeper';
import { slack } from './slack';
import { snyk } from './snyk';

export const orbs = {
  artifactory,
  awsCli,
  awsS3,
  github,
  helm,
  keeper,
  slack,
  snyk,
};
