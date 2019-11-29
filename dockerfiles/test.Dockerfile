FROM gravitee-portal-webui/dependencies

RUN npm run lint
RUN npm run test:ci

CMD ["bash", "-c", "cp -r /usr/build/coverage /usr/target/ && chmod 777 -R /usr/target/"]
