#!/bin/sh

echo "Checking toolset..."

command -v az >/dev/null 2>&1 || {
  echo "############################"
  echo >&2 "Please install Azure CLI"
  echo "############################"
  exit 1
}

command -v jq >/dev/null 2>&1 || {
  echo "###################################"
  echo >&2 "Please install JQ CLI processor"
  echo "###################################"
  exit 1
}

if [[ -z "${1}" ]]; then
  echo "Missing folder name in current working directory"
  echo "Folder contents will be considered up to standards, zipped and uploaded to blob storage"
  echo "\n  Re-run script \`./sign-zip-upload.sh <folder-name> <zip-file-name>\`\n"

  exit 1
fi

if [[ -z "${2}" ]]; then
  echo "Missing zip file name"
  echo "\n  Re-run script \`./sign-zip-upload.sh <folder-name> <zip-file-name>\`\n"

  exit 1
fi

ENVI="demo"
DIRECTORY=$1
ZIP_FILE_NAME=$2

if [[ ! -d "$DIRECTORY" ]]; then
  echo "Cannot see $DIRECTORY in `pwd`"
  echo "\n  Re-run script \`./sign-zip-upload.sh <folder-name> <zip-file-name>\`\n"

  exit 1
fi

if [[ -z "$CONTAINER" ]]; then
  echo "Missing container name. Script is incomplete - check `CONTAINER=""` value is set"

  exit 1
fi

if [[ -z "$SAS_TOKEN" ]]; then
  echo "Missing SAS token. Export it into current session or locally per script run."
  echo "Ask BSP Team for help"

  exit 1
fi

echo "Verifying private key for zipping is present..."

# verify private.pem is present. mark for deletion in case it will be retrieved from vault
PEM_EXISTS="false"

if [[ ! -f "private.pem" ]]; then
  az keyvault secret show --vault-name "reform-scan-$ENVI" --name test-signing-key-der | jq -r .value | fold -s -w64 > private.pem
  exec 3<> private.pem && awk -v TEXT="-----BEGIN RSA PRIVATE KEY-----" 'BEGIN {print TEXT}{print}' private.pem >&3
  echo "-----END RSA PRIVATE KEY-----" >> private.pem
  PEM_EXISTS="true"
fi

# signed envelope contents:
ENVELOPE_FILE_NAME=envelope.zip
SIGNATURE_FILE_NAME=signature

echo "Zipping the envelope..."

# zip desired contents (pdf and json only!)
zip -j ${ENVELOPE_FILE_NAME} ${DIRECTORY}/*.json ${DIRECTORY}/*.pdf

# sign
openssl dgst -sha256 -sign private.pem -out ${SIGNATURE_FILE_NAME} ${ENVELOPE_FILE_NAME}
zip -j ${ZIP_FILE_NAME} ${ENVELOPE_FILE_NAME} ${SIGNATURE_FILE_NAME} > /dev/null 2>&1

# remove retrieved private key
if [[ "$PEM_EXISTS" == "true" ]]; then
  rm private.pem
fi

# cleanup
rm ${ENVELOPE_FILE_NAME}
rm ${SIGNATURE_FILE_NAME}

echo "Uploading $ZIP_FILE_NAME to blob storage..."

CLEAN_CONTAINER=$(echo "$CONTAINER" | tr -d '\n' | xargs)
CLEAN_ZIP_FILE_NAME=$(echo "$ZIP_FILE_NAME" | tr -d '\n' | xargs)
CLEAN_SAS_TOKEN=$(echo "$SAS_TOKEN" | tr -d '\n' | xargs)

response=$(curl -s -o /dev/null -w "%{http_code}" -X PUT --upload-file "$CLEAN_ZIP_FILE_NAME" \
  -H "x-ms-date: $(date -u)" \
  -H "x-ms-blob-type: BlockBlob" \
  -H "Content-Type: application/octet-stream" \
  "https://reformscan.$ENVI.platform.hmcts.net/$CLEAN_CONTAINER/$CLEAN_ZIP_FILE_NAME?$CLEAN_SAS_TOKEN")

if [ "$response" -eq 201 ]; then
  echo "Upload successful!"
else
  echo "Upload failed with status $response"

# more cleanup
rm ${ZIP_FILE_NAME}
