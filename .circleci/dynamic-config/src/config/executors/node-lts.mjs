import { CustomParametersList, DockerExecutor, ReusableExecutor } from '../../sdk/index.mjs';

import { createClassParameter } from '../parameters/class.mjs';

const image = 'cimg/node:16.10';

const docker = new DockerExecutor(image, '<< parameters.class >>');

const params = new CustomParametersList([createClassParameter('medium')]);

export const NodeLTS = new ReusableExecutor('node-lts', docker, params);
