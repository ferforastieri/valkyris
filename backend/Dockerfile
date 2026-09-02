FROM golang:1.26.5-bookworm AS build
WORKDIR /src/backend
COPY backend/go.mod backend/go.sum* ./
RUN go mod download
COPY backend/ ./
ARG BUILD_VERSION=dev
RUN CGO_ENABLED=1 GOOS=linux go build -trimpath -ldflags="-s -w -X main.version=${BUILD_VERSION}" -o /out/camtacte ./cmd/camtacte
RUN mkdir -p /out/lib && cp /go/pkg/mod/github.com/k2-fsa/sherpa-onnx-go-linux@*/lib/$(uname -m)-unknown-linux-gnu/*.so /out/lib/
ADD --checksum=sha256:07e2fafcdcbc461f2816188d9b0bbafced12584030cf67d5652e549ef256a2c6 https://github.com/k2-fsa/sherpa-onnx/releases/download/audio-tagging-models/sherpa-onnx-zipformer-small-audio-tagging-2024-04-15.tar.bz2 /tmp/audio-model.tar.bz2
RUN apt-get update && apt-get install -y --no-install-recommends bzip2 && mkdir -p /out/models && tar -xjf /tmp/audio-model.tar.bz2 --strip-components=1 -C /out/models && rm -rf /var/lib/apt/lists/* /tmp/audio-model.tar.bz2 /out/models/test_wavs /out/models/model.onnx

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates ffmpeg openssl wget && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 10001 --home /data camtacte
RUN install -d -o camtacte -g camtacte -m 0700 /data
COPY --from=build /out/camtacte /usr/local/bin/camtacte
COPY --from=build /out/lib /usr/local/lib
COPY --from=build /out/models /opt/camtacte/models
ENV LD_LIBRARY_PATH=/usr/local/lib
ENV CAMTACTE_MODELS_DIR=/opt/camtacte/models
ENV CAMTACTE_DATA_DIR=/data
ENV CAMTACTE_DATABASE=/data/camtacte.db
ENV CAMTACTE_TLS_CERT=/data/tls/server.crt
ENV CAMTACTE_TLS_KEY=/data/tls/server.key
ENV CAMTACTE_MASTER_KEY_FILE=/data/secrets/master.key
ENV CAMTACTE_MEDIA_RECORDINGS=/data/recordings
VOLUME ["/data"]
EXPOSE 8443
USER camtacte
ENTRYPOINT ["/usr/local/bin/camtacte"]
