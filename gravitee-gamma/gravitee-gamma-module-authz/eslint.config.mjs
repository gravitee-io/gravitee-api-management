import prettier from 'eslint-config-prettier';
import grapheneConfig from '@gravitee/graphene-core/eslint';

export default [{ ignores: ['dist/', 'target/', 'rsbuild.config.ts', 'vitest.config.ts'] }, ...grapheneConfig, prettier];
