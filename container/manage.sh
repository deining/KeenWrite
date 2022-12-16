#!/usr/bin/env bash

# ---------------------------------------------------------------------------
# Copyright 2022 White Magic Software, Ltd.
#
# This script helps manage a container. The container is configured to
# run ConTeXt.
# ---------------------------------------------------------------------------

source ../scripts/build-template

readonly BUILD_DIR=build

readonly CONTAINER_EXE=podman
readonly CONTAINER_NAME=typesetter
readonly CONTAINER_NETWORK=host
readonly CONTAINER_FILE="${CONTAINER_NAME}"
readonly CONTAINER_ARCHIVE_FILE="${CONTAINER_FILE}.tar"
readonly CONTAINER_ARCHIVE_PATH="${BUILD_DIR}/${CONTAINER_ARCHIVE_FILE}"
readonly CONTAINER_COMPRESSED_FILE="${CONTAINER_ARCHIVE_FILE}.gz"
readonly CONTAINER_COMPRESSED_PATH="${BUILD_DIR}/${CONTAINER_ARCHIVE_FILE}.gz"
readonly CONTAINER_DIR_TEXT="/root/text"
readonly CONTAINER_DIR_IMAGES="/root/images"
readonly CONTAINER_DIR_OUTPUT="/root/output"

ARG_CONTAINER_COMMAND="context --version"
ARG_MOUNTPOINT_TEXT=""
ARG_MOUNTPOINT_IMAGES=""
ARG_MOUNTPOINT_OUTPUT="."

DEPENDENCIES=(
  "podman,https://podman.io"
  "tar,https://www.gnu.org/software/tar"
  "bzip2,https://gitlab.com/bzip2/bzip2"
)

ARGUMENTS+=(
  "b,build,Build container (${CONTAINER_NAME})"
  "c,connect,Connect to container"
  "d,delete,Remove all containers"
  "i,images,Set mount point for image files (to typeset)"
  "l,load,Load container (${CONTAINER_COMPRESSED_PATH})"
  "o,output,Set mount point for output files (after typesetting)"
  "r,run,Run a command in the container (\"${ARG_CONTAINER_COMMAND}\")"
  "s,save,Save container (${CONTAINER_COMPRESSED_PATH})"
  "t,text,Set mount point for text file (to typeset)"
)

# ---------------------------------------------------------------------------
# Manages the container.
# ---------------------------------------------------------------------------
execute() {
  $do_delete
  $do_build
  $do_save
  $do_load
  $do_execute
  $do_connect

  return 1
}

# ---------------------------------------------------------------------------
# Deletes all containers.
# ---------------------------------------------------------------------------
utile_delete() {
  $log "Deleting all containers"

  ${CONTAINER_EXE} rmi --all --force > /dev/null

  $log "Containers deleted"
}

# ---------------------------------------------------------------------------
# Builds the container file in the current working directory.
# ---------------------------------------------------------------------------
utile_build() {
  # Show what commands are run while building, but not the commands' output.
  ${CONTAINER_EXE} build \
    --network=${CONTAINER_NETWORK} \
    --tag ${CONTAINER_NAME} . | \
  grep ^STEP
}

# ---------------------------------------------------------------------------
# Creates the command-line option for a read-only mountpoint.
#
# $1 - The host directory.
# $2 - The guest (container) directory.
# $3 - The file system permissions (set to 1 for read-write).
# ---------------------------------------------------------------------------
get_mountpoint() {
  local result=""

  local binding="ro"

  if [ ! -z "${3+x}" ]; then
    binding="Z"
  fi

  if [ ! -z "${1}" ]; then
    result="-v ${1}:${2}:${binding}"
  fi

  echo "${result}"
}

get_mountpoint_text() {
  echo $(get_mountpoint "${ARG_MOUNTPOINT_TEXT}" "${CONTAINER_DIR_TEXT}")
}

get_mountpoint_images() {
  echo $(get_mountpoint "${ARG_MOUNTPOINT_IMAGES}" "${CONTAINER_DIR_IMAGES}")
}

