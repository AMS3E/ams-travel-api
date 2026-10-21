# MinIO built from its source at a pinned release, because MinIO no longer publishes binaries or images
# for the community edition (its download address answers 410, the registries carry no tags). The first
# build compiles it, a few minutes; Docker keeps the result. Works on Intel and Apple Silicon alike.
FROM golang:1.25-bookworm AS build
ARG MINIO_VERSION=RELEASE.2025-10-15T17-29-55Z
ENV CGO_ENABLED=0 GOFLAGS=-trimpath
RUN git clone --depth 1 --branch ${MINIO_VERSION} https://github.com/minio/minio.git /src \
    && cd /src && go build -ldflags "-s -w" -o /minio .

FROM busybox:stable
COPY --from=build /minio /usr/local/bin/minio
VOLUME /data
EXPOSE 9000 9001
ENTRYPOINT ["/usr/local/bin/minio"]
