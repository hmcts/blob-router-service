#!/usr/bin/env bash

# Script to up Azurite. Ideally for Blob Router at the moment
# Author/contact for updating: Adam Stevenson
AZURITE_NAME="${1}"
INIT_STORAGE_NAME="${2}"

MY_PATH="$(dirname -- "${BASH_SOURCE[0]}")"
PARENT_PATH="$(dirname "${MY_PATH}")"

echo "Initialising Azurite with name of ${AZURITE_NAME} with storage ${INIT_STORAGE_NAME}"

docker-compose -f ${PARENT_PATH}/docker-compose.yml build ${INIT_STORAGE_NAME}
docker-compose -f ${PARENT_PATH}/docker-compose.yml up -d ${AZURITE_NAME}
docker-compose -f ${PARENT_PATH}/docker-compose.yml up -d ${INIT_STORAGE_NAME}
