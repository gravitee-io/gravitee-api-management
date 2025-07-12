

<div align="center">
  <a href="https://www.gravitee.io/">
    <picture>
      <source srcset="https://raw.githubusercontent.com/gravitee-io/gravitee-api-management/refs/heads/master/assets/gravitee-dark-mode.svg" media="(prefers-color-scheme: dark)">
      <source srcset="https://raw.githubusercontent.com/gravitee-io/gravitee-api-management/refs/heads/master/assets/gravitee-light-mode.svg" media="(prefers-color-scheme: light)">
      <img src="https://raw.githubusercontent.com/gravitee-io/gravitee-api-management/refs/heads/master/assets/gravitee-dark-mode.svg" alt="Gravitee Logo" style="max-width: 100%; height: auto;">
    </picture>
  </a>
</div>

<br/>

![GitHub Tag](https://img.shields.io/github/v/tag/gravitee-io/gravitee-api-management?style=flat&label=version&color=FF8A00)
[![License](https://img.shields.io/github/license/gravitee-io/gravitee-api-management?style=flat&label=license&color=FF8A00)](https://github.com/gravitee-io/gravitee-api-management/blob/master/LICENSE.txt)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Follow-blue?style=flat&color=22A3B3)](https://www.linkedin.com/company/graviteesource)
[![Twitter](https://img.shields.io/badge/Twitter-Follow-blue?style=flat&color=22A3B3)](https://twitter.com/intent/follow?screen_name=graviteeio)  
![GitHub commit activity](https://img.shields.io/github/commit-activity/m/gravitee-io/gravitee-api-management?style=flat&color=F76C6C
)
![GitHub last commit](https://img.shields.io/github/last-commit/gravitee-io/gravitee-api-management?style=flat&color=F76C6C)
![CircleCI](https://img.shields.io/circleci/build/github/gravitee-io/gravitee-api-management?style=flat&color=F76C6C)
[![Community](https://img.shields.io/badge/community-join-F76C6C?style=flat)](https://community.gravitee.io)
[![Documentation](https://img.shields.io/badge/documentation-see-F76C6C?style=flat)](https://documentation.gravitee.io/apim)
---



<span style="color:#ea3527"><strong>Gravitee API Management</strong></span> (also called <span style="color:#ea3527"><strong>Gravitee APIM</strong></span>)
is a flexible, lightweight, and blazing-fast Open Source solution that helps your organization control who, when, and how users access your APIs. \
Effortlessly manage the lifecycle of your APIs.

Download API Management to document, discover, and publish your APIs.

Different ways to start using <span style="color:#ea3527"><strong>Gravitee APIM</strong></span>:



| Tool                                                                                                                                            | Target                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|-------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| <a href="https://documentation.gravitee.io/gravitee-cloud" style="color:#ea3527; font-weight:bold;">Gravitee Cloud</a>                          | <img src="https://upload.wikimedia.org/wikipedia/commons/f/fa/Microsoft_Azure.svg" alt="Azure Logo" height="20" style=" vertical-align:middle; margin-right:8px;"><img src="https://icon.icepanel.io/Technology/svg/Google-Cloud.svg" alt="Google Cloud Logo" height="20" style="vertical-align:middle; margin-right:8px;"><img src="https://upload.wikimedia.org/wikipedia/commons/9/93/Amazon_Web_Services_Logo.svg" alt="AWS Logo" height="20" style="vertical-align:middle; margin-right:8px;"> |
| <a href="https://github.com/gravitee-io/gravitee-kubernetes-operator" style="color:#ea3527; font-weight:bold;">Gravitee Kubernetes Operator</a> | <img src="https://raw.githubusercontent.com/cncf/artwork/refs/heads/main/projects/kubernetes/horizontal/all-blue-color/kubernetes-horizontal-all-blue-color.svg" alt="Kubernetes Logo" height="35" style=" vertical-align:middle; margin-right:8px;">                                                                                                                                                                                                                                               |
| [docker-compose / make][quick-setup]                                                                                                            | <img src="https://upload.wikimedia.org/wikipedia/commons/4/4e/Docker_%28container_engine%29_logo.svg" alt="Kubernetes Logo" height="20" style=" vertical-align:middle; margin-right:8px;">                                                                                                                                                                                                                                                                                                          |

---
[Installation](https://documentation.gravitee.io/apim/getting-started/local-install-with-docker) | [Documentation](https://documentation.gravitee.io/apim) | [Community](https://community.gravitee.io/) | [Contributing](./CONTRIBUTING.adoc) | [License](./LICENSE.txt) | [Website][gravitee-url]

---


## Getting Started
Do you only have a few minutes or want to try it out without installation or configuration?  
<a href="https://documentation.gravitee.io/gravitee-cloud" style="color:#ea3527; font-weight:bold;">Gravitee Cloud</a> has you covered.  
<strong>Try our Saas offer with a [free trial][free-trial-url] today!</strong>

If you prefer to use <span style="color:#ea3527"><strong>Gravitee APIM</strong></span> on your own infrastructure, follow the steps below.

1) Clone the <span style="color:#ea3527"><strong>Gravitee APIM</strong></span> repository 
```sh
   git clone --depth=1 https://github.com/gravitee-io/gravitee-api-management
   cd gravitee-api-management/docker
```


2) Start the <a href="https://www.gravitee.io/platform/api-management" style="color:#ea3527; font-weight:bold;">Gravitee Console</a>, <a href="https://www.gravitee.io/platform/api-developer-portal" style="color:#ea3527; font-weight:bold;">Portal</a> and <a href="https://www.gravitee.io/platform/api-gateway" style="color:#ea3527; font-weight:bold;">Gateway</a> with a Mongodb database:

 - Run the `Make` command

```sh
  make mongodb
```

- Or the `Docker` command
```sh
   cd quick-setup/mongodb && docker compose down -v && docker compose pull && docker compose up -d
```

3. <span style="color:#ea3527"><strong>Gravitee APIM</strong></span> is now up and running!<br>
   Now let's explore some of the subcomponents you've deployed:

- `:4100` - [Portal UI](http://localhost:4100) ~ A catalog of your APIs, complete with documentation and more.
- `:8084` - [Console UI](http://localhost:8084) ~ The administrative interface for managing your APIs.
- `:8082` - Gateway ~ Gravitee's powerful API gateway.
- `:8083` - [APIM Backend](http://localhost:8083/portal/openapi) ~ Backend for both the Portal and Console UIs.

\
What's next?
[Follow our documentation](https://documentation.gravitee.io/apim/how-to-guides) and learn how to create an API, add a Policy,... and more!

4.  💡 Tips (Optional)

>💡 Tip 1: Use your **Enterprise License**  
If you have an Enterprise License, you can export it as a Base64-encoded environment variable or move your license file into `docker/quick-setup/mongodb/.license` to gain full access to <span style="color:#ea3527"><strong>Gravitee APIM</strong></span> features.
 ```sh
    export LICENSE_KEY=*****
```

>💡 Tip 2: Issue when starting <span style="color:#ea3527"><strong>Gravitee APIM</strong></span>  
If you're having an issue during `.license` folder creation (using Rancher Desktop for example), please run:

```sh
   make prepare TARGET=mongodb
```

## Features

<div style="text-align: center;">
  <span style="
    background: linear-gradient(99deg, #f09135 2.8%, #ea3527 96.58%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    font-weight: bold;
    font-size: 24px;
  ">
    Hold Nothing Back!
  </span>
</div>

-   **Register your API**: Create and register APIs in a few clicks to easily expose your secured APIs to internal and external consumers.
-   **Configure policies using flows**: Gravitee.io API Management provides over 50 pre-built policies to effectively shape
    traffic reaching the gateway according to your business requirements.
-   **Developer portal**: Build the portal that your developers want with a custom theme, full-text search, and API documentation.
-   **Analytics dashboard**: The out-of-the-box dashboards give you a 360-degree view of your API. You can also build your own
    dashboards from Gravitee.io or use all metrics with external tools like Grafana or Kibana.
-   **Register applications**: Users and administrators can register applications for consuming APIs with ease. Gravitee.io
    provides advanced dynamic client registration to link API Management and Access Management effectively.
-   **Secured plans**: Create plans to define the rate limits, quotas, and security policies that apply to your APIs.

[![][gravitee-features]][gravitee-url]


[gravitee-url]: https://www.gravitee.io
[gravitee-features]: https://www.gravitee.io/hubfs/Spiralyze/assets/hero_1002.png
[free-trial-url]: https://eu-auth.cloud.gravitee.io/cloud/register?response_type=code&client_id=fd45d898-e621-4b12-85d8-98e621ab1237&state=enlSaG1YWThMfmc4QXFLZE5aZGdpYUJrcF9VR0tGa3ZRfjBjWm12a2pXY1Bj&redirect_uri=https%3A%2F%2Feu.cloud.gravitee.io&scope=openid+profile+email+offline_access&code_challenge=Zqs-oyj0nSZXIt__zhOTZRmpoR2ShAaQRWksGEQeIYQ&code_challenge_method=S256&nonce=enlSaG1YWThMfmc4QXFLZE5aZGdpYUJrcF9VR0tGa3ZRfjBjWm12a2pXY1Bj
[quick-setup]: docker/README.md
