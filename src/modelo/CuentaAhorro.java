package modelo;

import app.Contexto;   // GUIADO: se usa solo en estado() -> Contexto.dinero(valor)

/*
 * EJERCICIO 2 (EJ2) - Clase CuentaAhorro                             [14 pts de estructura]
 *
 * ATRIBUTOS (todos PRIVATE):
 *   int numero, double saldo, Titular titular, int movimientos
 *
 * CONSTRUCTORES (public) - sobrecarga:
 *   CuentaAhorro(int numero, Titular titular)                    -> saldo 0, movimientos 0
 *   CuentaAhorro(int numero, Titular titular, double saldoInicial) -> saldo = saldoInicial, movimientos 0
 *
 * GETTERS (public): getNumero(), getSaldo(), getTitular(), getMovimientos()
 *
 * MODIFICADORES DE ACCESO (tema 2.6) - respete EXACTAMENTE estos:
 *   boolean validarMonto(double monto)     <- SIN modificador (visibilidad de paquete)
 *        true si 0 < monto <= 10000; en otro caso false.
 *   protected double calcularComision(double monto)
 *        - si el titular es jubilado: 0.0
 *        - si no, y monto > 500:  monto * 0.01
 *        - si no, y monto > 100:  0.50
 *        - en otro caso:          0.25
 *
 * METODOS public:
 *   boolean depositar(double monto)
 *        Si validarMonto falla -> false. Si no: suma al saldo, movimientos++ y true.
 *   boolean retirar(double monto)
 *        Si validarMonto falla -> false. total = monto + calcularComision(monto).
 *        Si total > saldo -> false (no cambia nada). Si no: resta total, movimientos++ y true.
 *   boolean transferir(CuentaAhorro destino, double monto)
 *        Retira de ESTA cuenta (con su comision); si el retiro falla -> false.
 *        Si funciona, deposita el monto en destino y devuelve true.
 *   String estado()
 *        "Cuenta 123456 | titular=Maria Gonzalez | saldo=1500.00 | movimientos=2"
 *        (el dinero con Contexto.dinero(...))
 *
 * PROHIBIDO: static, arreglos, listas, herencia, try/catch, lambdas, Math.
 */
public class CuentaAhorro {

    // TODO(EJ2): atributos privados

    // TODO(EJ2): los dos constructores

    // TODO(EJ2): getters

    // TODO(EJ2): validarMonto (sin modificador) y calcularComision (protected)

    // TODO(EJ2): depositar, retirar, transferir, estado

}
