package net.minestom.server.function;

import org.jetbrains.annotations.UnknownNullability;

public interface ThrowingFunctions {
    @FunctionalInterface
    interface TF1<P1 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1) throws Exception;

        static <T> TF1<T, T> identity() {
            return t -> t;
        }
    }

    @FunctionalInterface
    interface TF2<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2) throws Exception;
    }

    @FunctionalInterface
    interface TF3<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3) throws Exception;
    }

    @FunctionalInterface
    interface TF4<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4) throws Exception;
    }

    @FunctionalInterface
    interface TF5<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) throws Exception;
    }

    @FunctionalInterface
    interface TF6<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6) throws Exception;
    }

    @FunctionalInterface
    interface TF7<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7) throws Exception;
    }

    @FunctionalInterface
    interface TF8<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8) throws Exception;
    }

    @FunctionalInterface
    interface TF9<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, P9 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9) throws Exception;
    }

    @FunctionalInterface
    interface TF10<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, P9 extends @UnknownNullability Object, P10 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10) throws Exception;
    }

    @FunctionalInterface
    interface TF11<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, P9 extends @UnknownNullability Object, P10 extends @UnknownNullability Object, P11 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11) throws Exception;
    }

    @FunctionalInterface
    interface TF12<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, P9 extends @UnknownNullability Object, P10 extends @UnknownNullability Object, P11 extends @UnknownNullability Object, P12 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12) throws Exception;
    }

    @FunctionalInterface
    interface TF13<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, P9 extends @UnknownNullability Object, P10 extends @UnknownNullability Object, P11 extends @UnknownNullability Object, P12 extends @UnknownNullability Object, P13 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13) throws Exception;
    }

    @FunctionalInterface
    interface TF14<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, P9 extends @UnknownNullability Object, P10 extends @UnknownNullability Object, P11 extends @UnknownNullability Object, P12 extends @UnknownNullability Object, P13 extends @UnknownNullability Object, P14 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14) throws Exception;
    }

    @FunctionalInterface
    interface TF15<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, P9 extends @UnknownNullability Object, P10 extends @UnknownNullability Object, P11 extends @UnknownNullability Object, P12 extends @UnknownNullability Object, P13 extends @UnknownNullability Object, P14 extends @UnknownNullability Object, P15 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15) throws Exception;
    }

    @FunctionalInterface
    interface TF16<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, P9 extends @UnknownNullability Object, P10 extends @UnknownNullability Object, P11 extends @UnknownNullability Object, P12 extends @UnknownNullability Object, P13 extends @UnknownNullability Object, P14 extends @UnknownNullability Object, P15 extends @UnknownNullability Object, P16 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16) throws Exception;
    }

    @FunctionalInterface
    interface TF17<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, P9 extends @UnknownNullability Object, P10 extends @UnknownNullability Object, P11 extends @UnknownNullability Object, P12 extends @UnknownNullability Object, P13 extends @UnknownNullability Object, P14 extends @UnknownNullability Object, P15 extends @UnknownNullability Object, P16 extends @UnknownNullability Object, P17 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17) throws Exception;
    }

    @FunctionalInterface
    interface TF18<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, P9 extends @UnknownNullability Object, P10 extends @UnknownNullability Object, P11 extends @UnknownNullability Object, P12 extends @UnknownNullability Object, P13 extends @UnknownNullability Object, P14 extends @UnknownNullability Object, P15 extends @UnknownNullability Object, P16 extends @UnknownNullability Object, P17 extends @UnknownNullability Object, P18 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18) throws Exception;
    }

    @FunctionalInterface
    interface TF19<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, P9 extends @UnknownNullability Object, P10 extends @UnknownNullability Object, P11 extends @UnknownNullability Object, P12 extends @UnknownNullability Object, P13 extends @UnknownNullability Object, P14 extends @UnknownNullability Object, P15 extends @UnknownNullability Object, P16 extends @UnknownNullability Object, P17 extends @UnknownNullability Object, P18 extends @UnknownNullability Object, P19 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18, P19 p19) throws Exception;
    }

    @FunctionalInterface
    interface TF20<P1 extends @UnknownNullability Object, P2 extends @UnknownNullability Object, P3 extends @UnknownNullability Object, P4 extends @UnknownNullability Object, P5 extends @UnknownNullability Object, P6 extends @UnknownNullability Object, P7 extends @UnknownNullability Object, P8 extends @UnknownNullability Object, P9 extends @UnknownNullability Object, P10 extends @UnknownNullability Object, P11 extends @UnknownNullability Object, P12 extends @UnknownNullability Object, P13 extends @UnknownNullability Object, P14 extends @UnknownNullability Object, P15 extends @UnknownNullability Object, P16 extends @UnknownNullability Object, P17 extends @UnknownNullability Object, P18 extends @UnknownNullability Object, P19 extends @UnknownNullability Object, P20 extends @UnknownNullability Object, R extends @UnknownNullability Object> {
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18, P19 p19, P20 p20) throws Exception;
    }
}
