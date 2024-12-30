## How to run tests

First, install `unittest` helm plugin 

```shell
helm plugin install https://github.com/helm-unittest/helm-unittest.git
```

Inside `apim3` directory, run :

```shell
helm unittest -f 'tests/**/*.yaml' .
```