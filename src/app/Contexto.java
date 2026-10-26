package app;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * ARCHIVO OFICIAL DEL PROFESOR - NO MODIFICAR.
 *
 * Ayudas ya hechas (usan cosas que aun no vemos: static, archivos, Locale).
 * Usted solo las LLAMA desde Main:
 *   int semilla = Contexto.semilla(args);
 *   Contexto.imprimirCabecera(semilla);
 *   Contexto.dinero(125.5)   ->  "125.50"
 */
public class Contexto {

    /** Semilla: primer argumento si existe; si no, los 3 ultimos digitos de identidad.txt. */
    public static int semilla(String[] args) {
        if (args != null && args.length > 0) {
            try {
                return Integer.parseInt(args[0].trim());
            } catch (NumberFormatException e) {
                // se ignora y se usa identidad.txt
            }
        }
        try {
            return Integer.parseInt(leer("integrante1_cedula_ultimos3").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Formato de dinero con 2 decimales y punto decimal, sin importar el idioma del sistema. */
    public static String dinero(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    /** Imprime las dos primeras lineas de la salida (incluye el detalle de control). */
    public static void imprimirCabecera(int semilla) {
        System.out.println("=== BANCO ISTMENO | Taller 4 ===");
        System.out.println("Control: " + leer("integrante1_apellido") + " / "
                + leer("integrante2_apellido") + " | semilla=" + semilla);
    }

    private static String leer(String clave) {
        try {
            File f = new File("identidad.txt");
            if (!f.exists()) {
                return "SIN-IDENTIDAD";
            }
            List<String> lineas = Files.readAllLines(f.toPath());
            for (String l : lineas) {
                String t = l.trim();
                if (t.startsWith("#") || !t.contains("=")) {
                    continue;
                }
                int i = t.indexOf('=');
                if (t.substring(0, i).trim().equals(clave)) {
                    String v = t.substring(i + 1).trim();
                    return v.isEmpty() ? "PENDIENTE" : v;
                }
            }
        } catch (Exception e) {
            // se devuelve el valor por defecto
        }
        return "PENDIENTE";
    }
}
