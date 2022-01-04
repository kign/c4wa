package net.inet_lab.c4wa.wat;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Module extends Instruction_list {
    enum Section implements WasmOutputStream.Opcode {
        TYPE (0x01),
        IMPORT (0x02),
        FUNCTION (0x03),
        MEMORY (0x05),
        GLOBAL (0x06),
        EXPORT (0x07),
        CODE (0x0A),
        DATA (0x0B);

        final byte opcode;
        Section(int opcode) {
            this.opcode = (byte) opcode;
        }

        @Override
        public byte opcode() {
            return opcode;
        }
    }

    enum Desc implements WasmOutputStream.Opcode {
        FUNC (0x00),
        TABLE (0x01),
        MEM (0x02),
        GLOBAL (0x03);

        final byte opcode;

        Desc(int opcode) {
            this.opcode = (byte) opcode;
        }

        @Override
        public byte opcode() {
            return opcode;
        }
    }

    enum Limits implements WasmOutputStream.Opcode {
        MIN_ONLY (0x00),
        MIN_AND_MAX (0x01);

        final byte opcode;

        Limits(int opcode) {
            this.opcode = (byte) opcode;
        }

        @Override
        public byte opcode() {
            return opcode;
        }
    }

    enum Mutable implements WasmOutputStream.Opcode {
        CONST(0x00),
        MUT (0x01);

        final byte opcode;

        Mutable(int opcode) {
            this.opcode = (byte) opcode;
        }

        @Override
        public byte opcode() {
            return opcode;
        }
    }

    public Module(List<Instruction> elements) {
        super(InstructionName.MODULE, elements.toArray(Instruction[]::new), true);
    }

    public void wasm(WasmOutputStream out) throws IOException {
        out.writeDirect(new byte[]{0x00, 'a', 's', 'm'});
        out.writeDirect(new byte[]{0x01, 0x00, 0x00, 0x00});

        List<WasmOutputStream> types = new ArrayList<>();
        List<WasmOutputStream> imports = new ArrayList<>();
        List<WasmOutputStream> functions = new ArrayList<>();
        List<WasmOutputStream> memories = new ArrayList<>();
        List<WasmOutputStream> globals = new ArrayList<>();
        List<WasmOutputStream> exports = new ArrayList<>();
        List<WasmOutputStream> codes = new ArrayList<>();
        List<WasmOutputStream> datas = new ArrayList<>();

        WasmContext mCtx = new WasmContext();

        int type_idx = 0;
        for (Instruction i: elements) {
            if (i.type == InstructionName.IMPORT) {
                Import iImport = (Import) i;
                if (iImport.decl != null && iImport.decl.type == InstructionName.FUNC) {
                    Func iFunc = (Func) iImport.decl;
                    types.add(iFunc.wasmSignature());
                    type_idx++;

                    WasmOutputStream subImport = new WasmOutputStream();
                    subImport.writeString(iImport.importModule);
                    subImport.writeString(iImport.importName);
                    subImport.writeOpcode(Desc.FUNC);
                    subImport.writeInt(type_idx - 1);
                    imports.add(subImport);

                    mCtx.funcs.put(iFunc.getName(), type_idx - 1);
                }
            }
            else if (i.type == InstructionName.FUNC) {
                Func iFunc = (Func) i;
                types.add(iFunc.wasmSignature());
                type_idx++;

                WasmOutputStream subFunc = new WasmOutputStream();
                subFunc.writeInt(type_idx - 1);
                functions.add(subFunc);

                Export export = iFunc.getExport();
                if (export != null) {
                    WasmOutputStream subExport = new WasmOutputStream();
                    subExport.writeString(export.exportName);
                    subExport.writeOpcode(Desc.FUNC);
                    subExport.writeInt(type_idx - 1);
                    exports.add(subExport);
                }

                WasmOutputStream subCode = new WasmOutputStream();
                WasmOutputStream fCode = iFunc.makeWasm(mCtx);
                subCode.writeInt(fCode.size());
                subCode.writeSubStream(fCode);
                codes.add(subCode);

                mCtx.funcs.put(iFunc.getName(), type_idx - 1);
            }
            else if (i.type == InstructionName.MEMORY) {
                Memory iMemory = (Memory) i;

                WasmOutputStream subMemory = new WasmOutputStream();
                subMemory.writeOpcode(Limits.MIN_ONLY);
                subMemory.writeInt(iMemory.pages);
                memories.add(subMemory);

                if (iMemory.anImport != null) {
                    WasmOutputStream subImport = new WasmOutputStream();
                    subImport.writeString(iMemory.anImport.importModule);
                    subImport.writeString(iMemory.anImport.importName);
                    subImport.writeOpcode(Desc.MEM);
                    imports.add(subImport);
                }
                if (iMemory.export != null) {
                    WasmOutputStream subExport = new WasmOutputStream();
                    subExport.writeString(iMemory.export.exportName);
                    subExport.writeOpcode(Desc.MEM);
                    subExport.writeInt(0); // I assume this is index of memory? So always 0?
                    exports.add(subExport);
                }
            }
            else if (i.type == InstructionName.GLOBAL) {
                Global iGlobal = (Global) i;
                WasmOutputStream subGlobal = new WasmOutputStream();
                subGlobal.writeOpcode(iGlobal.numType);
                subGlobal.writeOpcode(iGlobal.mutable ? Mutable.MUT : Mutable.CONST);
                if (iGlobal.initialization != null) {
                    iGlobal.initialization.wasm(mCtx, null, subGlobal);
                    subGlobal.writeOpcode(InstructionName.END);
                }
                mCtx.globals.put(iGlobal.ref, globals.size());
                globals.add(subGlobal);

                if (iGlobal.anImport != null) {
                    WasmOutputStream subImport = new WasmOutputStream();
                    subImport.writeString(iGlobal.anImport.importModule);
                    subImport.writeString(iGlobal.anImport.importName);
                    subImport.writeOpcode(Desc.GLOBAL);
                    imports.add(subImport);
                }
                if (iGlobal.export != null) {
                    WasmOutputStream subExport = new WasmOutputStream();
                    subExport.writeString(iGlobal.export.exportName);
                    subExport.writeOpcode(Desc.GLOBAL);
                    exports.add(subExport);
                }
            }
            else if (i.type == InstructionName.DATA) {
                Data iData = (Data) i;

                WasmOutputStream subData = new WasmOutputStream();
                subData.writeInt(0); // index of data, always 0
                datas.add(subData);
                iData.offset.wasm(mCtx, null, subData);
                subData.writeOpcode(InstructionName.END);
                subData.writeString(iData.data);
            }
        }

        out.writeSection(Section.TYPE, types);
        out.writeSection(Section.IMPORT, imports);
        out.writeSection(Section.FUNCTION, functions);
        out.writeSection(Section.MEMORY, memories);
        out.writeSection(Section.GLOBAL, globals);
        out.writeSection(Section.EXPORT, exports);
        out.writeSection(Section.CODE, codes);
        out.writeSection(Section.DATA, datas);
    }

    static class WasmContext {
        final Map<String, Integer> globals;
        final Map<String, Integer> funcs;

        WasmContext() {
            this.globals = new HashMap<>();
            this.funcs = new HashMap<>();
        }
    }
}
