#!/usr/bin/env bash

set -e

SOURCE_CONNECTION_STRING="DefaultEndpointsProtocol=http;AccountName=reformscanlocal;AccountKey=cmVmb3Jtc2NhbmtleQo=;BlobEndpoint=http://azure-storage-emulator-azurite:10000/reformscanlocal;"

az storage container create --name pcq --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name crime --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name bulkscan --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name bulkscanauto --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name publiclaw --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name privatelaw --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name sscs --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name nfd --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name finrem --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name divorce --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name probate --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name cmc --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name mosh --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name jason --connection-string $SOURCE_CONNECTION_STRING

SOURCE_CONNECTION_STRING2="DefaultEndpointsProtocol=http;AccountName=bulkscanlocal;AccountKey=cmVmb3Jtc2NhbmtleQo=;BlobEndpoint=http://azure-storage-emulator-azurite:10000/bulkscanlocal;"

az storage container create --name pcq --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name pcq-rejected --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name crime --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name crime-rejected --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name bulkscan --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name bulkscan-rejected --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name bulkscanauto --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name bulkscanauto-rejected --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name publiclaw --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name publiclaw-rejected --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name privatelaw --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name privatelaw-rejected --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name sscs --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name sscs-rejected --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name nfd --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name nfd-rejected --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name finrem --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name finrem-rejected --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name divorce --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name divorce-rejected --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name probate --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name probate-rejected --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name cmc --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name cmc-rejected --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name mosh --connection-string $SOURCE_CONNECTION_STRING2
az storage container create --name mosh-rejected --connection-string $SOURCE_CONNECTION_STRING2

SOURCE_CONNECTION_STRING3="DefaultEndpointsProtocol=http;AccountName=crimelocal;AccountKey=cmVmb3Jtc2NhbmtleQo=;BlobEndpoint=http://azure-storage-emulator-azurite:10000/crimelocal;"

az storage container create --name bs-sit-scans-received --connection-string $SOURCE_CONNECTION_STRING3

SOURCE_CONNECTION_STRING4="DefaultEndpointsProtocol=http;AccountName=pcqlocal;AccountKey=cmVmb3Jtc2NhbmtleQo=;BlobEndpoint=http://azure-storage-emulator-azurite:10000/pcqlocal;"

az storage container create --name pcq --connection-string $SOURCE_CONNECTION_STRING4
