## [6.3.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/6.3.0...6.3.1) (2025-11-20)


### Bug Fixes

* document reverting 6.3 commit ([45b6f73](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/45b6f7324c2dddc854e86a9b93e7cdf520c33f71))
* document reverting 6.3.0 commit ([037c797](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/037c797df54c0ecc1501ec63d11bb88a5d089d49))


### Reverts

* Revert "feat: add additional metrics to message metrics" ([d865e4a](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/d865e4ab447eec07c74c829b47149afcb30e01f2))

# [6.3.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/6.2.4...6.3.0) (2025-11-18)


### Features

* add additional metrics to message metrics ([18b5cb4](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/18b5cb42c8b52612098191ffa27cf899c657c3e4))

## [6.2.4](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/6.2.3...6.2.4) (2025-10-16)


### Bug Fixes

* update reporter-common to 1.7.2 ([3f67d95](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/3f67d95331600360d94e5e47ec6f18d953891bb4))

## [6.2.3](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/6.2.2...6.2.3) (2025-10-08)


### Bug Fixes

* Elastic Index Error in V2 and V4 http proxy ([85fe7cc](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/85fe7ccf92112a3785bbb1b41f44f2b48d36983f))

## [6.2.2](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/6.2.1...6.2.2) (2025-10-01)


### Bug Fixes

* bump gravitee-reporter-common version ([b27942e](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/b27942ed8154d1d38074e68229152fd52ebfb322))

## [6.2.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/6.2.0...6.2.1) (2025-09-29)


### Bug Fixes

* adapt opensearch for event-metrics ([929de61](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/929de612fbb0e264f12861d1363803c6549e4aca))

# [6.2.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/6.1.2...6.2.0) (2025-09-22)


### Bug Fixes

* add missing stuff for new EVENT_METRICS ([b9646fc](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/b9646fcec06479e3ac31a79fa841a8be210372ae))
* bump ci gravitee orb ([342c907](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/342c907118e8e9afb6768852d151cbf365b4b14b))
* bump gravitee-reporter-common version ([e6c1acd](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/e6c1acdd8e9c64d007a36f481aceecf73c39afa4))
* bump gravitee-reporter-common version ([2c1ce02](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/2c1ce028f7d2401f2b02e557a70902ae4907127c))
* failed repository tests on ES7x ([94ed41b](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/94ed41b505ecc5d152859992f71fc7a8a808a998))


### Features

* add an index template for v4 native event metrics in ES7 ([eee942a](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/eee942aedca828b8739da37850a2d4e89073c008))
* add more metrics in ES index template ([1ca98de](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/1ca98deff82f1ed7f5e00877253f7b324ebfaacb))
* bump dependancies ([ed739cc](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/ed739cc2923b5a2442b81652d4dd9288c3440546))
* bump gravitee-reporter-common version ([13dc3aa](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/13dc3aac0ea8a67dc2ced6f3682eb4520ef94302))
* exclude creating index alias when using data streams ([ad2e160](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/ad2e160e46be585443dbedbe6ef6ac9f5fa58cc0))
* handle failure and warnings metrics in v2 and v4 ([86ec671](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/86ec671682e726a966dd26f2a0ca36cc8b60fe3f))
* prepare index as data stream ([06448f2](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/06448f2db981d2ea9d647b10b5c320268c107b27))
* update circleci config to use gravitee orb version 5.2.1 ([3332f4b](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/3332f4bcb5bca3755b8040e325523099cabb5d3c))
* update index pattern in event metrics templates ([3d6e6e0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/3d6e6e00e315a69ce4b443660846894ddef562b8))

## [6.1.2](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/6.1.1...6.1.2) (2025-08-08)


### Bug Fixes

* **deps:** bump commons-validator to 1.10.0 ([8c8d124](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/8c8d124c3dd6a9d64e382c1e0c92018043e116b8))

## [6.1.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/6.1.0...6.1.1) (2025-07-29)


### Bug Fixes

* bump gravitee-reporter-common version ([57dd8ac](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/57dd8aca8db217c6940a5e4a2ca4fe023242c734))

# [6.1.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/6.0.0...6.1.0) (2025-07-15)


### Features

* add an index template for v4 native event metrics ([eac6647](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/eac6647de2cff2097b5e68636a4fe001e6a97f0c))

# [6.0.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.9.3...6.0.0) (2025-06-10)


* feat!: support additional metrics ([8fb7654](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/8fb7654152c731cee1920112c32837a61e9b3e9a))


### BREAKING CHANGES

* need reporter-api 1.34 or later

## [5.9.3](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.9.2...5.9.3) (2025-06-09)


### Bug Fixes

* ensure compatibility with APIM 4.7 ([a1ca0d5](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/a1ca0d5e5269d393ecf7cbb4541d92a505184cc0))
* fix dependencies management ([e5e0fab](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/e5e0fabe6e8a19dfc02b5223cd4cd779644d1eb3))

## [5.9.2](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.9.1...5.9.2) (2025-06-02)


### Bug Fixes

* rename policy metric to additional metric ([f20eeee](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/f20eeeed32eaf933c20c13ed842de4b40053d529))

## [5.9.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.9.0...5.9.1) (2025-05-22)


### Bug Fixes

* fix mapping ([1cbc485](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/1cbc48599190a4dd46d596b7c5ac8a4f0b5893f4))

# [5.9.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.8.1...5.9.0) (2025-05-15)


### Features

* handle double metrics ([dcd1924](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/dcd1924f66ca02be6b9c2afdd06ed8b659211307))

## [5.8.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.8.0...5.8.1) (2025-05-12)


### Bug Fixes

* ensure to get version 1.32.3 of gravitee-reporter-api ([3bdfa90](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/3bdfa906a0fce53b81989bad8c901483edc18c38))

# [5.8.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.7.0...5.8.0) (2025-05-06)


### Features

* additional metrics long, boolean & keyword ([b94262a](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/b94262aca69df27fc6659f1289e75532358e2a9e))

# [5.7.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.6.0...5.7.0) (2025-04-23)


### Features

* add new AI metrics ([a2a7f12](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/a2a7f12b26c52f051524f12682f331c4ff77b361))

# [5.6.0](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.5.1...5.6.0) (2024-12-05)


### Features

* use new common BulkProcessor ([91e8587](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/91e8587f0b2bf3ad54f54ab127b6920561c5bcbd))

## [5.5.1](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/compare/5.5.0...5.5.1) (2024-11-25)


### Bug Fixes

* use index state management policy id and rollover alias properties for ism ([cbe8105](https://github.com/gravitee-io/gravitee-reporter-elasticsearch/commit/cbe8105de34649fdb2cfc1da9a0cc21365cc66e4))

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
