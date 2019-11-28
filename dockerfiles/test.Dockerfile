FROM gravitee-portal-webui/dependencies

RUN npm run lint
RUN npm run test:ci

CMD cp -r /usr/build/coverage /usr/target/
