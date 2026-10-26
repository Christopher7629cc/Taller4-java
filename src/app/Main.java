package app;

import modelo.*;   // GUIADO: trae Titular, CuentaAhorro y Banco desde el paquete modelo

/*
 * EJERCICIO 4 (EJ4) - Programa principal                             [50 pts de salida + 3 de estructura]
 *
 * Main esta en el paquete "app"; las clases estan en "modelo". Por eso Main solo puede usar
 * lo que es PUBLIC (no puede usar validarMonto ni calcularComision directamente).
 *
 * Los datos NO se escriben a mano: salen de Escenario con la semilla.
 * Ejemplos:  Escenario.nombre(semilla, 1)   Escenario.edad(semilla, 2)   Escenario.montoOp(semilla, 3)
 * Lea los comentarios de Escenario.java para ver que devuelve cada metodo.
 *
 * Los formatos EXACTOS de salida estan en el documento del taller (Parte 4: Main).
 */
public class Main {

    public static void main(String[] args) {
        int semilla = Contexto.semilla(args);
        Contexto.imprimirCabecera(semilla);   // GUIADO: imprime las 2 primeras lineas (no las cambie)

        // TODO(EJ4) PARTE 1: crear los 2 titulares, imprimir, intentar cambiar la edad, contar jubilados
        // TODO(EJ4) PARTE 2: crear las 2 cuentas (una con cada constructor) e imprimir su estado()
        // TODO(EJ4) PARTE 3: ciclo for de 6 operaciones (deposito/retiro) sobre la cuenta indicada
        // TODO(EJ4) PARTE 4: transferencia entre las cuentas
        // TODO(EJ4) PARTE 5: crear el Banco e imprimir el resumen
    }
}
