package net.minestom.server.network.ir;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;

import static net.minestom.server.network.ir.IrCompiler.constructorInterface;
import static net.minestom.server.network.ir.IrEmitter.*;
import static net.minestom.server.network.ir.IrLocalAccess.*;
import static net.minestom.server.network.ir.IrValueEmitter.*;

final class IrRunEmitter {
    private IrRunEmitter() {
    }

    static void emitProgram(CodeBuilder codeBuilder, IrEmitter.EmitContext context, ProgramIr program) {
        for (RunIr run : program.runs()) {
            emitRun(codeBuilder, context, run);
        }
    }

    private static void emitRun(CodeBuilder codeBuilder, IrEmitter.EmitContext context, RunIr run) {
        if (run.reserve()) {
            codeBuilder.aload(context.directSlot());
            emitLongValue(codeBuilder, context, run.size());
            if (context.write()) {
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE);
            } else {
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveRead", MT_RESERVE);
            }
            codeBuilder.lstore(context.indexSlot());
        }

        for (RunItem item : run.items()) {
            emitItem(codeBuilder, context, item);
        }
    }

    private static void emitItem(CodeBuilder codeBuilder, IrEmitter.EmitContext context, RunItem item) {
        switch (item) {
            case RunItem.Put put -> {
                codeBuilder.aload(context.directSlot());
                emitLongValue(codeBuilder, context, put.offset());
                emitValue(codeBuilder, context, put.value());
                emitStoreKindWrite(codeBuilder, put.kind(), put.value());
            }
            case RunItem.Get get -> {
                codeBuilder.aload(context.directSlot());
                emitLongValue(codeBuilder, context, get.offset());
                emitStoreKindRead(codeBuilder, get.kind(), get.out());
                emitStoreLocal(codeBuilder, context, get.out());
            }
            case RunItem.PutVarInt putVarInt -> {
                codeBuilder.aload(context.directSlot());
                emitLongValue(codeBuilder, context, putVarInt.offset());
                emitIntValue(codeBuilder, context, putVarInt.value());
                codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeVarIntUnchecked", MT_WRITE_VAR_INT_UNCHECKED, true);
                codeBuilder.pop2();
            }
            case RunItem.GetVarInt getVarInt -> {
                codeBuilder.aload(context.directSlot())
                        .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "readVarInt", MT_READ_VAR_INT, true);
                emitStoreLocal(codeBuilder, context, getVarInt.out());
            }
            case RunItem.PutVarLong putVarLong -> {
                codeBuilder.aload(context.directSlot());
                emitLongValue(codeBuilder, context, putVarLong.offset());
                emitLongValue(codeBuilder, context, putVarLong.value());
                codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeVarLongUnchecked", MT_WRITE_VAR_LONG_UNCHECKED, true);
                codeBuilder.pop2();
            }
            case RunItem.GetVarLong getVarLong -> {
                codeBuilder.aload(context.directSlot())
                        .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "readVarLong", MT_READ_VAR_LONG, true);
                emitStoreLocal(codeBuilder, context, getVarLong.out());
            }
            case RunItem.PutBytes putBytes -> {
                codeBuilder.aload(context.directSlot());
                emitLongValue(codeBuilder, context, putBytes.offset());
                emitValue(codeBuilder, context, putBytes.byteArray());
                codeBuilder.iconst_0();
                emitIntValue(codeBuilder, context, putBytes.length());
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putBytesUnchecked", MT_PUT_BYTES);
            }
            case RunItem.GetBytes getBytes -> {
                final int lengthSlot = codeBuilder.allocateLocal(TypeKind.INT);
                emitIntValue(codeBuilder, context, getBytes.length());
                codeBuilder.dup()
                        .istore(lengthSlot)
                        .newarray(TypeKind.BYTE);
                emitStoreLocal(codeBuilder, context, getBytes.byteArray());
                codeBuilder.aload(context.directSlot());
                emitLongValue(codeBuilder, context, getBytes.offset());
                emitLoadLocal(codeBuilder, context, getBytes.byteArray());
                codeBuilder.iconst_0()
                        .iload(lengthSlot)
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getBytesUnchecked", MT_GET_BYTES);
            }
            case RunItem.If branch -> {
                final Label elseLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();
                emitValue(codeBuilder, context, branch.condition());
                codeBuilder.ifeq(elseLabel);
                for (RunIr child : branch.thenRuns()) emitRun(codeBuilder, context, child);
                codeBuilder.goto_(endLabel);
                codeBuilder.labelBinding(elseLabel);
                for (RunIr child : branch.elseRuns()) emitRun(codeBuilder, context, child);
                codeBuilder.labelBinding(endLabel);
            }
            case RunItem.ForEach loop -> {
                final Label startLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();

                emitValue(codeBuilder, context, loop.source());
                codeBuilder.checkcast(CD_ITERABLE);
                codeBuilder.invokeinterface(CD_ITERABLE, "iterator", MethodTypeDesc.of(CD_ITERATOR));
                final int iteratorSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                codeBuilder.astore(iteratorSlot);

                codeBuilder.labelBinding(startLabel);
                codeBuilder.aload(iteratorSlot);
                codeBuilder.invokeinterface(CD_ITERATOR, "hasNext", MethodTypeDesc.of(ConstantDescs.CD_boolean));
                codeBuilder.ifeq(endLabel);

                codeBuilder.aload(iteratorSlot);
                codeBuilder.invokeinterface(CD_ITERATOR, "next", MethodTypeDesc.of(CD_OBJECT));
                emitStoreLocal(codeBuilder, context, loop.element());

                for (RunIr child : loop.body()) emitRun(codeBuilder, context, child);

                codeBuilder.goto_(startLabel);
                codeBuilder.labelBinding(endLabel);
            }
            case RunItem.ForIndex loop -> {
                final Label startLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();
                final int loopIndexSlot = localSlot(codeBuilder, context, loop.index());

                emitIntValue(codeBuilder, context, loop.start());
                codeBuilder.istore(loopIndexSlot);

                codeBuilder.labelBinding(startLabel);
                codeBuilder.iload(loopIndexSlot);
                emitIntValue(codeBuilder, context, loop.end());
                codeBuilder.if_icmpge(endLabel);

                for (RunIr child : loop.body()) emitRun(codeBuilder, context, child);

                codeBuilder.iinc(loopIndexSlot, 1);
                codeBuilder.goto_(startLabel);
                codeBuilder.labelBinding(endLabel);
            }
            case RunItem.Apply apply -> {
                codeBuilder.getstatic(context.classDesc(), context.data().transformFunctionName(apply.function()), CD_FUNCTION);
                emitLoadLocal(codeBuilder, context, apply.in());
                codeBuilder.invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
                emitStoreLocal(codeBuilder, context, apply.out());
            }
            case RunItem.Cast cast -> {
                emitLoadLocal(codeBuilder, context, cast.in());
                codeBuilder.checkcast(classDesc(cast.targetClass()));
                emitStoreLocal(codeBuilder, context, cast.out());
            }
            case RunItem.Unbox unbox -> {
                emitLoadLocal(codeBuilder, context, unbox.in());
                emitUnboxedTypeIrValue(codeBuilder, unbox.kind());
                emitStoreLocal(codeBuilder, context, unbox.out());
            }
            case RunItem.Box box -> {
                emitLoadLocal(codeBuilder, context, box.in());
                emitBoxedTypeIrValue(codeBuilder, box.kind());
                emitStoreLocal(codeBuilder, context, box.out());
            }
            case RunItem.StringToBytes stringToBytes -> {
                emitLoadLocal(codeBuilder, context, stringToBytes.in());
                codeBuilder.checkcast(CD_STRING);
                codeBuilder.getstatic(CD_STANDARD_CHARSETS, "UTF_8", CD_CHARSET);
                codeBuilder.invokevirtual(CD_STRING, "getBytes", MethodTypeDesc.of(CD_BYTE_ARRAY, CD_CHARSET));
                emitStoreLocal(codeBuilder, context, stringToBytes.out());
            }
            case RunItem.BytesToString bytesToString -> {
                codeBuilder.new_(CD_STRING);
                codeBuilder.dup();
                emitLoadLocal(codeBuilder, context, bytesToString.in());
                codeBuilder.checkcast(CD_BYTE_ARRAY);
                codeBuilder.getstatic(CD_STANDARD_CHARSETS, "UTF_8", CD_CHARSET);
                codeBuilder.invokespecial(CD_STRING, ConstantDescs.INIT_NAME, MethodTypeDesc.of(CD_VOID, CD_BYTE_ARRAY, CD_CHARSET));
                emitStoreLocal(codeBuilder, context, bytesToString.out());
            }
            case RunItem.EitherLeft eitherLeft -> {
                emitLoadLocal(codeBuilder, context, eitherLeft.in());
                codeBuilder.invokestatic(CD_EITHER, "left", MethodTypeDesc.of(CD_EITHER, CD_OBJECT), true);
                emitStoreLocal(codeBuilder, context, eitherLeft.out());
            }
            case RunItem.EitherRight eitherRight -> {
                emitLoadLocal(codeBuilder, context, eitherRight.in());
                codeBuilder.invokestatic(CD_EITHER, "right", MethodTypeDesc.of(CD_EITHER, CD_OBJECT), true);
                emitStoreLocal(codeBuilder, context, eitherRight.out());
            }
            case RunItem.Store store -> {
                emitValue(codeBuilder, context, store.value());
                emitStoreLocal(codeBuilder, context, store.out());
            }
            case RunItem.Check check -> {
                final Label endLabel = codeBuilder.newLabel();
                emitValue(codeBuilder, context, check.condition());
                codeBuilder.ifne(endLabel);
                codeBuilder.new_(classDesc(IllegalArgumentException.class))
                        .dup()
                        .ldc(check.message())
                        .invokespecial(classDesc(IllegalArgumentException.class), ConstantDescs.INIT_NAME, MethodTypeDesc.of(CD_VOID, CD_STRING))
                        .athrow()
                        .labelBinding(endLabel);
            }
            case RunItem.WriteExternal writeExternal -> {
                codeBuilder.getstatic(context.classDesc(), context.data().typeFieldName(writeExternal.type()), CD_TYPE)
                        .aload(1);
                emitValue(codeBuilder, context, writeExternal.value());
                codeBuilder.invokeinterface(CD_TYPE, WRITE, MT_WRITE_OBJECT);
            }
            case RunItem.ReadExternal readExternal -> {
                codeBuilder.getstatic(context.classDesc(), context.data().typeFieldName(readExternal.type()), CD_TYPE)
                        .aload(1)
                        .invokeinterface(CD_TYPE, READ, MT_READ_OBJECT);
                emitStoreLocal(codeBuilder, context, readExternal.out());
            }
            case RunItem.ElementAt elementAt -> {
                emitValue(codeBuilder, context, elementAt.source());
                codeBuilder.checkcast(CD_LIST);
                emitIntValue(codeBuilder, context, elementAt.index());
                codeBuilder.invokeinterface(CD_LIST, "get", MethodTypeDesc.of(CD_OBJECT, CD_INT));
                emitStoreLocal(codeBuilder, context, elementAt.out());
            }
            case RunItem.MapEntrySet mapEntrySet -> {
                emitValue(codeBuilder, context, mapEntrySet.map());
                codeBuilder.checkcast(CD_MAP);
                codeBuilder.invokeinterface(CD_MAP, "entrySet", MethodTypeDesc.of(CD_SET));
                emitStoreLocal(codeBuilder, context, mapEntrySet.out());
            }
            case RunItem.ArrayCreate arrayCreate -> {
                emitIntValue(codeBuilder, context, arrayCreate.size());
                codeBuilder.anewarray(CD_OBJECT);
                emitStoreLocal(codeBuilder, context, arrayCreate.out());
            }
            case RunItem.ArraySet arraySet -> {
                emitLoadLocal(codeBuilder, context, arraySet.array());
                codeBuilder.checkcast(CD_OBJECT_ARRAY);
                emitIntValue(codeBuilder, context, arraySet.index());
                emitValue(codeBuilder, context, arraySet.value());
                codeBuilder.aastore();
            }
            case RunItem.ResultElementSet resultElementSet -> {
                emitValue(codeBuilder, context, resultElementSet.result());
                codeBuilder.checkcast(CD_OBJECT_ARRAY);
                emitIntValue(codeBuilder, context, resultElementSet.index());
                emitValue(codeBuilder, context, resultElementSet.value());
                codeBuilder.aastore();
            }
            case RunItem.ListFinish listFinish -> {
                emitLoadLocal(codeBuilder, context, listFinish.array());
                codeBuilder.checkcast(CD_OBJECT_ARRAY);
                codeBuilder.invokestatic(CD_LIST, "of", MethodTypeDesc.of(CD_LIST, CD_OBJECT_ARRAY), true);
                emitStoreLocal(codeBuilder, context, listFinish.out());
            }
            case RunItem.MapFinish mapFinish -> {
                emitLoadLocal(codeBuilder, context, mapFinish.keys());
                codeBuilder.checkcast(CD_OBJECT_ARRAY);
                emitLoadLocal(codeBuilder, context, mapFinish.values());
                codeBuilder.checkcast(CD_OBJECT_ARRAY);
                emitIntValue(codeBuilder, context, mapFinish.size());
                codeBuilder.invokestatic(CD_ARRAY_UTILS, "toMap", MT_TO_MAP);
                emitStoreLocal(codeBuilder, context, mapFinish.out());
            }
            case RunItem.MapEntryKey mapEntryKey -> {
                emitLoadLocal(codeBuilder, context, mapEntryKey.entry());
                codeBuilder.checkcast(CD_MAP_ENTRY);
                codeBuilder.invokeinterface(CD_MAP_ENTRY, "getKey", MethodTypeDesc.of(CD_OBJECT));
                emitStoreLocal(codeBuilder, context, mapEntryKey.out());
            }
            case RunItem.MapEntryValue mapEntryValue -> {
                emitLoadLocal(codeBuilder, context, mapEntryValue.entry());
                codeBuilder.checkcast(CD_MAP_ENTRY);
                codeBuilder.invokeinterface(CD_MAP_ENTRY, "getValue", MethodTypeDesc.of(CD_OBJECT));
                emitStoreLocal(codeBuilder, context, mapEntryValue.out());
            }
            case RunItem.Construct construct -> {
                final int argSize = construct.args().size();
                final ClassDesc constructorType = constructorInterface(argSize);
                codeBuilder.getstatic(context.classDesc(), context.data().ctorFieldName(construct.factory()), constructorType);
                for (Value arg : construct.args()) {
                    emitValue(codeBuilder, context, arg);
                }
                codeBuilder.invokeinterface(constructorType, "apply", constructorApplyType(argSize));
                emitStoreLocal(codeBuilder, context, construct.out());
            }
            case RunItem.Return ret -> {
                emitValue(codeBuilder, context, ret.value());
                codeBuilder.areturn();
            }
        }
    }

    private static void emitStoreKindWrite(CodeBuilder codeBuilder, StoreKind kind, Value value) {
        switch (kind) {
            case BOOLEAN, BYTE -> {
                if (emitsLong(value)) codeBuilder.l2i();
                codeBuilder.i2b()
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByteUnchecked", MT_PUT_BYTE);
            }
            case SHORT -> {
                if (emitsLong(value)) codeBuilder.l2i();
                codeBuilder.i2s()
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putShortUnchecked", MT_PUT_SHORT);
            }
            case INT -> {
                if (emitsLong(value)) codeBuilder.l2i();
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putIntUnchecked", MT_PUT_INT);
            }
            case LONG -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putLongUnchecked", MT_PUT_LONG);
            case FLOAT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putFloatUnchecked", MT_PUT_FLOAT);
            case DOUBLE -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putDoubleUnchecked", MT_PUT_DOUBLE);
        }
    }

    private static void emitStoreKindRead(CodeBuilder codeBuilder, StoreKind kind, Local out) {
        final TypeKind targetKind = localTypeKind(out.type());
        switch (kind) {
            case BOOLEAN -> {
                final var falseLabel = codeBuilder.newLabel();
                final var endLabel = codeBuilder.newLabel();
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getByteUnchecked", MT_GET_BYTE)
                        .iconst_0()
                        .if_icmpeq(falseLabel)
                        .iconst_1()
                        .goto_(endLabel)
                        .labelBinding(falseLabel)
                        .iconst_0()
                        .labelBinding(endLabel);
            }
            case BYTE -> {
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getByteUnchecked", MT_GET_BYTE);
                if (targetKind == TypeKind.INT) {
                    codeBuilder.sipush(0xFF)
                            .iand();
                }
            }
            case SHORT -> {
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getShortUnchecked", MT_GET_SHORT);
                if (targetKind == TypeKind.INT) {
                    codeBuilder.loadConstant(0xFFFF)
                            .iand();
                }
            }
            case INT -> {
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getIntUnchecked", MT_GET_INT);
                if (targetKind == TypeKind.LONG) {
                    codeBuilder.i2l()
                            .loadConstant(0xFFFFFFFFL)
                            .land();
                }
            }
            case LONG -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getLongUnchecked", MT_GET_LONG);
            case FLOAT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getFloatUnchecked", MT_GET_FLOAT);
            case DOUBLE -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getDoubleUnchecked", MT_GET_DOUBLE);
        }
    }
}
