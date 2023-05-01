#!/bin/bash

generate_cert() {
    local name=$1
    local cn="$2"
    local opts="$3"

    local keyfile=${name}.key
    local certfile=${name}.crt

    [ -f $keyfile ] || openssl genrsa -out $keyfile 2048
    openssl req \
        -new -sha256 \
        -subj "/O=Spring Boot Test/CN=$cn" \
        -addext "subjectAltName=DNS:example.com,DNS:localhost,DNS:127.0.0.1" \
        -key $keyfile | \
        openssl x509 \
            -req -sha256 \
            -CA test-ca.crt \
            -CAkey test-ca.key \
            -CAserial test-ca.txt \
            -CAcreateserial \
            -days 365 \
            $opts \
            -out $certfile
}

[ -f test-ca.key ] || openssl genrsa -out test-ca.key 4096
openssl req \
    -x509 -new -nodes -sha256 \
    -key test-ca.key \
    -days 3650 \
    -subj '/O=Spring Boot Test/CN=Certificate Authority' \
    -addext "subjectAltName=DNS:example.com,DNS:localhost,DNS:127.0.0.1" \
    -out test-ca.crt

cat > openssl.cnf <<_END_
subjectAltName = @alt_names
[alt_names]
DNS.1 = example.com
DNS.2 = localhost
[ server_cert ]
keyUsage = digitalSignature, keyEncipherment
nsCertType = server
[ client_cert ]
keyUsage = digitalSignature, keyEncipherment
nsCertType = client
_END_

generate_cert test-server "localhost" "-extfile openssl.cnf -extensions server_cert"
generate_cert test-client "localhost" "-extfile openssl.cnf -extensions client_cert"

rm openssl.cnf
rm test-ca.txt