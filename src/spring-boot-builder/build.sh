#!/usr/bin/env bash
set -x -eo pipefail

docker pull paketobuildpacks/build-jammy-tiny
docker tag paketobuildpacks/build-jammy-tiny ghcr.io/spring-projects/build-jammy-tiny:latest
docker push ghcr.io/spring-projects/build-jammy-tiny:latest

docker pull paketobuildpacks/run-jammy-tiny
docker tag paketobuildpacks/run-jammy-tiny ghcr.io/spring-projects/run-jammy-tiny:latest
docker push ghcr.io/spring-projects/run-jammy-tiny:latest

cd buildpacks
pack buildpack package ghcr.io/spring-projects/spring-boot-test-info --config ./package.toml
docker push ghcr.io/spring-projects/spring-boot-test-info:latest
cd -

cd builder
pack builder create ghcr.io/spring-projects/spring-boot-cnb-builder:0.0.3 --config builder.toml
docker push ghcr.io/spring-projects/spring-boot-cnb-builder:0.0.3
cd -
