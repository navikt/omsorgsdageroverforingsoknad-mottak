#!/usr/bin/env sh

echo "Importing Serviceuser credentials"

if test -d /var/run/secrets/nais.io/serviceuser
then
  echo "found files..."
    for FILE in /var/run/secrets/nais.io/serviceuser/*
    do
        FILE_NAME=$(echo $FILE | sed 's:.*/::')
        KEY=NAIS_$FILE_NAME
        VALUE=$(cat "$FILE")

        echo "- exporting $KEY"
        export "$KEY"="$VALUE"
    done
fi