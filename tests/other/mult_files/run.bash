#!/bin/bash -u

unset CDPATH
cd "$(dirname "$0")" || exit 1

(cd ../../..; ./gradlew installDist)

../../../build/install/c4wa-compile/bin/c4wa-compile part_1.c part_2.c
wat2wasm bundle.wat
../../../etc/run-tests bundle.wasm

echo
echo "OK!"