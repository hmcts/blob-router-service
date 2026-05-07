# CLI scripts for Bulk Scan Processor

## Table of contents

1. [Zip and sign and upload](#zip-and-sign-and-upload)

### Zip and sign and upload

Tool `sign-zip-upload.sh` provides the capabilities to zip the directory of your choosing.
It is considered to have correct metafile json and required pdf files.
It may contain anything else but for correctness - script only takes `*.json` and `*.pdf` files.

After all files are zipped, the signing technique is applied and uploaded to `demo` Bulk Scan Demo blob storage.
The script fetches a container short lived SAS token from blob-router before upload.

Note.
`CONTAINER` must be exported in the shell before running the script.
If help required - ask BSP team.

#### Sample command line

```bash
$ export CONTAINER="mycontainer"
$ ./sign-zip-upload.sh envelope 1-15-02-2020-08-08-19.zip
```

In the above command:
 - `envelope` is folder name existing in working directory
 - `1-15-02-2020-08-08-19.zip` is the zip file name and it should match with the `zip_file_name` property in metafile json

#### Pre-requisites

- Request access to `demo` bulk scan key vault from BSP team
- Azure CLI
- JQ CLI processor
- curl
- &lt;folder-name&gt; e.g. `envelope`
- `CONTAINER` environment variable. Current session where command is executed

The upload token is fetched from:
`http://reform-scan-blob-router-{env}.service.core-compute-{env}.internal/token/{container}`

Response format:

```json
{
  "sas_token": "sv=2020-10-02&spr=https%2Chttp&se=2026-05-07T16%3A47%3A02Z&sr=c&sp=rwl&sig=U..."
}
```

#### Commands used

- az
- jq
- fold
- awk
- date
- openssl
- zip
- curl
