[![Build Status](http://build.gravitee.io/jenkins/buildStatus/icon?job=gateway-admin-web)](http://build.gravitee.io/jenkins/view/Tous/job/gateway-admin-web/)
# Gravitee-IO

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
gulp serve.dev
```

If you wanna serve the built version on production mode :
```
gulp serve.prod
```

[1]: http://nodejs.org
[2]: http://npmjs.org
[3]: https://github.com/gulpjs/gulp
[4]: http://gulpjs.com
