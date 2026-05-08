package net.minestom.server.network.ir;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;

final class IrLocalAccess {
    private IrLocalAccess() {
    }

    static void emitLoadLocal(CodeBuilder codeBuilder, IrEmitter.EmitContext context, Local local) {
        codeBuilder.loadLocal(localTypeKind(local.type()), localSlot(codeBuilder, context, local));
    }

    static void emitStoreLocal(CodeBuilder codeBuilder, IrEmitter.EmitContext context, Local local) {
        codeBuilder.storeLocal(localTypeKind(local.type()), localSlot(codeBuilder, context, local));
    }

    static int localSlot(CodeBuilder codeBuilder, IrEmitter.EmitContext context, Local local) {
        return context.locals().computeIfAbsent(local, ignored -> codeBuilder.allocateLocal(localTypeKind(local.type())));
    }

    static TypeKind localTypeKind(LocalType type) {
        return switch (type) {
            case LocalType.Kind kind -> kind.kind();
            case LocalType.Reference _ -> TypeKind.REFERENCE;
        };
    }
}
