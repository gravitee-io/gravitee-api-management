# Rate Limiting with Redis

Here is a docker-compose to run APIM with Redis as database for Rate Limiting

You can classically call your apis through your gateway, for example: `http://localhost:8082/myapi`.

---
> For more information, please read this doc:
> https://documentation.gravitee.io/apim/getting-started/configuration/repositories/redis
---

## Verify Rate Limit counters in Redis

This docker-compose comes with a Redis GUI for you to be able to check the counters. 

You can access it through `http://localhost:8001`. You have to configure it to access you database, you can see help here: https://docs.redis.com/latest/ri/using-redisinsight/add-instance/

⚠️ If you are a MacOS user, the host should not be `localhost` but `docker.for.mac.localhost`.

## How to run ?

⚠️ As Redis Repository is not bundled by default, do not forget to download the zip file related to the version you want to run.
The zip is downloadable here: https://download.gravitee.io/#graviteeio-apim/plugins/repositories/

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`
