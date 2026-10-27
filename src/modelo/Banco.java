package modelo;

/*
 * EJERCICIO 3 (EJ3) - Clase Banco                                    [5 pts de estructura]
 *
 * Administra DOS cuentas.
 *
 * ATRIBUTOS (PRIVATE): CuentaAhorro cuenta1, CuentaAhorro cuenta2
 * CONSTRUCTOR (public): Banco(CuentaAhorro cuenta1, CuentaAhorro cuenta2)
 *
 * METODOS (public):
 *   double       totalDepositos()      -> suma de los saldos de las dos cuentas
 *   CuentaAhorro cuentaMayorSaldo()    -> la cuenta con mayor saldo (si empatan, cuenta1).
 *                                         Devuelve LA MISMA cuenta (referencia), no una copia.
 *   double       comisionEstimada(CuentaAhorro cuenta, double monto)
 *                                      -> llama a cuenta.calcularComision(monto)
 *                                         (Banco esta en el MISMO paquete que CuentaAhorro,
 *                                          por eso puede llamar a un metodo protected;
 *                                          Main, que esta en otro paquete, NO puede).
 *   int          totalMovimientos()    -> movimientos de cuenta1 + movimientos de cuenta2
 */
public class Banco {

    // TODO(EJ3): atributos, constructor y metodos
private CuentaAhorro cuenta1;
private CuentaAhorro cuenta2;
public Banco(CuentaAhorro cuenta1, CuentaAhorro cuenta2) {
    this.cuenta1 = cuenta1;
    this.cuenta2 = cuenta2;
}
public double totalDepositos() {
    return cuenta1.getSaldo() + cuenta2.getSaldo();
}

public CuentaAhorro cuentaMayorSaldo() {
    if (cuenta1.getSaldo() >= cuenta2.getSaldo()) {
        return cuenta1;
    } else {
        return cuenta2;
    }
}

public double comisionEstimada(CuentaAhorro cuenta, double monto) {
    return cuenta.calcularComision(monto);
}

public int totalMovimientos() {
    return cuenta1.getMovimientos() + cuenta2.getMovimientos();
}
}
