### Test certificates

[unrecognised-client-certificate.pfx](unrecognised-client-certificate.jks) is a Java key store containing a test
SSL client certificate (and the corresponding private key) that should not be recognised by the API (gateway).
The purpose of this key store is to be used in tests, in order to verify that HTTPS requests with this certificate are rejected.

Here's how it was created:

```bash
openssl genrsa 2048 > private.pem
openssl req -x509 -new -key private.pem -out cert.pem -days 36500
openssl pkcs12 -export -in cert.pem -inkey private.pem -out unrecognised-client-certificate.pfx -noiter -nomaciter
```

To convert to Java key store:

```bash
keytool -importkeystore -srckeystore unrecognised-client-certificate.pfx -srcstoretype pkcs12 -destkeystore unrecognised-client-certificate.jks -deststoretype pkcs12
```

The key store was created with password `testcert`.

#### Note

Only valid certificates are stored in key vault.
As inspected in test suite value is base64 encoded.
Here is the command line which achieved desired outcome:

```bash
openssl base64 -in <infile> -out <outfile>
```

- `<infile>` would be certificate, e.g. `valid-certificate.jks`
- `<outfile>` - anything. Only need its contents to be stored
