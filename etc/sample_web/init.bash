#!/bin/bash

LIGHT_CYAN='\033[1;36m'
LIGHT_RED='\033[1;31m'
LIGHT_GREEN='\033[1;32m'
NC='\033[0m' # No Color

unset CDPATH
cd "$(dirname "$0")" || exit 1

printf "\n${LIGHT_CYAN}Checking requirements${NC}\n"

req=(python3 npm)
nf=0
for r in ${req[@]}; do
  res="${LIGHT_GREEN}ok${NC}"
  type $r >/dev/null 2>&1 || { res="${LIGHT_RED}not found${NC}"; nf=$((nf+1)); }
  printf "%-10s ${res}\n" $r
done

echo ""
if [ $nf -gt 0 ]; then
  echo "Some requirements not satisfied, exiting"
  exit 1
else
  echo "All requirements are satisfied"
fi



printf "\n${LIGHT_CYAN}Looking for c4wa compiler${NC}\n"
compile="c4wa-compile"
if ! type $compile >/dev/null 2>&1; then
  compile=../../build/install/c4wa-compile/bin/c4wa-compile
  if [ ! -x $compile ]; then
    (cd ../.. && /gradlew installDist)
  fi
fi

echo "Found: $compile"

printf "\n${LIGHT_CYAN}Generating WAT files${NC}\n"

for x in *.c; do
  echo $compile $x -lmm_incr
  $compile $x -lmm_incr
done

printf "\n${LIGHT_CYAN}Generating WASM files${NC}\n"

for x in *.wat; do
  echo wat2wasm $x
  wat2wasm $x
done

printf "\n${LIGHT_CYAN}Setting up virtual environment${NC}\n"
py=venv/bin/python3
if [ -d venv ]; then
  echo "Directory venv exists, skipping"
else
  echo python3 -m venv venv
  python3 -m venv venv
  $py -m pip install --upgrade pip
  $py -m pip install inetlab flask
fi

printf "\n${LIGHT_CYAN}Setting up node modules${NC}\n"
if [ -d node_modules ]; then
  echo "Directory node_modules exists, skipping"
else
  echo npm i
  npm i
fi

printf "\n${LIGHT_CYAN}Make JS bundle${NC}\n"
echo node_modules/.bin/browserify main.js -o bundle.js
node_modules/.bin/browserify main.js -o bundle.js

printf "\n${LIGHT_CYAN}Launch the server${NC}\n"
echo FLASK_ENV=development $py run-server.py
FLASK_ENV=development $py run-server.py