get_mountpoint_output() {
  echo $(get_mountpoint "${ARG_MOUNTPOINT_OUTPUT}" "${CONTAINER_DIR_OUTPUT}" 1)
}

# ---------------------------------------------------------------------------
# Connects to the container.
# ---------------------------------------------------------------------------
utile_connect() {
  $log "Connecting to container"

  declare -r mount_text=$(get_mountpoint_text)
  declare -r mount_images=$(get_mountpoint_images)
  declare -r mount_output=$(get_mountpoint_output)

  ${CONTAINER_EXE} run \
    --network="${CONTAINER_NETWORK}" \
    --rm \
    -it \
    ${mount_text} \
    ${mount_images} \
    ${mount_output} \
    "${CONTAINER_NAME}"
}

# ---------------------------------------------------------------------------
# Runs a command in the container.
#
# Examples:
#
#   ./manage.sh -r "ls /"
#   ./manage.sh -r "context --version"
# ---------------------------------------------------------------------------
utile_execute() {
  $log "Run \"${ARG_CONTAINER_COMMAND}\":"

  ${CONTAINER_EXE} run \
    --network="${CONTAINER_NETWORK}" \
    --rm \
    -i \
    -t "${CONTAINER_NAME}" \
    /bin/sh --login -c "${ARG_CONTAINER_COMMAND}"
}

# ---------------------------------------------------------------------------
# Saves the container to a file.
# ---------------------------------------------------------------------------
utile_save() {
  if [[ -f "${CONTAINER_COMPRESSED_PATH}" ]]; then
    warning "${CONTAINER_COMPRESSED_PATH} exists, delete before saving."
  else
    $log "Saving ${CONTAINER_NAME} image"

    mkdir -p "${BUILD_DIR}"

    ${CONTAINER_EXE} save \
      --quiet \
      -o "${BUILD_DIR}/${CONTAINER_ARCHIVE_FILE}" \
      "${CONTAINER_NAME}"

    $log "Compressing to ${CONTAINER_COMPRESSED_PATH}"
    gzip "${CONTAINER_ARCHIVE_PATH}"

    $log "Saved ${CONTAINER_NAME} image"
  fi
}

# ---------------------------------------------------------------------------
# Loads the container from a file.
# ---------------------------------------------------------------------------
utile_load() {
  if [[ -f "${CONTAINER_COMPRESSED_PATH}" ]]; then
    $log "Loading ${CONTAINER_NAME} image from ${CONTAINER_COMPRESSED_PATH}"

    ${CONTAINER_EXE} load \
      --quiet \
      -i "${CONTAINER_COMPRESSED_PATH}"

    $log "Loaded ${CONTAINER_NAME} image"
  else
    warning "Missing ${CONTAINER_COMPRESSED_PATH}; use build follwed by save"
  fi
}

argument() {
  local consume=1

  case "$1" in
    -b|--build)
    do_build=utile_build
    ;;
    -c|--connect)
    do_connect=utile_connect
    ;;
    -d|--delete)
    do_delete=utile_delete
    ;;
    -l|--load)
    do_load=utile_load
    ;;
    -i|--images)
    if [ ! -z "${2+x}" ]; then
      ARG_MOUNTPOINT_IMAGES="$2"
      consume=2
    fi
    ;;
    -o|--output)
    if [ ! -z "${2+x}" ]; then
      ARG_MOUNTPOINT_OUTPUT="$2"
      consume=2
    fi
    ;;
    -r|--run)
    do_execute=utile_execute

    if [ ! -z "${2+x}" ]; then
      ARG_CONTAINER_COMMAND="$2"
      consume=2
    fi
    ;;
    -s|--save)
    do_save=utile_save
    ;;
    -t|--text)
    if [ ! -z "${2+x}" ]; then
      ARG_MOUNTPOINT_TEXT="$2"
      consume=2
    fi
    ;;
  esac

  return ${consume}
}

do_build=:
do_connect=:
do_delete=:
do_load=:
do_execute=:
do_save=:

main "$@"

