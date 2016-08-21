#!/usr/bin/env bash
set -e

# Do not use getopts here, because this script is sourced by others,
# which very likely will have other argument parameters as this one.
for OPTARG in "$@"; do
	if [[ $OPTARG == "-d" ]]; then
		set -x
	fi
done

# Reset OPTIND because setup.sh may be sourced from other scripts that
# also use getopts
OPTIND=1

# Pretty fancy method to get reliable the absolute path of a shell
# script, *even if it is sourced*. Credits go to GreenFox on
# stackoverflow: http://stackoverflow.com/a/12197518/194894
pushd . > /dev/null
SCRIPTDIR="${BASH_SOURCE[0]}";
while([ -h "${SCRIPTDIR}" ]); do
	cd "`dirname "${SCRIPTDIR}"`"
	SCRIPTDIR="$(readlink "`basename "${SCRIPTDIR}"`")";
done
cd "`dirname "${SCRIPTDIR}"`" > /dev/null
SCRIPTDIR="`pwd`";
popd  > /dev/null

BASEDIR="$(cd ${SCRIPTDIR}/.. && pwd)"

if [[ -f ${BASEDIR}/config ]]; then
	# config is there, source it
	. ${BASEDIR}/config
fi
