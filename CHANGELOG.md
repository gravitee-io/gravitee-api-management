# [5.5.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.4.0...5.5.0) (2024-11-07)


### Features

* index success attribute in health index ([e27e54c](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/e27e54c5077e117cccd230fb56df888eed5edc93))

# [5.4.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.3.2...5.4.0) (2024-07-10)


### Features

* Show API name in reporters ([667eddb](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/667eddb96dac42a4a9da7606fb5badfde3916a8a))

## [5.3.2](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.3.1...5.3.2) (2024-06-04)


### Bug Fixes

* **deps:** update dependency io.gravitee.reporter:gravitee-reporter-common to v1.2.2 ([4958e2c](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/4958e2c7609341308a2e2dc888832599c18712f1))

## [5.3.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.3.0...5.3.1) (2024-05-22)


### Bug Fixes

* bump reporter-common for Monitor issue ([aa0b439](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/aa0b43943c441714c6e33258fe1fdc54898e1e23))

# [5.3.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.2.0...5.3.0) (2024-05-06)


### Features

* report count increments in v4 messages metrics index ([a18d8a7](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/a18d8a7c5e959e3b0a5fe464d195066b3b58a724))

# [5.2.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.1.3...5.2.0) (2024-04-17)


### Features

* report entrypoint id in v4 metrics index ([8aae560](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/8aae5606f9e09ebf6d9a45f828e9f5eee87d5c9f))

## [5.1.3](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.1.2...5.1.3) (2024-04-04)


### Bug Fixes

* bump reporter-common for Monitor issue ([7cf41b8](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/7cf41b8c22795bd5d346224242ccda92b7343183))

## [5.1.2](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.1.1...5.1.2) (2024-01-31)


### Bug Fixes

* **deps:** update dependency io.gravitee.reporter:gravitee-reporter-common to v1.0.5 ([26cf85e](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/26cf85e90ccf418b69d63fd24454d59c3838e521))

## [5.1.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.1.0...5.1.1) (2024-01-30)


### Bug Fixes

* **deps:** update dependency io.gravitee:gravitee-parent to v22.0.19 ([eaf8d8c](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/eaf8d8ce783d496fc41e19ac4b720fe44fdb5ccb))
* **deps:** update dependency io.gravitee.common:gravitee-common to v3.3.3 ([32c442e](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/32c442ed3904174e714a282ea959b9430b4667f8))

# [5.1.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.0.3...5.1.0) (2023-11-10)


### Features

* use index template instead of legacy template in ES8 ([85bf25a](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/85bf25a49305873433dc0844102fb51ce9ea4cda))

## [5.0.3](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.0.2...5.0.3) (2023-11-06)


### Bug Fixes

* bump gravitee-reporter-common to fix espace issue ([75a7f0a](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/75a7f0a2e996bb1c2c2971280113488c2c378316))

## [5.0.2](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.0.1...5.0.2) (2023-09-12)


### Bug Fixes

* add ingest pipelines plugins template ([e6ce2d3](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/e6ce2d371106982b652b8597a6b4e13fe1d6e1dc))

## [5.0.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.0.0...5.0.1) (2023-08-31)


### Bug Fixes

* move reporter common dependency scope to compile ([2b1608f](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/2b1608f84e6b35ef43b5f0be174da0bf398b9c52))

# [5.0.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.2.2...5.0.0) (2023-08-31)


### Code Refactoring

* drop support for es5 and es6 ([1f86cee](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/1f86cee0c2dc39ae780e668c644831e1125b5762))


### Features

* use gravitee-reporter-common for indexing ([b6a124b](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/b6a124b3e3380298a7dc5c65607d8262e798783c))


### BREAKING CHANGES

* dropped support for es5 and es6 - needs apim 4.1.x

## [4.2.2](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.2.1...4.2.2) (2023-08-28)


### Bug Fixes

* read pem certs and keys as a list for ElasticSearch configuration ([3de5958](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/3de5958b2e9f484a8b238db9f1f58111932610b9))

## [4.2.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.2.0...4.2.1) (2023-05-09)


### Bug Fixes

* **deps:** update dependency io.gravitee.node:gravitee-node-api to v2.0.8 ([cb8ebb3](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/cb8ebb3f8351104740240d1a17463a8fb2b7e70c))

# [4.2.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.1.0...4.2.0) (2023-04-05)


### Features

* add support for OpenSearch 2 ([c3e41a3](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/c3e41a3bd45d5a372126ab7245393711a2609d0e))

# [4.1.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.0.1...4.1.0) (2023-03-17)


### Bug Fixes

* bump common elasticsearch version ([f6e9ece](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/f6e9ece91e16dbc78b1de68251e9376e881d8589))
* bump common elasticsearch version to 4.1.0-alpha.6 ([37628cf](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/37628cfd6c545751596c3c6e51b4fb59bc4a4ad2))
* bump common elasticsearch version to get new error status on message metrics ([e0f1ab9](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/e0f1ab9d787327e1f937b0b3a5741fe1da4c0a9c))
* bump common-elasticsearch version ([41173f7](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/41173f77258ea39cb342b228f3c7d437c7044df0))
* **deps:** upgrade gravitee-bom & alpha version ([35acf27](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/35acf2789dc931fa77e4340e3f47ba5ceb8406eb))
* rename metrics for message ([08894e1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/08894e18b5fe4624f4519fd8d1341be4d0671ce7))
* rename v4 metrics ([91c644f](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/91c644fa3943eea1d94455891c122cededa98fbd))


### Features

