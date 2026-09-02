#!/bin/sh
set -eu

REPOSITORY="ferforastieri/camtacte"
REQUESTED_VERSION="${CAMTACTE_VERSION:-latest}"
INSTALL_ROOT="${CAMTACTE_HOME:-${XDG_DATA_HOME:-${HOME}/.local/share}/camtacte}"
TEMP_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/camtacte.XXXXXX")"

cleanup() {
  rm -rf -- "$TEMP_ROOT"
}
trap cleanup EXIT HUP INT TERM

fail() {
  printf 'Camtacte: %s\n' "$1" >&2
  exit 1
}

command -v curl >/dev/null 2>&1 || fail "curl não foi encontrado."
command -v docker >/dev/null 2>&1 || fail "Docker não foi encontrado. Instale-o antes de continuar."
docker compose version >/dev/null 2>&1 || fail "Docker Compose v2 não foi encontrado."
command -v sha256sum >/dev/null 2>&1 || fail "sha256sum não foi encontrado."

if [ "$REQUESTED_VERSION" = "latest" ]; then
  RELEASE_URL="https://github.com/${REPOSITORY}/releases/latest/download"
else
  RELEASE_URL="https://github.com/${REPOSITORY}/releases/download/${REQUESTED_VERSION}"
fi

printf 'Camtacte: baixando a release %s...\n' "$REQUESTED_VERSION"
for asset in compose.yaml mediamtx.yml VERSION SHA256SUMS; do
  curl -fsSL --retry 3 --proto '=https' --tlsv1.2 \
    "${RELEASE_URL}/${asset}" -o "${TEMP_ROOT}/${asset}" || fail "não foi possível baixar ${asset}."
done

verify_asset() {
  asset="$1"
  expected="$(awk -v file="$asset" '$2 == file || $2 == "*" file { print $1; exit }' "${TEMP_ROOT}/SHA256SUMS")"
  [ -n "$expected" ] || fail "checksum ausente para ${asset}."
  actual="$(sha256sum "${TEMP_ROOT}/${asset}" | awk '{ print $1 }')"
  [ "$actual" = "$expected" ] || fail "checksum inválido para ${asset}."
}

verify_asset compose.yaml
verify_asset mediamtx.yml
verify_asset VERSION

RELEASE_VERSION="$(tr -d '\r\n' < "${TEMP_ROOT}/VERSION")"
[ -n "$RELEASE_VERSION" ] || fail "a release não informou uma versão."
case "$RELEASE_VERSION" in
  *[!A-Za-z0-9._-]*) fail "versão de release inválida." ;;
esac

mkdir -p "$INSTALL_ROOT"
for asset in compose.yaml mediamtx.yml; do
  if [ -f "${INSTALL_ROOT}/${asset}" ]; then
    cp -p "${INSTALL_ROOT}/${asset}" "${INSTALL_ROOT}/${asset}.previous"
  fi
  mv "${TEMP_ROOT}/${asset}" "${INSTALL_ROOT}/${asset}"
done

LAN_ADDRESS="$(hostname -I 2>/dev/null | awk '{ print $1 }')"
[ -n "$LAN_ADDRESS" ] || LAN_ADDRESS="localhost"
ENV_FILE="${INSTALL_ROOT}/.env"
if [ ! -f "$ENV_FILE" ]; then
  {
    printf 'CAMTACTE_VERSION=%s\n' "$RELEASE_VERSION"
    printf 'CAMTACTE_PORT=8443\n'
    printf 'CAMTACTE_PUBLIC_URL=https://%s:8443\n' "$LAN_ADDRESS"
  } > "$ENV_FILE"
else
  if grep -q '^CAMTACTE_VERSION=' "$ENV_FILE"; then
    sed "s/^CAMTACTE_VERSION=.*/CAMTACTE_VERSION=${RELEASE_VERSION}/" "$ENV_FILE" > "${TEMP_ROOT}/env"
    mv "${TEMP_ROOT}/env" "$ENV_FILE"
  else
    printf '\nCAMTACTE_VERSION=%s\n' "$RELEASE_VERSION" >> "$ENV_FILE"
  fi
fi
chmod 600 "$ENV_FILE"

printf 'Camtacte: iniciando os serviços em %s...\n' "$INSTALL_ROOT"
docker compose --project-directory "$INSTALL_ROOT" --env-file "$ENV_FILE" pull
docker compose --project-directory "$INSTALL_ROOT" --env-file "$ENV_FILE" up -d --remove-orphans

printf '\nCamtacte %s está em https://%s:8443\n' "$RELEASE_VERSION" "$LAN_ADDRESS"
printf 'Veja o código de pareamento com:\n  docker compose --project-directory %s logs camtacte\n' "$INSTALL_ROOT"
