Some openssl commands related to sha256withrsa signatures:
- Create rsa private key:

        openssl genrsa -out private_key.pem 1024

- Generate DER format private key from PEM:

        openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem -out private_key.der -nocrypt

- Generate DER format public key from PEM private key:

        openssl rsa -in private_key.pem -pubout -outform DER -out public_key.der

- Generate DER format public key from PEM public key:

        openssl rsa -pubin -inform PEM -outform DER -in public_key.pem

- Generate signature for file:

        openssl dgst -sha256 -sign private_key.pem -out signature envelope.zip

- Verify file signature:

        openssl dgst -sha256 -verify public_key.pem -signature signature envelope.zip
