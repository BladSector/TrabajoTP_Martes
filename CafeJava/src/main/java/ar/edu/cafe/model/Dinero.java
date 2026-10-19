package ar.edu.cafe.model;

public final class Dinero {
    public static final double CERO = 0.0;

    private Dinero() { }

    public static double redondear(double valor) {
        if (!Double.isFinite(valor) || Math.abs(valor) > Long.MAX_VALUE / 100.0) {
            throw new IllegalArgumentException("El importe debe ser finito y estar dentro del rango permitido");
        }
        return Math.round(valor * 100.0) / 100.0;
    }

    public static double noNegativo(double valor) {
        if (!Double.isFinite(valor) || valor < 0.0) {
            throw new IllegalArgumentException("El importe debe ser finito y no negativo");
        }
        return redondear(valor);
    }
}
