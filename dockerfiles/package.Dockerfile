FROM gravitee-portal-webui/dependencies

RUN npm run build:prod

CMD cp -r /usr/build/dist /usr/target/
