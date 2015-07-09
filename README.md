[![Build Status](http://build.gravitee.io/jenkins/buildStatus/icon?job=gravitee-management-webui)](http://build.gravitee.io/jenkins/view/Tous/job/gravitee-management-webui/)
# Gravitee Management Webui

# Install

- Install [nodejs] [1]
- It comes with [npm] [2]
- Install [gulp] [3] :
```
sudo npm install -g gulp
```

- You can install from the base dir :
```
npm install
```

  - *npm install* reads **package.json** and generates the directory **node_modules**

- You can always update your project dependencies :
```
npm update
```

# Working with [gulp] [4]

Gulp tasks are run from this base dir.

## Serve

If you wanna serve the built version on dev mode :
```
gulp serve
```

If you wanna serve the built version on production mode :
```
gulp serve:dist
```

If you wanna launch unit tests with Karma :
```
gulp test
```

If you wanna launch unit tests with Karma in watch mode :
```
gulp test:auto
```

If you wanna launch e2e (end to end) tests with Protractor :
```
gulp protractor
```

If you wanna launch e2e tests with Protractor on the dist files :
```
gulp protractor:dist
```

[1]: http://nodejs.org
[2]: http://npmjs.org
[3]: https://github.com/gulpjs/gulp
[4]: http://gulpjs.com
