
c4wa-compile = ../../../build/install/c4wa-compile/bin/c4wa-compile

C4WA_OPTS = -Xmodule.memoryStatus=import:js1
# C4WA_OPTS =
# C4WA_OPTS = -Xmodule.memoryStatus=internal
# C4WA_OPTS = -Xmodule.memoryStatus=none

test: compiler diff

compiler:
	(cd ../../.. && ./gradlew installDist)

im_ex.wat:
	$(c4wa-compile) $(C4WA_OPTS) im_ex.c -o $@

im_ex.wasm:
	$(c4wa-compile) $(C4WA_OPTS) im_ex.c -o $@

im_ex_1.wasm: im_ex.wat
	wat2wasm im_ex.wat -o $@

diff: im_ex.wasm im_ex_1.wasm
	diff im_ex.wasm im_ex_1.wasm

clean:
	rm -f im_ex.wasm im_ex_1.wasm im_ex.wat

