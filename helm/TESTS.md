## How to run tests

First, install `unittest` helm plugin 

```shell
helm plugin install https://github.com/quintush/helm-unittest
```

Inside `helm` directory, run :

```shell
helm unittest -3 -f 'tests/**/*.yaml' .
```