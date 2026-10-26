package app;

/**
 * ARCHIVO OFICIAL DEL PROFESOR - NO MODIFICAR.
 *
 * Genera los datos del taller a partir de la semilla (ultimos 3 digitos de la
 * cedula del integrante 1). Cada semilla produce datos distintos.
 *
 * NOTA PARA EL ESTUDIANTE: aqui se usan cosas que todavia no vemos en clase
 * (static, arreglos, switch). NO las copie en su codigo: solo llame a estos
 * metodos escribiendo  Escenario.nombre(semilla, 1)  y similares.
 */
public class Escenario {

    private static final String[] NOMBRES = {
        "Carlos", "Maria", "Jose", "Ana", "Luis",
        "Gabriela", "Miguel", "Daniela", "Ricardo", "Valeria"
    };
    private static final String[] APELLIDOS = {
        "Perez", "Gonzalez", "Rodriguez", "Jimenez", "Batista",
        "Mendoza", "Villarreal", "Quintero", "Samaniego", "Delgado"
    };

    private static int mezcla(int s, int k) {
        long x = (s * 7919L + k * 104729L + 12345L) % 2147483647L;
        x = (x * 48271L) % 2147483647L;
        x = (x * 48271L + k) % 2147483647L;
        return (int) x;
    }

    private static double monto(int s, int k) {
        int b = mezcla(s, k) % 6;
        int r = mezcla(s, k + 500);
        long c;
        switch (b) {
            case 0:  c = 50 + r % 9951; break;        // 0.50 .. 100.00
            case 1:  c = 10001 + r % 39999; break;    // 100.01 .. 500.00
            case 2:  c = 50001 + r % 250000; break;   // 500.01 .. 3000.00
            case 3:  c = 1000001 + r % 200000; break; // 10000.01 .. 12000.00 (invalido)
            case 4:  c = -(500 + r % 4501); break;    // -5.00 .. -50.00 (invalido)
            default: c = 100 + r % 9901; break;       // 1.00 .. 100.00
        }
        return c / 100.0;
    }

    /** Nombre del titular t (t = 1 o 2). */
    public static String nombre(int s, int t) { return NOMBRES[mezcla(s, 10 + t) % 10]; }

    /** Apellido del titular t (t = 1 o 2). */
    public static String apellido(int s, int t) { return APELLIDOS[mezcla(s, 20 + t) % 10]; }

    /** Edad inicial del titular t (18 a 77). */
    public static int edad(int s, int t) { return 18 + mezcla(s, 30 + t) % 60; }

    /** Edad "nueva" que se intentara asignar al titular t (10 a 110). */
    public static int edadNueva(int s, int t) { return 10 + mezcla(s, 40 + t) % 101; }

    /** Numero de la cuenta c (c = 1 o 2), de 6 digitos. */
    public static int numeroCuenta(int s, int c) { return 100000 + mezcla(s, 50 + c) % 900000; }

    /** Saldo inicial de la cuenta c (200.00 a 9000.00). */
    public static double saldoInicial(int s, int c) { return (20000 + mezcla(s, 60 + c) % 880001) / 100.0; }

    /** Tipo de la operacion i (i = 1..6): 1 = deposito, 2 = retiro. */
    public static int tipoOp(int s, int i) { return 1 + mezcla(s, 70 + i) % 2; }

    /** Cuenta sobre la que se hace la operacion i: 1 o 2. */
    public static int cuentaOp(int s, int i) { return 1 + mezcla(s, 80 + i) % 2; }

    /** Monto de la operacion i (puede ser invalido a proposito). */
    public static double montoOp(int s, int i) { return monto(s, 90 + i); }

    /** Cuenta que origina la transferencia: 1 o 2 (el destino es la otra). */
    public static int cuentaOrigen(int s) { return 1 + mezcla(s, 120) % 2; }

    /** Monto de la transferencia (puede ser invalido a proposito). */
    public static double montoTransferencia(int s) { return monto(s, 130); }

    /** Monto para la consulta de comision estimada (1.00 a 1500.99, siempre valido). */
    public static double montoComision(int s) { return (100 + mezcla(s, 140) % 150000) / 100.0; }
}
