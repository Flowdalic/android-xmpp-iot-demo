#!/usr/bin/env bash

# shellcheck source=setup.sh disable=SC1091
. "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/setup.sh"

set -e

PUBLISH=false
REMOTE=false

while getopts dhprt: OPTION "$@"; do
	case $OPTION in
		d)
			set -x
			;;
		h)
			cat <<EOF
usage: $(basename "$0") [-d] [-p] [-r] [-t <tag>]
	-d: debug output
	-p: publish
	-r: get keystore data from remote location
EOF
			exit
			;;
		p)
			PUBLISH=true
			;;
		r)
			REMOTE=true
			;;
	esac
done

declare -r KEYSTORE_PROPERTIES_FILE="$BASEDIR/keystore.properties"

if $REMOTE; then
	# shellcheck disable=SC2064
	trap "rm \"$KEYSTORE_PROPERTIES_FILE\"" EXIT
fi

if [[ ! -f "$KEYSTORE_PROPERTIES_FILE" ]]; then
	if ! $REMOTE; then
		KEYSTOREFILE=${KEYSTOREDATA}/release.keystore
		KEYSTOREPASSGPG=${KEYSTOREDATA}/keystore_password.gpg

		if [[ ! -d ${KEYSTOREDATA} ]]; then
			echo "KEYSTOREDATA=${KEYSTOREDATA} does not exist or is not a directory"
			exit 1
		fi

		if [[ ! -f ${KEYSTOREFILE} ]]; then
			echo "KEYSTOREFILE=${KEYSTOREFILE} does not exist or is not a file"
			exit 1
		fi

		if [[ ! -f ${KEYSTOREPASSGPG} ]]; then
			echo "KEYSTOREPASSGPG=${KEYSTOREPASSGPG} does not exist or is not a file"
			exit 1
		fi
		# shellcheck disable=SC2002,SC2086
		KEYSTOREPASSWORD=$(cat ${KEYSTOREPASSGPG} | gpg -d)
	elif $REMOTE; then
		if [[ -z $KEYSTOREPASSWORD ]]; then
			echo "error: \$KEYSTOREPASSWORD not set"
			exit 1
		fi
		if [[ -z $KEYSTOREURL ]]; then
			echo "error: \$KEYSTORERUL not set"
			exit 1
		fi
		KEYSTOREFILE=$TMPDIR/release.keystore
		wget -q -O "$KEYSTOREFILE" "$KEYSTOREURL" 2>&1
	fi

	cat <<EOF > "$KEYSTORE_PROPERTIES_FILE"
storeFile=${KEYSTOREFILE}
keyAlias=xiot
storePassword=${KEYSTOREPASSWORD}
keyPassword=${KEYSTOREPASSWORD}
EOF

fi

cd "$BASEDIR"

./gradlew --stacktrace app:assembleRelease

#ANT_ARGS="-propertyfile ${TMPDIR}/ant.properties" make parrelease

BUILT_DATE=$(date +"%Y-%m-%d_-_%H:%M_%Z")

if $REMOTE; then
	TARGET_DIR=$BUILT_DATE
else
	TARGET_DIR=""
fi

declare -r LOCAL_RELEASE_DIR=releases/"$TARGET_DIR"

[[ -d "$LOCAL_RELEASE_DIR" ]] || mkdir -p "$LOCAL_RELEASE_DIR"

cp app/build/outputs/apk/app-release.apk "$LOCAL_RELEASE_DIR"

if $PUBLISH; then
	BRANCH=$(git rev-parse --abbrev-ref HEAD)
	# If we are building from a non-master branch, then add the branch
	# name to the target directory.
	if [[ "$BRANCH" != "master" ]]; then
		SUBDIR=$BRANCH
		TARGET_DIR="${BRANCH}/"
	else
		SUBDIR=""
		TARGET_DIR=""
	fi
	TARGET_DIR+=$BUILT_DATE
	cat <<EOF | sftp $RELEASE_HOST
mkdir ${RELEASE_DIR}/nightlies/${SUBDIR}
mkdir ${RELEASE_DIR}/nightlies/${TARGET_DIR}
put ${LOCAL_RELEASE_DIR}/*.apk ${RELEASE_DIR}/nightlies/${TARGET_DIR}
EOF

fi
