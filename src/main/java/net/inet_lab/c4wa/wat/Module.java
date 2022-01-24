package net.inet_lab.c4wa.wat;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        List<Func.WasmType> types = new ArrayList<>();
        List<WasmOutputStream> imports = new ArrayList<>();
        List<WasmOutputStream> functions = new ArrayList<>();
        List<WasmOutputStream> memories = new ArrayList<>();
        List<WasmOutputStream> globals = new ArrayList<>();
        List<WasmOutputStream> exports = new ArrayList<>();
        List<WasmOutputStream> codes = new ArrayList<>();
        List<WasmOutputStream> datas = new ArrayList<>();

        WasmContext mCtx = new WasmContext();

        int func_idx = 0;
        int type_idx;
        int global_idx = 0;
        for (Instruction i: elements) {
            if (i.type == InstructionName.IMPORT) {
                Import iImport = (Import) i;
                if (iImport.decl != null && iImport.decl.type == InstructionName.FUNC) {
                    Func iFunc = (Func) iImport.decl;
                    Func.WasmType iType = iFunc.wasmSignature();
                    for (type_idx = 0; type_idx < types.size() && !types.get(type_idx).same(iType); type_idx++) ;
                    if (type_idx == types.size())
                        types.add(iType);

                    WasmOutputStream subImport = new WasmOutputStream();
                    subImport.writeString(iImport.importModule);
                    subImport.writeString(iImport.importName);
                    subImport.writeOpcode(Desc.FUNC);
                    subImport.writeUnsignedInt(type_idx);
                    imports.add(subImport);

                    mCtx.funcs.put(iFunc.ref, func_idx);
                    func_idx++;
                }
            }
            else if (i.type == InstructionName.FUNC) {
                Func iFunc = (Func) i;
                Func.WasmType iType = iFunc.wasmSignature();
                for(type_idx = 0; type_idx < types.size() && !types.get(type_idx).same(iType); type_idx ++);
                if (type_idx == types.size())
                    types.add(iType);

                WasmOutputStream subFunc = new WasmOutputStream();
                subFunc.writeUnsignedInt(type_idx);
                functions.add(subFunc);

                Export export = iFunc.getExport();
                if (export != null) {
                    WasmOutputStream subExport = new WasmOutputStream();
                    subExport.writeString(export.exportName);
                    subExport.writeOpcode(Desc.FUNC);
                    subExport.writeUnsignedInt(func_idx);
                    exports.add(subExport);
                }

                mCtx.funcs.put(iFunc.ref, func_idx);
                func_idx++;
            }
            else if (i.type == InstructionName.MEMORY) {
                Memory iMemory = (Memory) i;

                if (iMemory.anImport == null) {
                    WasmOutputStream subMemory = new WasmOutputStream();
                    subMemory.writeOpcode(Limits.MIN_ONLY);
                    subMemory.writeUnsignedInt(iMemory.pages);
                    memories.add(subMemory);
                }
                else {
                    WasmOutputStream subImport = new WasmOutputStream();
                    subImport.writeString(iMemory.anImport.importModule);
                    subImport.writeString(iMemory.anImport.importName);
                    subImport.writeOpcode(Desc.MEM);
                    subImport.writeOpcode(Limits.MIN_ONLY);
                    subImport.writeUnsignedInt(iMemory.pages);
                    imports.add(subImport);
                }
                if (iMemory.export != null) {
                    WasmOutputStream subExport = new WasmOutputStream();
                    subExport.writeString(iMemory.export.exportName);
                    subExport.writeOpcode(Desc.MEM);
                    subExport.writeUnsignedInt(0); // I assume this is index of memory? So always 0?
                    exports.add(subExport);
                }
            }
            else if (i.type == InstructionName.GLOBAL) {
                Global iGlobal = (Global) i;

                if (iGlobal.anImport == null) {
                    WasmOutputStream subGlobal = new WasmOutputStream();
                    subGlobal.writeOpcode(iGlobal.numType);
                    subGlobal.writeOpcode(iGlobal.mutable ? Mutable.MUT : Mutable.CONST);
                    if (iGlobal.initialization != null) {
                        iGlobal.initialization.wasm(mCtx, null, subGlobal);
                        subGlobal.writeOpcode(InstructionName.END);
                    }
                    globals.add(subGlobal);
                }
                else {
                    WasmOutputStream subImport = new WasmOutputStream();
                    subImport.writeString(iGlobal.anImport.importModule);
                    subImport.writeString(iGlobal.anImport.importName);
                    subImport.writeOpcode(Desc.GLOBAL);
                    subImport.writeOpcode(iGlobal.numType);
                    subImport.writeOpcode(iGlobal.mutable ? Mutable.MUT : Mutable.CONST);
                    imports.add(subImport);
                }

                if (iGlobal.export != null) {
                    WasmOutputStream subExport = new WasmOutputStream();
                    subExport.writeString(iGlobal.export.exportName);
                    subExport.writeOpcode(Desc.GLOBAL);
                    subExport.writeUnsignedInt(global_idx);
                    exports.add(subExport);
                }
                mCtx.globals.put(iGlobal.ref, global_idx);
                global_idx++;
            }
            else if (i.type == InstructionName.DATA) {
                Data iData = (Data) i;

                WasmOutputStream subData = new WasmOutputStream();
                subData.writeUnsignedInt(0); // index of data, always 0
                datas.add(subData);
                iData.offset.wasm(mCtx, null, subData);
                subData.writeOpcode(InstructionName.END);
                subData.writeString(iData.data);
            }
        }

        for (Instruction i : elements) {
            if (i.type == InstructionName.FUNC) {
                Func iFunc = (Func) i;
                WasmOutputStream subCode = new WasmOutputStream();
                WasmOutputStream fCode = iFunc.makeWasm(mCtx);
                subCode.writeUnsignedInt(fCode.size());
                subCode.writeSubStream(fCode);
                codes.add(subCode);
            }
        }

        out.writeSection(Section.TYPE, types.stream().map(Func.WasmType::wasm).collect(Collectors.toUnmodifiableList()));
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
