package net.inet_lab.c4wa.wat;

import java.nio.ByteBuffer;
import java.util.*;

public class ExecutionCtx {
    final static int ONE_PAGE = 65536;

    final Map<String, Func> internalFunc;
    final Set<String> importFunc;
    final Map<String, ExeGlobal> globals;
    byte[] memory;
    int mem_pages;
    final Deque<ExecutionFunc> callStack;

    public ExecutionCtx() {
        importFunc = new HashSet<>();
        internalFunc = new HashMap<>();
        globals = new HashMap<>();
        memory = null;
        mem_pages = 0;
        callStack = new ArrayDeque<>();
    }

    void registerImportFunction(String name) {
        if (importFunc.contains(name) || internalFunc.containsKey(name))
            throw new RuntimeException("Function " + name + " already defined");

        importFunc.add(name);
    }

    void registerInternalFunction(String name, Func wat) {
        if (importFunc.contains(name) || internalFunc.containsKey(name))
            throw new RuntimeException("Function " + name + " already defined");

        internalFunc.put(name, wat);
    }

    Const evalFunctionCall(String name, Const[] evaluated_args) {
        if (!importFunc.contains(name) && !internalFunc.containsKey(name))
            throw new RuntimeException("Function " + name + " is not defined");

        Func wat = internalFunc.get(name);

        if (wat != null) {
            callStack.push(new ExecutionFunc());
            Const result = wat.executeCall(this, evaluated_args);
            callStack.pop();
            return result;
        }
        else if (name.equals("printf")) {
            String fmt = read_str(evaluated_args[0].asInt());
            int offset = evaluated_args[1].asInt();
            List<String> p_fmt = new ArrayList<>();
            List<Object> p_args = new ArrayList<>();
            int i = 0;
            while (i < fmt.length()) {
                p_fmt.add(String.valueOf(fmt.charAt(i)));
                if (fmt.charAt(i) == '%') {
                    if (fmt.charAt(i + 1) == '%') {
                        p_fmt.add("%");
                        i += 2;
                        continue;
                    }
                    int j = i + 1;
                    while (j < fmt.length() && !('a' <= Character.toLowerCase(fmt.charAt(j)) && Character.toLowerCase(fmt.charAt(j)) <= 'z'))
                        j ++;
                    assert j < fmt.length();
                    String f_spec = fmt.substring(i + 1, j);
                    if (Character.toLowerCase(fmt.charAt(j)) == 'l')
                        j ++;
                    assert j < fmt.length();
                    char f_type = Character.toLowerCase(fmt.charAt(j));

                    if ("cdxu".indexOf(f_type) >= 0) {
                        long v = read_i64(offset);
                        if (f_type == 'c')
                            p_args.add((char)v);
                        else if (f_type == 'u') {
                            f_spec = "";
                            f_type = 's';
                            p_args.add(Long.toUnsignedString(v));
                        }
                        else
                            p_args.add(v);
                    }
                    else if ("fe".indexOf(f_type) >= 0)
                        p_args.add(read_f64(offset));
                    else if (f_type == 's') {
                        int a = (int)read_i64(offset);
                        p_args.add(read_str(a));
                    }
                    offset += 8;
                    p_fmt.add(f_spec);
                    p_fmt.add(String.valueOf(f_type));

                    i = j + 1;
                }
                else
                    i ++;
            }

            System.out.printf(String.join("", p_fmt), p_args.toArray());
            return null;
        }
        else
            throw new RuntimeException("Import function " + name + " not implemented yet");
    }

    ExecutionFunc getCurrentFunc() {
        return callStack.peek();
    }

    void registerGlobal(String name, boolean mutable, Const val) {
        if (globals.containsKey(name))
            throw new RuntimeException("Global " + name + " already defined");

        ExeGlobal g = new ExeGlobal(mutable, val);
        globals.put(name, g);
    }

