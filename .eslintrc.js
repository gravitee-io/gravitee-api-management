module.exports = {
  "env": {
    "browser": true,
    "node": true
  },
  "parser": "@typescript-eslint/parser",
  "parserOptions": {
    "project": "tsconfig.json",
    "sourceType": "module"
  },
  "plugins": [
    "eslint-plugin-jsdoc",
    "@typescript-eslint"
  ],
  extends: [
    'angular',
    "eslint:recommended",
    "plugin:@typescript-eslint/eslint-recommended"
  ],
  "reportUnusedDisableDirectives": true,
  "rules": {
    "indent": ["warn", 2],
    'angular/no-service-method': 'off',
    'angular/module-getter': 'off',
    'angular/definedundefined': 'warn',
    'angular/document-service': 'off',
    'angular/no-private-call': 'warn',
    'angular/json-functions': 'warn',
    'angular/timeout-service': 'warn',
    'angular/typecheck-array': 'warn',
    'angular/typecheck-string': 'warn',
    'angular/typecheck-function': 'warn',
    'angular/window-service': 'warn',
    'angular/controller-name': 'warn',
    'angular/module-setter': 'warn',
    'angular/interval-service': 'warn',
    'angular/log': 'warn',
    'angular/di': 'warn',
    'angular/on-watch': 'warn',
    'no-bitwise': 'warn',
    'no-unused-vars': 'warn',
    'no-redeclare': 'warn',
    'no-var': 'warn',
    'prefer-const': 'warn',
    'no-useless-escape': 'warn',
    'no-prototype-builtins': 'warn',
    'no-cond-assign': 'warn',
    'no-irregular-whitespace': 'warn',
    'brace-style': 'warn',
    'no-case-declarations': 'warn',
    "@typescript-eslint/dot-notation": "warn",
    "angular/controller-as-route": "warn",
    "@typescript-eslint/no-unused-expressions": "warn",
    "@typescript-eslint/member-delimiter-style": [
      "warn",
      {
        "multiline": {
          "delimiter": "none",
          "requireLast": true
        },
        "singleline": {
          "delimiter": "semi",
          "requireLast": false
        }
      }
    ],
    "@typescript-eslint/member-ordering": "warn",
    "@typescript-eslint/naming-convention": "off",
    "@typescript-eslint/no-empty-function": "warn",
    "@typescript-eslint/no-var-requires": "warn",
    "@typescript-eslint/quotes": [
      "warn",
      "single"
    ],
    "@typescript-eslint/semi": "warn",
    "@typescript-eslint/type-annotation-spacing": "warn",
    "curly": "error",
    "eol-last": "error",
    "eqeqeq": [
      "warn",
      "smart"
    ],
    "guard-for-in": "warn",
    "id-blacklist": "off",
    "id-match": "off",
    "jsdoc/check-alignment": "error",
    "jsdoc/check-indentation": "warn",
    "jsdoc/newline-after-description": "error",
    "max-len": [
      "off",
      {
        "code": 140
      }
    ],
    "no-caller": "error",
    "no-console": [
      "warn",
      {
        "allow": [
          "warn",
          "dir",
          "timeLog",
          "assert",
          "clear",
          "count",
          "countReset",
          "group",
          "groupEnd",
          "table",
          "dirxml",
          "error",
          "groupCollapsed",
          "Console",
          "profile",
          "profileEnd",
          "timeStamp",
          "context"
        ]
      }
    ],
    "no-debugger": "error",
    "no-empty": "off",
    "no-eval": "error",
    "no-fallthrough": "error",
    "no-new-wrappers": "error",
    "no-trailing-spaces": "error",
    "no-underscore-dangle": "off",
    "no-unused-labels": "error",
    "radix": "warn",
    "spaced-comment": [
      "warn",
      "always",
      {
        "markers": [
          "/"
        ]
      }
    ]
  }
};
