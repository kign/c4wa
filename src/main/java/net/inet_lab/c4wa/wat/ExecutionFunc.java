package net.inet_lab.c4wa.wat;

import java.util.HashMap;
import java.util.Map;

public class ExecutionFunc {
    final Map<String, ExeLocal> locals;

    ExecutionFunc() {
        locals = new HashMap<>();
    }

    void registerLocal(String name, NumType numType) {
        if (locals.containsKey(name))
            throw new RuntimeException("Variable " + name + " already defined");

        locals.put(name, new ExeLocal(numType));
    }

    void assignLocal(String name, Const val) {
        ExeLocal v = locals.get(name);
        if (v == null)
            throw new RuntimeException("Local " + name + " is not defined");

        if (val == null)
            throw new RuntimeException("Attempt to assign NULL to local variable " + name);
        if (val.numType != v.numType)
            throw new RuntimeException("Attempt to assign " + val + " to local variable of type " + v.numType);

        v.val = val;
    }

    Const getLocal(String name) {
        ExeLocal v = locals.get(name);
        if (v == null)
            throw new RuntimeException("Local " + name + " is not defined");
        return v.val;
    }

    private static class ExeLocal {
        final NumType numType;
        Const val;

        ExeLocal(NumType numType) {
            this.numType = numType;
            this.val = numType.is_int()? new Const(numType, 0) : new Const(numType, 0.0);
        }
    }

    static class ExeReturn extends RuntimeException {
        final Const returnValue;
        ExeReturn(Const returnValue) {
            this.returnValue = returnValue;
        }
        ExeReturn() {
            this.returnValue = null;
        }
    }

    static class ExeBreak extends RuntimeException {
        final Const returnValue;
        final String label;
        ExeBreak(String label, Const returnValue) {
            this.returnValue = returnValue;
            this.label = label;
        }
        ExeBreak(String label) {
            this.returnValue = null;
            this.label = label;
        }
    }

}