* add support for ES 8.x ([fd10d8a](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/fd10d8a8881339fec00ed3606b3e451cf68b7dc3))
* report v4 metrics & logs ([cf1ffd8](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/cf1ffd82acea94234e5008b01773294118082342))
* share all ES templates in gravitee-common-elasticsearch ([d8c55f6](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/d8c55f6844f60dd3ab1d9d284210a50433dfdaad))
* support ES 8.x ([efec022](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/efec022f844b51335328ffc7702a4369359c4282))
* support ES 8.x ([f045a5f](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/f045a5fac0372cc6172d8b1fb8bf426cc7ea8ffe))

# [4.1.0-alpha.9](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.1.0-alpha.8...4.1.0-alpha.9) (2023-03-16)


### Bug Fixes

* bump common-elasticsearch version ([41173f7](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/41173f77258ea39cb342b228f3c7d437c7044df0))

# [4.1.0-alpha.8](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.1.0-alpha.7...4.1.0-alpha.8) (2023-03-10)


### Features

* add support for ES 8.x ([fd10d8a](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/fd10d8a8881339fec00ed3606b3e451cf68b7dc3))
* support ES 8.x ([efec022](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/efec022f844b51335328ffc7702a4369359c4282))
* support ES 8.x ([f045a5f](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/f045a5fac0372cc6172d8b1fb8bf426cc7ea8ffe))

# [4.1.0-alpha.7](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.1.0-alpha.6...4.1.0-alpha.7) (2023-02-01)


### Bug Fixes

* bump common elasticsearch version to get new error status on message metrics ([e0f1ab9](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/e0f1ab9d787327e1f937b0b3a5741fe1da4c0a9c))

# [4.1.0-alpha.6](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.1.0-alpha.5...4.1.0-alpha.6) (2023-01-27)


### Bug Fixes

* bump common elasticsearch version to 4.1.0-alpha.6 ([37628cf](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/37628cfd6c545751596c3c6e51b4fb59bc4a4ad2))

# [4.1.0-alpha.5](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.1.0-alpha.4...4.1.0-alpha.5) (2023-01-25)


### Bug Fixes

* bump common elasticsearch version ([f6e9ece](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/f6e9ece91e16dbc78b1de68251e9376e881d8589))

# [4.1.0-alpha.4](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.1.0-alpha.3...4.1.0-alpha.4) (2023-01-25)


### Bug Fixes

* rename v4 metrics ([91c644f](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/91c644fa3943eea1d94455891c122cededa98fbd))

# [4.1.0-alpha.3](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.1.0-alpha.2...4.1.0-alpha.3) (2023-01-24)


### Bug Fixes

* rename metrics for message ([08894e1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/08894e18b5fe4624f4519fd8d1341be4d0671ce7))

# [4.1.0-alpha.2](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.1.0-alpha.1...4.1.0-alpha.2) (2023-01-16)


### Features

* report v4 metrics & logs ([cf1ffd8](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/cf1ffd82acea94234e5008b01773294118082342))

# [4.1.0-alpha.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.0.0...4.1.0-alpha.1) (2022-12-22)


### Features

* share all ES templates in gravitee-common-elasticsearch ([d8c55f6](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/d8c55f6844f60dd3ab1d9d284210a50433dfdaad))

## [4.0.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/4.0.0...4.0.1) (2023-03-16)


### Bug Fixes

* use custom body analyzer ([a910eb3](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/a910eb3faf7f41653162cda96ecd11a90282689c))

# [4.0.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/3.12.4...4.0.0) (2022-12-09)


### chore

* bump to rxJava3 ([be029a0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/be029a04e4e4c76ca3b3ece147df22dcbddb60e3))


### BREAKING CHANGES

* rxJava3 required

# [4.0.0-alpha.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/3.12.4...4.0.0-alpha.1) (2022-10-19)


### chore

* bump to rxJava3 ([be029a0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/be029a04e4e4c76ca3b3ece147df22dcbddb60e3))


### BREAKING CHANGES

* rxJava3 required

## [3.12.4](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/3.12.3...3.12.4) (2022-06-30)


### Bug Fixes

* use map iteration style for headers in logging - missed blocks ([5a256cd](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/5a256cd150dda9561b55cb414568dcec00455ee9)), closes [graviteeio/issues#7930](https://github.com/graviteeio/issues/issues/7930)

## [3.12.3](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/3.12.2...3.12.3) (2022-06-30)


### Bug Fixes

* use map iteration style for headers in health and logging ([1f09a65](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/1f09a651b66cfba78e0f423b62a2a2e59098c012)), closes [graviteeio/issues#7930](https://github.com/graviteeio/issues/issues/7930)

## [3.12.2](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/3.12.1...3.12.2) (2022-06-10)


### Bug Fixes

* align template api response time type with metrics api response time type ([78e7eb7](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/78e7eb77a53a56ad3e5a53602812e5dc6fc26d65))

## [3.12.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/3.12.0...3.12.1) (2022-05-18)


### Bug Fixes

* create index and alias to make ILM works ([f3611bc](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/f3611bc2c21e41a93e79895417afc8c64c91c8ff)), closes [gravitee-io/issues#7110](https://github.com/gravitee-io/issues/issues/7110)

## [3.8.5](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/3.8.4...3.8.5) (2022-05-17)


### Bug Fixes

* create index and alias to make ILM works ([f3611bc](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/f3611bc2c21e41a93e79895417afc8c64c91c8ff)), closes [gravitee-io/issues#7110](https://github.com/gravitee-io/issues/issues/7110)
