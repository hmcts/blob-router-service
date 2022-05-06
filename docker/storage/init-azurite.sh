#!/usr/bin/env bash

set -e

SOURCE_CONNECTION_STRING="DefaultEndpointsProtocol=http;AccountName=reformscanlocal;AccountKey=cmVmb3Jtc2NhbmtleQo=;BlobEndpoint=http://azure-storage-emulator-azurite:10000/reformscanlocal;"

az storage container create --name pcq --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name crime --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name bulkscan --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name bulkscanauto --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name publiclaw --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name sscs --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name nfd --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name finrem --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name divorce --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name probate --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name cmc --connection-string $SOURCE_CONNECTION_STRING


