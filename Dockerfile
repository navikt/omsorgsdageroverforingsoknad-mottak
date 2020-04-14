FROM navikt/java:11

COPY docker-init-scripts/import-serviceuser-credentials.sh /init-scripts/20-import-serviceuser-credentials.sh

COPY build/libs/app.jar ./