    void assignGlobal(String name, Const val) {
        ExeGlobal g = globals.get(name);
        if (g == null)
            throw new RuntimeException("Global " + name + " is not defined");
        if (!g.mutable)
            throw new RuntimeException("Global " + name + " is not mutable");
        g.val = val;
    }

    Const getGlobal(String name) {
        ExeGlobal g = globals.get(name);
        if (g == null)
            throw new RuntimeException("Global " + name + " is not defined");
        return g.val;
    }

    void initMemory(int pages) {
        if (memory != null)
            throw new RuntimeException("Memory has been initialized already");

        memory = new byte[pages * ONE_PAGE];
        this.mem_pages = pages;
    }

    int memoryGrow(int delta) {
        int new_pages = mem_pages + delta;
        byte[] new_memory = new byte[new_pages * ONE_PAGE];

        System.arraycopy(memory, 0, new_memory, 0, memory.length);

        memory = new_memory;
        mem_pages = new_pages;
        return new_pages;
    }

    void memoryWriteData(byte[] data, int offset) {
        if (memory == null)
            throw new RuntimeException("Memory has not been initialized");
        System.arraycopy(data, 0, memory, offset, data.length);
    }

    void memoryStore(int address, Const val, int wrap) {
        if (val.numType == NumType.F64)
            _memoryStore(address, Double.doubleToRawLongBits(val.doubleValue));
        else if (val.numType == NumType.F32)
            _memoryStore(address, Float.floatToRawIntBits((float)val.doubleValue), 32);
        else if (val.numType == NumType.I64)
            _memoryStore(address, val.longValue);
        else
            _memoryStore(address, val.asInt(), wrap);
    }

    private void _memoryStore(int address, int val, int wrap) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(val);
        byte[] bytes = buffer.array();
        if (wrap >= 8)
            memory[address] = bytes[3];
        if (wrap >= 16)
            memory[address + 1] = bytes[2];
        if (wrap >= 32) {
            memory[address + 2] = bytes[1];
            memory[address + 3] = bytes[0];
        }
    }

    long memoryLoad(int address, int wrap, boolean signed) {
        if (wrap == 8) {
            long res = memory[address];
            return signed? res : res + 256;
        }
        else if (wrap == 16) {
            long res = read_i16(address);
            return signed ? res : res + 65536;
        }
        else if (wrap == 32) {
            long res = read_i32(address);
            return signed ? res : res + 4294967296L;
        }
        else if (wrap == 64) {
            assert signed;
            return read_i64(address);
        }
        else
            throw new RuntimeException("Invalid value wrap = " + wrap);
    }

    void memoryCopy(int dest, int src, int size) {
        System.arraycopy(memory, src, memory, dest, size);
    }

    void memoryFill(int dest, int val, int size) {
        for (int i = 0; i < size; i ++)
            memory[dest + i] = (byte) val;
    }

    private String read_str(int address) {
        int end = address;
        while (memory[end] != 0)
            end ++;
        return new String(Arrays.copyOfRange(memory, address, end));
    }

    private double read_f64(int address) {
        return Double.longBitsToDouble(read_i64(address));
    }

    private long read_i64(int address) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        for (int i = Long.BYTES - 1; i >= 0; i --)
            buffer.put(memory[address + i]);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    private int read_i32(int address) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        for (int i = Integer.BYTES - 1; i >= 0; i --)
            buffer.put(memory[address + i]);
        buffer.flip();//need flip
        return buffer.getInt();
    }

    private int read_i16(int address) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        for (int i = 1; i >= 0; i --)
            buffer.put(memory[address + i]);
        buffer.flip();//need flip
        return buffer.getInt();
    }

    private void _memoryStore(int address, long val) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(val);
        byte[] bytes = buffer.array();
        for (int i = 0; i < Long.BYTES; i ++)
            memory[address + i] = bytes[Long.BYTES - 1 - i];
    }

    private static class ExeGlobal {
        final boolean mutable;
        Const val;
        ExeGlobal(boolean mutable, Const val) {
            this.mutable = mutable;
            this.val = val;
        }
    }
}
