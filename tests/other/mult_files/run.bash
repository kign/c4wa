#!/bin/bash -u

unset CDPATH
cd "$(dirname "$0")" || exit 1

(cd ../../..; ./gradlew installDist)

../../../build/install/c4wa-compile/bin/c4wa-compile -k part_1.c part_2.c
wat2wasm bundle.wat -o /tmp/cmp_bundle.wasm
diff /tmp/cmp_bundle.wasm bundle.wasm
if [ $? -ne 0 ]; then
  echo "FAILED"
  exit 1
fi
../../../etc/run-tests bundle.wasm

echo
echo "OK!"