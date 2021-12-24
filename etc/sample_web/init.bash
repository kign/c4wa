#!/bin/bash

LIGHT_CYAN='\033[1;36m'
CYAN='\033[0;36m'
LIGHT_RED='\033[1;31m'
LIGHT_GREEN='\033[1;32m'
NC='\033[0m' # No Color

unset CDPATH
cd "$(dirname "$0")" || exit 1

if [ "$1" == "clean" ]; then
  echo rm -rf node_modules *.wat *.wasm package-lock.json Makefile bundle.js index.html
  rm -rf node_modules *.wat *.wasm package-lock.json Makefile bundle.js index.html
  exit
fi

printf "\n${LIGHT_CYAN}Checking requirements${NC}\n"

nf=0
compile="c4wa-compile"
req=(npm make "$compile")
for r in "${req[@]}"; do
  res="${LIGHT_GREEN}ok${NC}"
  ok=1
  type "$r" >/dev/null 2>&1 || { ok=0; res="${LIGHT_RED}not found${NC}"; nf=$((nf+1)); }
  if [ "$r" == "c4wa-compile" ] && [ $ok -eq 0 ]; then
    compile=../../build/install/c4wa-compile/bin/c4wa-compile
    if [ ! -x $compile ] && [ -x ./gradlew ]; then
      (cd ../.. && ./gradlew installDist)
    fi
    if [ -x $compile ]; then
      res="${CYAN}$compile${NC}"
      nf=$((nf-1))
    fi
  fi
  printf "%-12s ${res}\n" "$r"
done

echo ""
if [ $nf -gt 0 ]; then
  echo "Some requirements not satisfied, exiting"
  exit 1
else
  echo "All requirements are satisfied"
fi

printf "\n${LIGHT_CYAN}Installing node modules${NC}\n"
if [ -d node_modules ]; then
  echo "Directory node_modules exists, skipping"
else
  echo npm i
  npm i
fi

browserify="node_modules/.bin/browserify"
printf "\n${LIGHT_CYAN}Generating Makefile${NC}\n"
if [ -e Makefile ]; then
  echo "Makefile already exists, skipping"
else
  rm -f Makefile
  TAB="$(printf '\t')"
  for x in *.c; do
    name="${x%.*}"
    cat << EOF >> Makefile
all: $name.wasm bundle.js
$name.wat: $name.c
$TAB$compile $name.c -lmm_incr
$name.wasm: $name.wat
${TAB}wat2wasm $name.wat
bundle.js: main.js ../wasm-printf.js
${TAB}$browserify main.js -o bundle.js
EOF
  done
  cat Makefile
fi

printf "\n${LIGHT_CYAN}Generating files${NC}\n"
echo make
make

printf "\n${LIGHT_CYAN}Launching server${NC}\n"
port=9811
for x in *.html; do
  ln -sf "$x" index.html
  break
done
(sleep 1 && open "http://localhost:$port") &
node_modules/.bin/light-server -s . -p $port -w "*.js,*.c,*.html,*.css # make"