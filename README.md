# Blob Router

![](https://github.com/hmcts/blob-router-service/workflows/CI/badge.svg)
[![](https://github.com/hmcts/blob-router-service/workflows/Publish%20Swagger%20Specs/badge.svg)](https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/blob-router-service.json)
[![codecov](https://codecov.io/gh/hmcts/blob-router-service/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/blob-router-service)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c99af8bcd53947deb32e8f0a7c500676)](https://www.codacy.com/manual/HMCTS/blob-router-service)

## Table of Contents

* [Purpose](#Purpose)
* [Building and deploying the application](#building-and-deploying-the-application)
  * [Building the application](#building-the-application)
  * [Running the application](#running-the-application)
  * [Quick Start](#quick-start)
* [API gateway](#api-gateway)
  * [Calling the API](#calling-the-api)
  * [Preparing client certificate](#preparing-client-certificate)
  * [Retrieving subscription key](#retrieving-subscription-key)
  * [Getting the token through the API](#getting-the-token-through-the-api)
* [License](#license)

## Purpose

Primary responsibility of this micro service is to retrieve blobs from source blob storage containers and then dispatch
them to destination blob storage containers based on source containers.
Currently it only routes blobs (zip files) to CFT and Crime blob storage containers. Blobs are uploaded to source storage
by the third party scanning supplier.
Before dispatching blobs it verifies if the files were uploaded by third party supplier through non repudiation checks.
It also provides infrastructure to deploy API management service for retrieving SAS tokens used to upload blobs to source
containers.

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/blob-router-service` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `8584` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:8584/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

### Quick Start
An alternative faster way getting started is by using the automated setup script. This script will help set up all
bulk scan/print repos including blob-router-service and its dependencies.
See the [common-dev-env-bsbp](https://github.com/hmcts/common-dev-env-bsbp) repository for more information.
Once set up script has ran successfully you can move the blob-router-service from the newly created
common-dev-env-bsbp/apps directory to your desired location.

## API gateway

Blob Router uses an (Azure API Management) API to protect its SAS token dispensing endpoint.
The API allows only HTTPS requests with approved client certificates and valid subscription keys to reach
the service.

Azure API Management is based on public swagger specs.
As part of creating API in there documentation had to be [published](.github/workflows/publish-openapi.yaml).
The full url to documentation can be found [here](https://github.com/hmcts/cnp-api-docs/blob/master/docs/specs/blob-router-service.json).

If SAS dispensing endpoint has changed in some incompatible way which causes amended specs - the management needs to be notified.
This means tiny alteration in [terraform file](infrastructure/cft-api-mgmt.tf).

In case any new endpoint needs to be included - same treatment must be applied.

### Calling the API

In order to talk to the SAS dispensing endpoint through the API, you need to have the following pieces
of information:

- a certificate whose thumbprint is known to the API (has to be added to the list of allowed thumbprints in `var.allowed_client_certificate_thumbprints` terraform variable)
- a valid subscription key
- name of an existing client service (e.g. `test`)

### Preparing client certificate

First, generate client private key, a certificate for that key and import both into a key store:

```
# generate private key
openssl genrsa 2048 > private.pem

# generate certificate
openssl req -x509 -new -key private.pem -out cert.pem -days 365

# create the key store
# when asked for password, provide one
openssl pkcs12 -export -in cert.pem -inkey private.pem -out cert.pfx -noiter -nomaciter
```

Next, calculate the thumbprint of your certificate:

```
openssl x509 -noout -fingerprint -inform pem -in cert.pem | sed -e s/://g
```

Finally, add this thumbprint to `allowed_client_certificate_thumbprints` terraform variable for the target environment (e.g. in `aat.tfvars` file). Your definition may look similar to this:

```
allowed_client_certificate_thumbprints = ["2FC66765E63BB2436F0F9E4F59E951A6D1D20D43"]
```

Once you're run the deployment, the API will recognise your certificate.

### Retrieving subscription key

You can get the subscription key for the API from the reform-scan-{env} key vault.
The available subscription keys in the key vault are:
  - bulk-scan-team-cft-apim-subscription-key
  - exela-cft-apim-subscription-key (supplier key)
  - iron-mountain-cft-apim-subscription-key (supplier key)


### Getting the token through the API

You can call the API using the following curl command (assuming your current directory contains the private key
and certificate you've created earlier):

```
curl -v --key private.pem --cert cert.pem https://cft-api-mgmt.{env}.platform.hmcts.net/reform-scan/token/{serviceName} -H "Ocp-Apim-Subscription-Key:{subscription key}"
```

You should get a response with status 200 and a token in the body.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
