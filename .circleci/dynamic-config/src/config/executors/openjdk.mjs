import { CustomParametersList, DockerExecutor, ReusableExecutor } from '../../sdk/index.mjs';

import { createClassParameter } from '../parameters/class.mjs';
import { createVersionParameter } from '../parameters/version.mjs';

const image = 'cimg/openjdk:<< parameters.version >><<# parameters.with_node >>-node<</ parameters.with_node >>';

const docker = new DockerExecutor('<< parameters.class >>', image);

const params = new CustomParametersList([createClassParameter('medium'), createVersionParameter('2204')]);

export const OpenJDK = new ReusableExecutor('openjdk', docker, params).defineParameter(
  'with_node',
  'boolean',
  false,
  `should we use the "node" version of the image`,
);
