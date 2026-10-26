/*
 * AUTOGRADE - Programacion 1 - Taller 4 (Modificadores de acceso, objetos, acceso a datos y metodos)
 *
 * Uso (desde la RAIZ del repositorio, con JDK 17 o superior):
 *     java autograde.java
 *
 * - NO guarda nada en servidores ni usa credenciales.
 * - Escribe solo resultado_autograde.json (ignorado por Git) y, si existe, el Job Summary de GitHub Actions.
 * - Es un archivo OFICIAL: si se modifica, el workflow lo detecta y lo marca como indicio de plagio.
 *
 * Opciones del profesor:
 *     java autograde.java --cobertura          verifica que las semillas cubren todas las ramas de la logica
 *     java autograde.java --hashes             imprime los hashes de Contexto.java y Escenario.java
 *     java autograde.java --sugerir-semillas   busca un conjunto pequeno de semillas que cubra todas las ramas
 */
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class Autograde {

    // ===================== CONFIGURACION DEL PROFESOR =====================
    static final String VERSION = "1.0";
    static final int NOTA_MINIMA = 60;
    static final int MIN_COMMITS = 4;
    static final int MIN_MINUTOS = 25;
    static final int TIMEOUT_SEG = 10;
    /** 15 semillas fijas (se agrega ademas la semilla real del equipo). Cubren todas las ramas: ver --cobertura. */
    static final int[] SEMILLAS = {0, 8, 42, 99, 123, 256, 314, 456, 500, 666, 789, 850, 921, 984, 999};
    /** Hashes SHA-256 (con saltos de linea normalizados) de los archivos oficiales. Recalcular con --hashes. */
    static final String HASH_CONTEXTO = "37d6c77af4858f7f1abc63bf7c92148eaeaaad340bd41bf29499df2f480dc652";
    static final String HASH_ESCENARIO = "b9602c9319f91ccd720fe274b8eff5cf5adaaf26e3b1d4ec62eb72e1fb0cd85b";
    /** Estructuras/funciones que aun NO se han visto en clase (etiqueta -> expresion regular sobre el codigo limpio). */
    static final String[][] NO_VISTO = {
        {"colecciones / java.util (ArrayList, List, Map, Arrays...)", "\\b(ArrayList|LinkedList|List|Map|HashMap|Set|HashSet|Arrays|Collections|Optional|Stream)\\b"},
        {"herencia / interfaces (extends, implements, abstract, super, instanceof)", "\\b(extends|implements|interface|abstract|super|instanceof)\\b"},
        {"enum / record", "\\b(enum|record)\\b"},
        {"lambdas / referencias a metodo (-> ::)", "->|::"},
        {"excepciones (try, catch, throw, throws, finally)", "\\b(try|catch|finally|throws?)\\b"},
        {"anotaciones (@Override, etc.)", "@\\w+"},
        {"var", "\\bvar\\b"},
        {"Math.*", "\\bMath\\s*\\."},
    };
    /** Cosas que ningun estudiante necesita: leer archivos, red, procesos, reflexion... */
    static final String SOSPECHOSO = "\\b(System\\s*\\.\\s*exit|Runtime|ProcessBuilder|Thread|java\\s*\\.\\s*io|java\\s*\\.\\s*nio|java\\s*\\.\\s*net|Files|Class\\s*\\.\\s*forName|reflect)\\b";
    // Pesos de la observacion de plagio
    static final int P_HARDCODE = 4, P_SOSPECHOSO = 4, P_POCOS_COMMITS = 2, P_COMMITS_JUNTOS = 2,
            P_AUTORES_3 = 3, P_AUTORES_2_AJENOS = 1, P_CONTROL = 2, P_CABECERA = 1,
            P_AUTOGRADE_MOD = 5, P_WORKFLOW_MOD = 5, P_OFICIALES_MOD = 4, P_NO_VISTO_C_U = 1, P_NO_VISTO_MAX = 3;
    // ======================================================================

    static final String[] SECCIONES = {"PARTE 1", "PARTE 2", "PARTE 3", "PARTE 4", "PARTE 5"};
    static final double[] PESOS = {8, 8, 16, 9, 9}; // = 50
    static final String[] NOMBRES_SEC = {"Titulares", "Cuentas", "Operaciones", "Transferencia", "Banco"};

    // ============================ MODELO DE REFERENCIA ============================
    // Replica la logica esperada del taller. Registra ramas para medir la cobertura de las semillas.
    static final Map<String, Integer> RAMAS = new TreeMap<>();
    static boolean registrar = false;
    static void r(String k) { if (registrar) RAMAS.merge(k, 1, Integer::sum); }
    static final String[] TODAS_RAMAS = {"SETEDAD_OK", "SETEDAD_NO", "JUB_SI", "JUB_NO", "MONTO_NEG", "MONTO_GRANDE", "MONTO_OK",
        "DEP_OK", "DEP_INV", "RET_OK", "RET_INV", "RET_SALDO", "COM_JUB", "COM_500", "COM_100", "COM_BAJA",
        "TRF_OK", "TRF_FALLA_INV", "TRF_FALLA_SALDO", "MAYOR_C1", "MAYOR_C2", "OP_DEP", "OP_RET", "JUBILADOS_0", "JUBILADOS_1", "JUBILADOS_2"};

    static final String[] NOMBRES = {"Carlos", "Maria", "Jose", "Ana", "Luis", "Gabriela", "Miguel", "Daniela", "Ricardo", "Valeria"};
    static final String[] APELLIDOS = {"Perez", "Gonzalez", "Rodriguez", "Jimenez", "Batista", "Mendoza", "Villarreal", "Quintero", "Samaniego", "Delgado"};

    static int mezcla(int s, int k) {
        long x = (s * 7919L + k * 104729L + 12345L) % 2147483647L;
        x = (x * 48271L) % 2147483647L;
        x = (x * 48271L + k) % 2147483647L;
        return (int) x;
    }
    static double monto(int s, int k) {
        int b = mezcla(s, k) % 6;
        int r = mezcla(s, k + 500);
        long c;
        switch (b) {
            case 0: c = 50 + r % 9951; break;
            case 1: c = 10001 + r % 39999; break;
            case 2: c = 50001 + r % 250000; break;
            case 3: c = 1000001 + r % 200000; break;
            case 4: c = -(500 + r % 4501); break;
            default: c = 100 + r % 9901; break;
        }
        return c / 100.0;
    }
    static String nombre(int s, int t) { return NOMBRES[mezcla(s, 10 + t) % 10]; }
    static String apellido(int s, int t) { return APELLIDOS[mezcla(s, 20 + t) % 10]; }
    static int edad(int s, int t) { return 18 + mezcla(s, 30 + t) % 60; }
    static int edadNueva(int s, int t) { return 10 + mezcla(s, 40 + t) % 101; }
    static int numeroCuenta(int s, int c) { return 100000 + mezcla(s, 50 + c) % 900000; }
    static double saldoInicial(int s, int c) { return (20000 + mezcla(s, 60 + c) % 880001) / 100.0; }
    static int tipoOp(int s, int i) { return 1 + mezcla(s, 70 + i) % 2; }
    static int cuentaOp(int s, int i) { return 1 + mezcla(s, 80 + i) % 2; }
    static double montoOp(int s, int i) { return monto(s, 90 + i); }
    static int cuentaOrigen(int s) { return 1 + mezcla(s, 120) % 2; }
    static double montoTransferencia(int s) { return monto(s, 130); }
    static double montoComision(int s) { return (100 + mezcla(s, 140) % 150000) / 100.0; }
    static String d(double v) { return String.format(Locale.US, "%.2f", v); }

    static class MTitular {
        String n, a; int e;
        MTitular(String n, String a, int e) { this.n = n; this.a = a; this.e = e; }
        boolean setEdad(int x) { if (x >= 18 && x <= 100) { e = x; r("SETEDAD_OK"); return true; } r("SETEDAD_NO"); return false; }
        boolean jub() { if (e >= 60) { r("JUB_SI"); return true; } r("JUB_NO"); return false; }
        String completo() { return n + " " + a; }
    }
    static class MCuenta {
        int num; double saldo; MTitular t; int mov;
        MCuenta(int num, MTitular t, double s) { this.num = num; this.t = t; this.saldo = s; }
        boolean valido(double m) {
            if (m <= 0) { r("MONTO_NEG"); return false; }
            if (m > 10000) { r("MONTO_GRANDE"); return false; }
            r("MONTO_OK"); return true;
        }
        double comision(double m) {
            if (t.jub()) { r("COM_JUB"); return 0.0; }
            if (m > 500) { r("COM_500"); return m * 0.01; }
            if (m > 100) { r("COM_100"); return 0.50; }
            r("COM_BAJA"); return 0.25;
        }
        boolean depositar(double m) {
            if (!valido(m)) { r("DEP_INV"); return false; }
            saldo += m; mov++; r("DEP_OK"); return true;
        }
        boolean retirar(double m) {
            if (!valido(m)) { r("RET_INV"); return false; }
            double tot = m + comision(m);
            if (tot > saldo) { r("RET_SALDO"); return false; }
            saldo -= tot; mov++; r("RET_OK"); return true;
        }
        boolean transferir(MCuenta dest, double m) {
            if (!retirar(m)) { r(m <= 0 || m > 10000 ? "TRF_FALLA_INV" : "TRF_FALLA_SALDO"); return false; }
            dest.depositar(m); r("TRF_OK"); return true;
        }
        String estado() { return "Cuenta " + num + " | titular=" + t.completo() + " | saldo=" + d(saldo) + " | movimientos=" + mov; }
    }

    /** Salida esperada para una semilla: secciones "PARTE n" -> lineas. */
    static LinkedHashMap<String, List<String>> esperado(int s) {
        LinkedHashMap<String, List<String>> m = new LinkedHashMap<>();
        List<String> p1 = new ArrayList<>(); m.put("PARTE 1", p1);
        p1.add("== PARTE 1: TITULARES ==");
        MTitular t1 = new MTitular(nombre(s, 1), apellido(s, 1), edad(s, 1));
        MTitular t2 = new MTitular(nombre(s, 2), apellido(s, 2), edad(s, 2));
        p1.add("Titular 1: " + t1.completo() + " | edad=" + t1.e + " | jubilado=" + (t1.jub() ? "SI" : "NO"));
        p1.add("Titular 2: " + t2.completo() + " | edad=" + t2.e + " | jubilado=" + (t2.jub() ? "SI" : "NO"));
        int n1 = edadNueva(s, 1); boolean o1 = t1.setEdad(n1);
        p1.add("Cambio de edad titular 1 a " + n1 + ": " + (o1 ? "ACEPTADO" : "RECHAZADO") + " | edad=" + t1.e);
        int n2 = edadNueva(s, 2); boolean o2 = t2.setEdad(n2);
        p1.add("Cambio de edad titular 2 a " + n2 + ": " + (o2 ? "ACEPTADO" : "RECHAZADO") + " | edad=" + t2.e);
        int j = (t1.jub() ? 1 : 0) + (t2.jub() ? 1 : 0);
        r("JUBILADOS_" + j);
        p1.add("Jubilados tras el cambio: " + j);

        List<String> p2 = new ArrayList<>(); m.put("PARTE 2", p2);
        p2.add("== PARTE 2: CUENTAS ==");
        MCuenta c1 = new MCuenta(numeroCuenta(s, 1), t1, saldoInicial(s, 1));
        MCuenta c2 = new MCuenta(numeroCuenta(s, 2), t2, 0);
        c2.depositar(saldoInicial(s, 2));
        p2.add(c1.estado()); p2.add(c2.estado());

        List<String> p3 = new ArrayList<>(); m.put("PARTE 3", p3);
        p3.add("== PARTE 3: OPERACIONES ==");
        for (int i = 1; i <= 6; i++) {
            MCuenta c = cuentaOp(s, i) == 2 ? c2 : c1;
            double mo = montoOp(s, i);
            boolean ok; String nom;
            if (tipoOp(s, i) == 1) { r("OP_DEP"); nom = "DEPOSITO"; ok = c.depositar(mo); }
            else { r("OP_RET"); nom = "RETIRO"; ok = c.retirar(mo); }
            p3.add("Op " + i + " " + nom + " " + d(mo) + " cuenta " + c.num + ": " + (ok ? "OK" : "RECHAZADO") + " | saldo=" + d(c.saldo));
        }

        List<String> p4 = new ArrayList<>(); m.put("PARTE 4", p4);
        p4.add("== PARTE 4: TRANSFERENCIA ==");
        MCuenta org = cuentaOrigen(s) == 2 ? c2 : c1;
        MCuenta dst = org == c1 ? c2 : c1;
        double mt = montoTransferencia(s);
        boolean ot = org.transferir(dst, mt);
        p4.add("Transferencia de " + org.num + " a " + dst.num + " por " + d(mt) + ": " + (ot ? "OK" : "RECHAZADA"));
        p4.add("Origen: saldo=" + d(org.saldo) + " | Destino: saldo=" + d(dst.saldo));

        List<String> p5 = new ArrayList<>(); m.put("PARTE 5", p5);
        p5.add("== PARTE 5: BANCO ==");
        p5.add("Total depositos: " + d(c1.saldo + c2.saldo));
        MCuenta may;
        if (c1.saldo >= c2.saldo) { r("MAYOR_C1"); may = c1; } else { r("MAYOR_C2"); may = c2; }
        p5.add("Cuenta con mayor saldo: " + may.num);
        double mc = montoComision(s);
        p5.add("Comision estimada cuenta " + may.num + " por " + d(mc) + ": " + d(may.comision(mc)));
        p5.add("Movimientos totales: " + (c1.mov + c2.mov));
        return m;
    }

    // ============================ UTILIDADES ============================
    static Path RAIZ = Paths.get("").toAbsolutePath();

    static String leer(Path p) { try { return new String(Files.readAllBytes(p), StandardCharsets.UTF_8); } catch (IOException e) { return null; } }

    static String sha(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(s.replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return "error"; }
    }

    static String git(String... a) {
        List<String> cmd = new ArrayList<>(Arrays.asList("git", "-c", "safe.directory=*"));
        cmd.addAll(Arrays.asList(a));
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd).directory(RAIZ.toFile()).redirectErrorStream(false);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor(30, TimeUnit.SECONDS);
            return p.exitValue() == 0 ? out : "";
        } catch (Exception e) { return ""; }
    }

    static String sinTildes(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    static class Limpio { String codigo; List<String> literales = new ArrayList<>(); }

    static Limpio limpiar(String s) {
        Limpio L = new Limpio(); StringBuilder c = new StringBuilder();
        int i = 0, n = s.length();
        while (i < n) {
            char ch = s.charAt(i);
            if (ch == '/' && i + 1 < n && s.charAt(i + 1) == '/') { while (i < n && s.charAt(i) != '\n') i++; }
            else if (ch == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) { if (s.charAt(i) == '\n') c.append('\n'); i++; }
                i += 2;
            } else if (ch == '"') {
                StringBuilder l = new StringBuilder(); i++;
                while (i < n && s.charAt(i) != '"' && s.charAt(i) != '\n') {
                    if (s.charAt(i) == '\\' && i + 1 < n) { l.append(s.charAt(i + 1)); i += 2; } else { l.append(s.charAt(i)); i++; }
                }
                i++; L.literales.add(l.toString()); c.append("\"\"");
            } else if (ch == '\'') {
                c.append("' '"); i++;
                while (i < n && s.charAt(i) != '\'' && s.charAt(i) != '\n') { if (s.charAt(i) == '\\') i++; i++; }
                i++;
            } else { c.append(ch); i++; }
        }
        L.codigo = c.toString();
        return L;
    }

    // ---- ejecucion de procesos ----
    static class Corrida { String salida = ""; String error = ""; boolean timeout; int codigo = -1; }

    static Corrida correr(Path out, int semilla) {
        Corrida c = new Corrida();
        try {
            String java = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
            ProcessBuilder pb = new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-Duser.language=en", "-Duser.country=US",
                    "-Xmx256m", "-cp", out.toString(), "app.Main", String.valueOf(semilla)).directory(RAIZ.toFile());
            Set<String> keep = new HashSet<>(Arrays.asList("path", "systemroot", "temp", "tmp"));
            pb.environment().keySet().removeIf(k -> !keep.contains(k.toLowerCase()));
            pb.redirectInput(ProcessBuilder.Redirect.from(nullFile()));
            Process p = pb.start();
            final ByteArrayOutputStream bo = new ByteArrayOutputStream(), be = new ByteArrayOutputStream();
            final boolean[] excesivo = {false};
            Thread t1 = lector(p.getInputStream(), bo, p, excesivo), t2 = lector(p.getErrorStream(), be, p, excesivo);
            t1.start(); t2.start();
            if (!p.waitFor(TIMEOUT_SEG, TimeUnit.SECONDS)) { p.destroyForcibly(); c.timeout = true; p.waitFor(); }
            else c.codigo = p.exitValue();
            t1.join(2000); t2.join(2000);
            c.salida = bo.toString(StandardCharsets.UTF_8); c.error = be.toString(StandardCharsets.UTF_8);
            if (excesivo[0]) { c.timeout = true; c.error = "La salida supero 300 KB (posible ciclo infinito)."; }
        } catch (Exception e) { c.error = "No se pudo ejecutar: " + e; }
        return c;
    }
    static Thread lector(InputStream in, ByteArrayOutputStream dst, Process p, boolean[] excesivo) {
        Thread t = new Thread(() -> {
            byte[] buf = new byte[8192]; int n;
            try { while ((n = in.read(buf)) > 0) { if (dst.size() < 300_000) dst.write(buf, 0, n); else { excesivo[0] = true; p.destroyForcibly(); break; } } } catch (IOException e) { }
        });
        t.setDaemon(true); return t;
    }
    static File nullFile() { return new File(System.getProperty("os.name").toLowerCase().contains("win") ? "NUL" : "/dev/null"); }
    // ---- comparacion con tolerancia de +-0.01 en dinero ----
    static final Pattern NUM = Pattern.compile("-?\\d+\\.\\d+");
    static boolean igual(String esp, String obt) {
        esp = esp.trim(); obt = obt.trim();
        if (esp.equals(obt)) return true;
        Matcher a = NUM.matcher(esp), b = NUM.matcher(obt);
        List<Double> na = new ArrayList<>(), nb = new ArrayList<>();
        while (a.find()) na.add(Double.parseDouble(a.group()));
        while (b.find()) nb.add(Double.parseDouble(b.group()));
        if (na.size() != nb.size() || na.isEmpty()) return false;
        if (!NUM.matcher(esp).replaceAll("#").equals(NUM.matcher(obt).replaceAll("#"))) return false;
        for (int i = 0; i < na.size(); i++) if (Math.abs(na.get(i) - nb.get(i)) > 0.0100001) return false;
        return true;
    }

    static Map<String, List<String>> partir(String salida) {
        Map<String, List<String>> m = new LinkedHashMap<>(); List<String> cur = null;
        for (String l : salida.split("\\r?\\n")) {
            Matcher mm = Pattern.compile("^==\\s*(PARTE \\d)\\b.*").matcher(l.trim());
            if (mm.matches()) { cur = new ArrayList<>(); m.put(mm.group(1), cur); cur.add(l.trim()); }
            else if (cur != null && !l.trim().isEmpty()) cur.add(l.trim());
        }
        return m;
    }

    /** null si coincide; si no, descripcion de la primera diferencia. */
    static String diferencia(List<String> esp, List<String> obt) {
        if (obt == null) return "No aparece esta parte en la salida.";
        for (int i = 0; i < Math.max(esp.size(), obt.size()); i++) {
            String e = i < esp.size() ? esp.get(i) : "(nada)", o = i < obt.size() ? obt.get(i) : "(falta la linea)";
            if (i >= esp.size() || i >= obt.size() || !igual(e, o)) return "esperado: [" + e + "]  obtenido: [" + o + "]";
        }
        return null;
    }

    // ============================ REFLEXION ============================
    static class Chk { String id, desc; double pts; boolean ok; String nota = ""; Chk(String id, String desc, double pts) { this.id = id; this.desc = desc; this.pts = pts; } }

    static String vis(int m) { return Modifier.isPublic(m) ? "public" : Modifier.isProtected(m) ? "protected" : Modifier.isPrivate(m) ? "private" : "package"; }

    static boolean campo(Class<?> c, String n, Class<?> t) {
        try { java.lang.reflect.Field f = c.getDeclaredField(n); return f.getType() == t && Modifier.isPrivate(f.getModifiers()) && !Modifier.isStatic(f.getModifiers()); }
        catch (Exception e) { return false; }
    }
    static boolean metodo(Class<?> c, String n, Class<?> ret, String visib, Class<?>... ps) {
        try { Method m = c.getDeclaredMethod(n, ps); return m.getReturnType() == ret && vis(m.getModifiers()).equals(visib) && !Modifier.isStatic(m.getModifiers()); }
        catch (Exception e) { return false; }
    }
    static boolean ctor(Class<?> c, String visib, Class<?>... ps) {
        try { return vis(c.getDeclaredConstructor(ps).getModifiers()).equals(visib); } catch (Exception e) { return false; }
    }

    static List<Chk> estructura(Path outModelo, boolean compiloModelo, Path mainSrc) {
        List<Chk> L = new ArrayList<>();
        Class<?> T = null, C = null, B = null;
        if (compiloModelo) {
            try (URLClassLoader cl = new URLClassLoader(new URL[]{outModelo.toUri().toURL()})) {
                T = cargar(cl, "modelo.Titular"); C = cargar(cl, "modelo.CuentaAhorro"); B = cargar(cl, "modelo.Banco");
                evaluarEstructura(L, T, C, B);
            } catch (Exception e) { /* se reporta abajo */ }
        }
        if (L.isEmpty()) evaluarEstructura(L, null, null, null);
        // EJ4: Main
        String main = mainSrc != null && Files.exists(mainSrc) ? leer(mainSrc) : null;
        Limpio lm = main == null ? null : limpiar(main);
        Chk a = new Chk("EJ4-a", "Main crea los 2 titulares con new Titular(...)", 1);
        Chk b = new Chk("EJ4-b", "Main usa los dos constructores de CuentaAhorro (2 y 3 argumentos)", 1);
        Chk c = new Chk("EJ4-c", "Main crea el Banco con new Banco(...)", 1);
        if (lm != null) {
            a.ok = contar(lm.codigo, "new\\s+Titular\\s*\\(") >= 2;
            c.ok = contar(lm.codigo, "new\\s+Banco\\s*\\(") >= 1;
            Set<Integer> aridades = new HashSet<>();
            Matcher m = Pattern.compile("new\\s+CuentaAhorro\\s*\\(").matcher(lm.codigo);
            while (m.find()) aridades.add(argumentos(lm.codigo, m.end()));
            b.ok = aridades.contains(2) && aridades.contains(3);
        }
        L.add(a); L.add(b); L.add(c);
        return L;
    }
    static Class<?> cargar(ClassLoader cl, String n) { try { return Class.forName(n, false, cl); } catch (Throwable e) { return null; } }
    static int contar(String s, String re) { Matcher m = Pattern.compile(re).matcher(s); int n = 0; while (m.find()) n++; return n; }
    static int argumentos(String s, int desde) {
        int prof = 1, n = 1, i = desde; boolean vacio = true;
        while (i < s.length() && prof > 0) {
            char ch = s.charAt(i);
            if (ch == '(') prof++; else if (ch == ')') prof--; else if (ch == ',' && prof == 1) n++;
            if (prof > 0 && !Character.isWhitespace(ch)) vacio = false;
            i++;
        }
        return vacio ? 0 : n;
    }

    static void evaluarEstructura(List<Chk> L, Class<?> T, Class<?> C, Class<?> B) {
        Class<?> S = String.class; Class<?> I = int.class, D = double.class, Z = boolean.class;
        // EJ1 Titular (8)
        add(L, "EJ1-a", "Titular: atributos private nombre, apellido (String) y edad (int)", 3, T != null && campo(T, "nombre", S) && campo(T, "apellido", S) && campo(T, "edad", I));
        add(L, "EJ1-b", "Titular: constructor public (String, String, int)", 1, T != null && ctor(T, "public", S, S, I));
        add(L, "EJ1-c", "Titular: getters public getNombre/getApellido/getEdad", 1, T != null && metodo(T, "getNombre", S, "public") && metodo(T, "getApellido", S, "public") && metodo(T, "getEdad", I, "public"));
        add(L, "EJ1-d", "Titular: public String getNombreCompleto()", 1, T != null && metodo(T, "getNombreCompleto", S, "public"));
        add(L, "EJ1-e", "Titular: public boolean setEdad(int)", 1, T != null && metodo(T, "setEdad", Z, "public", I));
        add(L, "EJ1-f", "Titular: public boolean esJubilado()", 1, T != null && metodo(T, "esJubilado", Z, "public"));
        // EJ2 CuentaAhorro (14)
        add(L, "EJ2-a", "CuentaAhorro: atributos private numero, saldo, titular, movimientos", 3, C != null && T != null && campo(C, "numero", I) && campo(C, "saldo", D) && campo(C, "titular", T) && campo(C, "movimientos", I));
        add(L, "EJ2-b", "CuentaAhorro: dos constructores public (int,Titular) y (int,Titular,double)", 2, C != null && T != null && ctor(C, "public", I, T) && ctor(C, "public", I, T, D));
        add(L, "EJ2-c", "CuentaAhorro: public boolean depositar(double)", 1, C != null && metodo(C, "depositar", Z, "public", D));
        add(L, "EJ2-d", "CuentaAhorro: public boolean retirar(double)", 1, C != null && metodo(C, "retirar", Z, "public", D));
        add(L, "EJ2-e", "CuentaAhorro: public boolean transferir(CuentaAhorro, double)", 1, C != null && metodo(C, "transferir", Z, "public", C, D));
        add(L, "EJ2-f", "CuentaAhorro: protected double calcularComision(double)", 2, C != null && metodo(C, "calcularComision", D, "protected", D));
        add(L, "EJ2-g", "CuentaAhorro: boolean validarMonto(double) con visibilidad de paquete (sin modificador)", 2, C != null && metodo(C, "validarMonto", Z, "package", D));
        add(L, "EJ2-h", "CuentaAhorro: getters public y public String estado()", 2, C != null && T != null && metodo(C, "getNumero", I, "public") && metodo(C, "getSaldo", D, "public")
                && metodo(C, "getTitular", T, "public") && metodo(C, "getMovimientos", I, "public") && metodo(C, "estado", S, "public"));
        // EJ3 Banco (5)
        add(L, "EJ3-a", "Banco: dos atributos private de tipo CuentaAhorro", 1, B != null && C != null && contarCampos(B, C, 2));
        add(L, "EJ3-b", "Banco: constructor public (CuentaAhorro, CuentaAhorro)", 1, B != null && C != null && ctor(B, "public", C, C));
        add(L, "EJ3-c", "Banco: public double totalDepositos()", 1, B != null && metodo(B, "totalDepositos", D, "public"));
        add(L, "EJ3-d", "Banco: public CuentaAhorro cuentaMayorSaldo()", 1, B != null && C != null && metodo(B, "cuentaMayorSaldo", C, "public"));
        add(L, "EJ3-e", "Banco: public double comisionEstimada(CuentaAhorro,double) y public int totalMovimientos()", 1, B != null && C != null && metodo(B, "comisionEstimada", D, "public", C, D) && metodo(B, "totalMovimientos", I, "public"));
    }
    static boolean contarCampos(Class<?> b, Class<?> c, int n) {
        int k = 0;
        for (java.lang.reflect.Field f : b.getDeclaredFields()) if (f.getType() == c && Modifier.isPrivate(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())) k++;
        return k == n;
    }
    static void add(List<Chk> L, String id, String d, double p, boolean ok) { Chk c = new Chk(id, d, p); c.ok = ok; L.add(c); }

    // ============================ COMPILACION ============================
    static String compilar(Path out, List<Path> fuentes, Path classpath) {
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        if (jc == null) return "No se encontro el compilador (necesita un JDK, no solo JRE).";
        List<String> args = new ArrayList<>(Arrays.asList("-encoding", "UTF-8", "-nowarn", "-d", out.toString()));
        if (classpath != null) { args.add("-cp"); args.add(classpath.toString()); }
        for (Path p : fuentes) args.add(p.toString());
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        int rc = jc.run(null, bo, bo, args.toArray(new String[0]));
        return rc == 0 ? null : bo.toString(StandardCharsets.UTF_8);
    }
    static List<Path> java(Path dir) {
        List<Path> r = new ArrayList<>();
        if (!Files.isDirectory(dir)) return r;
        try (java.util.stream.Stream<Path> s = Files.list(dir)) { s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(r::add); } catch (IOException e) { }
        return r;
    }

    // ============================ PRINCIPAL ============================
    public static void main(String[] args) throws Exception {
        List<String> a = Arrays.asList(args);
        if (a.contains("--cobertura")) { System.exit(cobertura() ? 0 : 2); }
        if (a.contains("--sugerir-semillas")) { sugerir(); return; }
        if (a.contains("--hashes")) {
            System.out.println("HASH_CONTEXTO  = \"" + sha(Optional.ofNullable(leer(RAIZ.resolve("src/app/Contexto.java"))).orElse("")) + "\"");
            System.out.println("HASH_ESCENARIO = \"" + sha(Optional.ofNullable(leer(RAIZ.resolve("src/app/Escenario.java"))).orElse("")) + "\"");
            return;
        }
        int i = a.indexOf("--dir");
        if (i >= 0 && i + 1 < a.size()) RAIZ = Paths.get(a.get(i + 1)).toAbsolutePath();
        System.exit(calificar());
    }

    static boolean cobertura() {
        RAMAS.clear(); registrar = true;
        for (int s : SEMILLAS) esperado(s);
        registrar = false;
        boolean todo = true;
        System.out.println("COBERTURA DE RAMAS con " + SEMILLAS.length + " semillas " + Arrays.toString(SEMILLAS));
        for (String k : TODAS_RAMAS) {
            int n = RAMAS.getOrDefault(k, 0);
            System.out.printf("  %-16s %s (%d veces)%n", k, n > 0 ? "CUBIERTA" : "** SIN CUBRIR **", n);
            if (n == 0) todo = false;
        }
        System.out.println(todo ? "RESULTADO: todas las ramas cubiertas." : "RESULTADO: faltan ramas.");
        return todo;
    }

    static void sugerir() {
        Set<String> cub = new HashSet<>(); List<Integer> el = new ArrayList<>();
        while (cub.size() < TODAS_RAMAS.length && el.size() < 40) {
            int mejor = -1, mn = 0;
            for (int s = 0; s < 1000; s++) {
                RAMAS.clear(); registrar = true; esperado(s); registrar = false;
                Set<String> n = new HashSet<>(RAMAS.keySet()); n.removeAll(cub);
                if (n.size() > mn) { mn = n.size(); mejor = s; }
            }
            if (mejor < 0) break;
            RAMAS.clear(); registrar = true; esperado(mejor); registrar = false;
            cub.addAll(RAMAS.keySet()); el.add(mejor);
        }
        System.out.println("Semillas sugeridas: " + el + "  (ramas cubiertas: " + cub.size() + "/" + TODAS_RAMAS.length + ")");
    }

    static String env(String k) { String v = System.getenv(k); return v == null ? "" : v.trim(); }

    static int calificar() throws Exception {
        List<String> detalles = new ArrayList<>();
        Map<String, String> id = new LinkedHashMap<>();
        String idTxt = leer(RAIZ.resolve("identidad.txt"));
        if (idTxt != null) for (String l : idTxt.split("\\r?\\n")) {
            l = l.trim(); int p = l.indexOf('=');
            if (l.startsWith("#") || p < 0) continue;
            id.put(l.substring(0, p).trim(), l.substring(p + 1).trim());
        }
        String[] claves = {"usuario_github", "integrante1_nombre", "integrante1_apellido", "integrante1_cedula_ultimos3", "integrante2_nombre", "integrante2_apellido", "integrante2_cedula_ultimos3"};
        List<String> faltan = new ArrayList<>();
        for (String k : claves) {
            String v = id.getOrDefault(k, "");
            boolean mal = v.isEmpty() || v.equalsIgnoreCase("PENDIENTE") || (k.endsWith("ultimos3") && !v.matches("\\d{3}"));
            if (mal) faltan.add(k);
        }
        String branch = !env("GITHUB_HEAD_REF").isEmpty() ? env("GITHUB_HEAD_REF") : !env("GITHUB_REF_NAME").isEmpty() ? env("GITHUB_REF_NAME") : git("rev-parse", "--abbrev-ref", "HEAD").trim();
        int semillaPropia = -1;
        if (!id.getOrDefault("integrante1_cedula_ultimos3", "").isEmpty() && id.get("integrante1_cedula_ultimos3").matches("\\d{3}")) semillaPropia = Integer.parseInt(id.get("integrante1_cedula_ultimos3"));
        List<Integer> semillas = new ArrayList<>(); for (int s : SEMILLAS) semillas.add(s);
        if (semillaPropia >= 0 && !semillas.contains(semillaPropia)) semillas.add(semillaPropia);

        // ---------- compilar ----------
        Path out = Files.createTempDirectory("ag-classes");
        Path src = RAIZ.resolve("src");
        List<Path> fModelo = java(src.resolve("modelo")), fApp = java(src.resolve("app"));
        // Fase 1: modelo + archivos oficiales de app (modelo usa Contexto.dinero). Fase 2: el resto de app (Main).
        List<Path> fase1 = new ArrayList<>(fModelo), fase2 = new ArrayList<>();
        for (Path p : fApp) { String n = p.getFileName().toString(); if (n.equals("Contexto.java") || n.equals("Escenario.java")) fase1.add(p); else fase2.add(p); }
        String errModelo = fModelo.isEmpty() ? "No hay archivos en src/modelo" : compilar(out, fase1, null);
        boolean okModelo = errModelo == null;
        String errApp = null; boolean okApp = false;
        if (okModelo) {
            errApp = fase2.isEmpty() ? "No hay src/app/Main.java" : compilar(out, fase2, out);
            okApp = errApp == null;
        } else errApp = "(no se compilo porque 'modelo' tiene errores)";
        if (errModelo != null) detalles.add("ERROR DE COMPILACION en src/modelo:\n" + primeras(errModelo, 12));
        if (errApp != null && okModelo) detalles.add("ERROR DE COMPILACION en src/app:\n" + primeras(errApp, 12));

        // ---------- estructura ----------
        List<Chk> chk = estructura(out, okModelo, src.resolve("app/Main.java"));
        double ptsEstr = 0; for (Chk c : chk) if (c.ok) ptsEstr += c.pts;

        // ---------- salida con multiples semillas ----------
        double[] okSec = new double[5]; int corridas = 0; List<String> fallas = new ArrayList<>();
        Map<String, Object> porSemilla = new LinkedHashMap<>();
        String lineaControl = null;
        if (okApp) {
            int timeouts = 0;
            for (int s : semillas) {
                if (timeouts >= 2) { corridas++; fallas.add("Semilla " + s + ": no se ejecuto (el programa ya agoto el tiempo en 2 semillas)."); porSemilla.put(String.valueOf(s), "00000"); continue; }
                Corrida c = correr(out, s); corridas++; if (c.timeout) timeouts++;
                LinkedHashMap<String, List<String>> esp = esperado(s);
                Map<String, List<String>> obt = partir(c.salida);
                StringBuilder res = new StringBuilder();
                for (int k = 0; k < 5; k++) {
                    String dif = diferencia(esp.get(SECCIONES[k]), obt.get(SECCIONES[k]));
                    if (dif == null) { okSec[k]++; res.append('1'); }
                    else { res.append('0'); if (fallas.stream().noneMatch(x -> x.startsWith(NOMBRES_SEC[0] + ""))) { /* no-op */ } fallas.add(NOMBRES_SEC[k] + " (semilla " + s + "): " + dif); }
                }
                if (c.timeout) fallas.add("Semilla " + s + ": el programa tardo mas de " + TIMEOUT_SEG + " s (posible ciclo infinito).");
                else if (c.codigo != 0) fallas.add("Semilla " + s + ": el programa termino con error: " + primeras(c.error, 3));
                porSemilla.put(String.valueOf(s), res.toString());
                if (s == semillaPropia) for (String l : c.salida.split("\\r?\\n")) if (l.startsWith("Control:")) lineaControl = l;
            }
        }
        double ptsSalida = 0; double[] porSec = new double[5];
        for (int k = 0; k < 5; k++) { porSec[k] = corridas == 0 ? 0 : PESOS[k] * okSec[k] / corridas; ptsSalida += porSec[k]; }
        // resumir fallas: 1 por seccion
        List<String> fallasResumen = new ArrayList<>(); Set<String> vistas = new HashSet<>();
        for (String f : fallas) { String k = f.contains(":") ? f.substring(0, f.indexOf(" (")).trim() : f; if (f.startsWith("Semilla")) fallasResumen.add(f); else if (vistas.add(k)) fallasResumen.add(f); }

        // ---------- git ----------
        String baseHash = null;
        for (String l : git("log", "--no-merges", "--format=%H%x09%s").split("\\r?\\n")) {
            int t = l.indexOf('\t'); if (t > 0 && l.substring(t + 1).startsWith("BASE:")) { baseHash = l.substring(0, t); break; }
        }
        String rango = baseHash != null ? baseHash + "..HEAD" : "HEAD";
        List<String[]> commits = new ArrayList<>();
        for (String l : git("log", "--no-merges", "--format=%H%x09%an%x09%ae%x09%at%x09%s", rango).split("\\r?\\n")) {
            String[] p = l.split("\t", 5); if (p.length == 5) commits.add(p);
        }
        double ptsGit = 0; List<Chk> gchk = new ArrayList<>();
        Chk gb = new Chk("G-rama", "Rama con formato taller4-apellido-nombre", 4);
        gb.ok = branch.matches("taller4-[a-z]+(-[a-z]+)+"); gb.nota = "rama detectada: " + branch; gchk.add(gb);
        for (int k = 1; k <= 4; k++) {
            Chk g = new Chk("G-EJ" + k, "Commit con prefijo 'EJ" + k + ":'", 3);
            for (String[] c : commits) if (c[4].matches("(?i)^\\s*EJ" + k + "\\s*:.*")) g.ok = true;
            gchk.add(g);
        }
        Chk gi = new Chk("G-ident", "Identidad Git configurada (nombre con 2 palabras y correo valido)", 4);
        boolean nomOk = false, mailOk = false;
        for (String[] c : commits) { if (c[1].trim().split("\\s+").length >= 2) nomOk = true; if (c[2].matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) mailOk = true; }
        gi.ok = nomOk && mailOk; gi.pts = 4; gchk.add(gi);
        for (Chk g : gchk) if (g.ok) ptsGit += g.pts;

        // ---------- observacion de plagio ----------
        List<String[]> flags = new ArrayList<>(); int pts = 0; // {codigo, texto, puntos}
        // 1) estructuras no vistas / hardcode / sospechoso
        Set<String> noVisto = new LinkedHashSet<>();
        boolean sospechoso = false; List<String> litEncontrados = new ArrayList<>(); int numMatch = 0;
        Set<String> nombresPool = new HashSet<>(); nombresPool.addAll(Arrays.asList(NOMBRES)); nombresPool.addAll(Arrays.asList(APELLIDOS));
        Set<String> numerosConocidos = new HashSet<>(); Set<String> enteros = new HashSet<>();
        for (int s : semillas) {
            for (int c = 1; c <= 2; c++) { enteros.add(String.valueOf(numeroCuenta(s, c))); numerosConocidos.add(d(saldoInicial(s, c))); }
            for (int o = 1; o <= 6; o++) numerosConocidos.add(d(montoOp(s, o)));
            numerosConocidos.add(d(montoTransferencia(s))); numerosConocidos.add(d(montoComision(s)));
        }
        Set<String> comunes = new HashSet<>(Arrays.asList("0.01", "0.25", "0.50", "1.00", "100.00", "500.00", "10000.00", "50.00", "5.00", "10.00"));
        numerosConocidos.removeAll(comunes);
        List<Path> estudiante = new ArrayList<>();
        for (String sub : new String[]{"modelo", "app"}) for (Path p : java(src.resolve(sub))) {
            String n = p.getFileName().toString();
            if (!(sub.equals("app") && (n.equals("Contexto.java") || n.equals("Escenario.java")))) estudiante.add(p);
        }
        for (Path p : estudiante) {
            String txt = leer(p); if (txt == null) continue;
            Limpio L = limpiar(txt); String cod = L.codigo;
            for (String[] nv : NO_VISTO) if (Pattern.compile(nv[1]).matcher(cod).find()) noVisto.add(nv[0] + " en " + p.getFileName());
            String c2 = cod.replaceAll("String\\s*\\[\\s*\\]\\s*\\w+", "").replaceAll("String\\s+\\w+\\s*\\[\\s*\\]", "");
            if (c2.contains("[")) noVisto.add("arreglos ([]) en " + p.getFileName());
            int st = contar(cod, "\\bstatic\\b") - contar(cod, "public\\s+static\\s+void\\s+main");
            if (st > 0) noVisto.add("static fuera de main en " + p.getFileName());
            Matcher im = Pattern.compile("\\bimport\\b[^;]*;").matcher(cod);
            while (im.find()) if (!im.group().matches("import\\s+(modelo|app)\\.[\\w.*]+\\s*;")) noVisto.add("import " + im.group().replaceAll("import|;", "").trim() + " en " + p.getFileName());
            if (Pattern.compile(SOSPECHOSO).matcher(cod).find()) sospechoso = true;
            for (String lit : L.literales) for (String tk : lit.split("[^A-Za-z]+")) if (nombresPool.contains(tk)) litEncontrados.add(tk);
            Matcher nm = Pattern.compile("(?<![\\w.])\\d+(\\.\\d+)?(?![\\w.])").matcher(cod);
            while (nm.find()) {
                String t = nm.group();
                if (t.contains(".")) { String f = d(Double.parseDouble(t)); if (numerosConocidos.contains(f)) numMatch++; }
                else if (t.length() == 6 && enteros.contains(t)) numMatch++;
            }
        }
        if (!noVisto.isEmpty()) { int p = Math.min(P_NO_VISTO_MAX, noVisto.size() * P_NO_VISTO_C_U); pts += p; flags.add(new String[]{"NO_VISTO", "Estructuras o funciones no vistas en clase: " + String.join("; ", noVisto), String.valueOf(p)}); }
        if (sospechoso) { pts += P_SOSPECHOSO; flags.add(new String[]{"SOSPECHOSO", "Uso de archivos, red, procesos, hilos o reflexion (nada de esto es necesario en el taller)", String.valueOf(P_SOSPECHOSO)}); }
        if (!litEncontrados.isEmpty() || numMatch >= 2) {
            pts += P_HARDCODE;
            flags.add(new String[]{"HARDCODE", "Posible hardcode: el codigo contiene como literales datos que genera Escenario (" + (litEncontrados.isEmpty() ? "" : "nombres: " + new LinkedHashSet<>(litEncontrados)) + (numMatch > 0 ? " " + numMatch + " valores numericos" : "") + ")", String.valueOf(P_HARDCODE)});
        }
        // 2) git
        if (commits.size() < MIN_COMMITS) { pts += P_POCOS_COMMITS; flags.add(new String[]{"POCOS_COMMITS", "Commits insuficientes: " + commits.size() + " (minimo " + MIN_COMMITS + ")", String.valueOf(P_POCOS_COMMITS)}); }
        long min = Long.MAX_VALUE, max = 0; for (String[] c : commits) { long t = Long.parseLong(c[3]); min = Math.min(min, t); max = Math.max(max, t); }
        long span = commits.size() < 2 ? 0 : (max - min) / 60;
        if (commits.size() >= 1 && span < MIN_MINUTOS) { pts += P_COMMITS_JUNTOS; flags.add(new String[]{"COMMITS_JUNTOS", "Commits muy juntos: " + span + " min entre el primero y el ultimo (minimo " + MIN_MINUTOS + ")", String.valueOf(P_COMMITS_JUNTOS)}); }
        if (commits.isEmpty()) flags.add(new String[]{"SIN_HISTORIAL", "No se encontraron commits del estudiante" + (baseHash == null ? " (tampoco el commit BASE)" : ""), "0"});
        Set<String> autores = new TreeSet<>(); for (String[] c : commits) autores.add(c[2].toLowerCase());
        if (autores.size() > 1) {
            String ap1 = sinTildes(id.get("integrante1_apellido")), ap2 = sinTildes(id.get("integrante2_apellido")); boolean ajenos = false;
            for (String[] c : commits) { String n = sinTildes(c[1]); if (!((!ap1.isEmpty() && n.contains(ap1)) || (!ap2.isEmpty() && n.contains(ap2)))) ajenos = true; }
            int p = autores.size() >= 3 ? P_AUTORES_3 : (ajenos ? P_AUTORES_2_AJENOS : 0);
            pts += p; flags.add(new String[]{"VARIOS_AUTORES", "Mas de un autor en los commits (" + autores.size() + ")" + (p == 0 ? " - informativo: coinciden con los 2 integrantes declarados" : ""), String.valueOf(p)});
        }
        // 3) control personal
        if (!faltan.isEmpty()) { pts += P_CONTROL; flags.add(new String[]{"CONTROL_AUSENTE", "Detalles de control personales ausentes o invalidos en identidad.txt: " + String.join(", ", faltan), String.valueOf(P_CONTROL)}); }
        else if (okApp && (lineaControl == null || !sinTildes(lineaControl).contains(sinTildes(id.get("integrante1_apellido"))))) { pts += P_CABECERA; flags.add(new String[]{"CABECERA", "La salida no muestra la linea 'Control:' con el apellido del integrante 1 (no se llamo a Contexto.imprimirCabecera?)", String.valueOf(P_CABECERA)}); }
        // 4) archivos oficiales modificados
        String hc = sha(Optional.ofNullable(leer(src.resolve("app/Contexto.java"))).orElse("")), he = sha(Optional.ofNullable(leer(src.resolve("app/Escenario.java"))).orElse(""));
        List<String> ofMod = new ArrayList<>();
        if (!HASH_CONTEXTO.equals("PENDIENTE") && !hc.equals(HASH_CONTEXTO)) ofMod.add("Contexto.java");
        if (!HASH_ESCENARIO.equals("PENDIENTE") && !he.equals(HASH_ESCENARIO)) ofMod.add("Escenario.java");
        if (!ofMod.isEmpty()) { pts += P_OFICIALES_MOD; flags.add(new String[]{"OFICIALES_MODIFICADOS", "Archivos oficiales modificados: " + String.join(", ", ofMod), String.valueOf(P_OFICIALES_MOD)}); }
        String agFuente = env("AG_FUENTE"), agMod = env("AG_MODIFICADO"), wfMod = env("WF_MODIFICADO");
        if (agMod.equals("si")) { pts += P_AUTOGRADE_MOD; flags.add(new String[]{"AUTOGRADE_MODIFICADO", "autograde.java del estudiante difiere del oficial", String.valueOf(P_AUTOGRADE_MOD)}); }
        if (wfMod.equals("si")) { pts += P_WORKFLOW_MOD; flags.add(new String[]{"WORKFLOW_MODIFICADO", "autograde.yml del estudiante difiere del oficial", String.valueOf(P_WORKFLOW_MOD)}); }
        if (agFuente.equals("local")) flags.add(new String[]{"SIN_VERIFICAR_OFICIAL", "No se pudo descargar la version oficial; se uso la copia local (no se pudo comparar)", "0"});
        String nivel = pts == 0 ? "NINGUNA" : pts <= 2 ? "BAJA" : pts <= 5 ? "MEDIA" : "ALTA";

        double nota = Math.round((ptsSalida + ptsEstr + ptsGit) * 10) / 10.0;

        // ---------- reporte ----------
        StringBuilder md = new StringBuilder();
        md.append("# Autograde Taller 4 - Programacion 1\n\n");
        md.append("**Rama:** `").append(branch).append("`  |  **Semilla:** ").append(semillaPropia >= 0 ? semillaPropia : "(sin cedula valida)").append("\n\n");
        md.append("## NOTA: ").append(nota).append(" / 100").append(nota >= NOTA_MINIMA ? "  (aprobado)" : "  (por debajo de " + NOTA_MINIMA + ")").append("\n\n");
        md.append("| Componente | Puntos |\n|---|---|\n");
        md.append("| Salida correcta (").append(corridas).append(" semillas) | ").append(r1(ptsSalida)).append(" / 50 |\n");
        for (int k = 0; k < 5; k++) md.append("| - ").append(NOMBRES_SEC[k]).append(" | ").append(r1(porSec[k])).append(" / ").append((int) PESOS[k]).append(" |\n");
        md.append("| Estructura pedida | ").append(r1(ptsEstr)).append(" / 30 |\n| Git | ").append(r1(ptsGit)).append(" / 20 |\n\n");
        md.append("### Estructura\n");
        for (Chk c : chk) md.append(c.ok ? "- [x] " : "- [ ] ").append(c.id).append(" ").append(c.desc).append(" (").append((int) c.pts).append(")\n");
        md.append("\n### Git\n");
        for (Chk c : gchk) md.append(c.ok ? "- [x] " : "- [ ] ").append(c.desc).append(" (").append((int) c.pts).append(")").append(c.nota.isEmpty() ? "" : " - " + c.nota).append("\n");
        md.append("- Commits del estudiante: ").append(commits.size()).append(" | tiempo entre primero y ultimo: ").append(span).append(" min\n");
        if (!detalles.isEmpty() || !fallasResumen.isEmpty()) {
            md.append("\n### Que corregir\n");
            for (String x : detalles) md.append("```\n").append(x).append("\n```\n");
            for (String x : fallasResumen) md.append("- ").append(x).append("\n");
        }
        md.append("\n## Observacion de plagio: ").append(nivel).append(" (").append(pts).append(" pts)\n");
        md.append("_Son INDICIOS, no pruebas. Solo el profesor decide, tras la revision y la defensa oral._\n\n");
        if (flags.isEmpty()) md.append("- Sin banderas.\n");
        for (String[] f : flags) md.append("- **").append(f[0]).append("** (+").append(f[2]).append("): ").append(f[1]).append("\n");
        md.append("\n_autograde v").append(VERSION).append("  |  version del autograde: ").append(agFuente.isEmpty() ? "local (ejecucion fuera de Actions)" : agFuente).append("_\n");
        System.out.println(md.toString().replace("**", "").replace("`", ""));

        // ---------- JSON ----------
        Map<String, Object> J = new LinkedHashMap<>();
        J.put("version", VERSION); J.put("fecha", Instant.now().toString()); J.put("rama", branch);
        Map<String, Object> integ = new LinkedHashMap<>();
        for (String k : claves) integ.put(k, id.getOrDefault(k, ""));
        J.put("identidad", integ);
        String n1 = (id.getOrDefault("integrante1_nombre", "") + " " + id.getOrDefault("integrante1_apellido", "")).trim();
        String n2 = (id.getOrDefault("integrante2_nombre", "") + " " + id.getOrDefault("integrante2_apellido", "")).trim();
        J.put("nombreCompleto", n1 + " y " + n2);
        J.put("semilla", semillaPropia); J.put("nota", nota); J.put("aprobado", nota >= NOTA_MINIMA);
        Map<String, Object> desg = new LinkedHashMap<>(); desg.put("salida", Math.round(ptsSalida * 10) / 10.0); desg.put("estructura", ptsEstr); desg.put("git", ptsGit); J.put("desglose", desg);
        Map<String, Object> secs = new LinkedHashMap<>(); for (int k = 0; k < 5; k++) secs.put(NOMBRES_SEC[k], Math.round(porSec[k] * 10) / 10.0); J.put("salidaPorParte", secs);
        J.put("seedsEjecutadas", semillas); J.put("resultadoPorSemilla", porSemilla);
        Map<String, Object> obs = new LinkedHashMap<>(); obs.put("nivel", nivel); obs.put("puntos", pts);
        List<Object> fl = new ArrayList<>(); for (String[] f : flags) { Map<String, Object> m = new LinkedHashMap<>(); m.put("codigo", f[0]); m.put("detalle", f[1]); m.put("puntos", Integer.parseInt(f[2])); fl.add(m); }
        obs.put("banderas", fl); obs.put("aviso", "Son indicios, no pruebas."); J.put("observacionPlagio", obs);
        J.put("commits", commits.size()); J.put("minutosEntrePrimeroYUltimo", span);
        J.put("autogradeFuente", agFuente.isEmpty() ? "local" : agFuente);
        Files.write(RAIZ.resolve("resultado_autograde.json"), json(J, 0).getBytes(StandardCharsets.UTF_8));
        String sum = env("GITHUB_STEP_SUMMARY");
        if (!sum.isEmpty()) Files.write(Paths.get(sum), (md + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return nota >= NOTA_MINIMA ? 0 : 1;
    }

    static String r1(double v) { return String.format(Locale.US, "%.1f", v); }
    static String primeras(String s, int n) { String[] l = s.split("\\r?\\n"); return String.join("\n", Arrays.copyOf(l, Math.min(n, l.length))); }

    @SuppressWarnings("unchecked")
    static String json(Object o, int ind) {
        String pad = "  ".repeat(ind + 1), end = "  ".repeat(ind);
        if (o == null) return "null";
        if (o instanceof String) { StringBuilder sb = new StringBuilder("\""); for (char c : ((String) o).toCharArray()) { if (c == '"') sb.append("\\\""); else if (c == '\\') sb.append("\\\\"); else if (c == '\n') sb.append("\\n"); else if (c == '\r') {} else if (c == '\t') sb.append("\\t"); else if (c < 32) sb.append(' '); else sb.append(c); } return sb.append('"').toString(); }
        if (o instanceof Number || o instanceof Boolean) return o.toString();
        if (o instanceof Map) { Map<String, Object> m = (Map<String, Object>) o; if (m.isEmpty()) return "{}"; StringBuilder sb = new StringBuilder("{\n"); int i = 0; for (Map.Entry<String, Object> e : m.entrySet()) { sb.append(pad).append(json(e.getKey(), 0)).append(": ").append(json(e.getValue(), ind + 1)).append(++i < m.size() ? ",\n" : "\n"); } return sb.append(end).append("}").toString(); }
        if (o instanceof List) { List<Object> l = (List<Object>) o; if (l.isEmpty()) return "[]"; StringBuilder sb = new StringBuilder("[\n"); for (int i = 0; i < l.size(); i++) sb.append(pad).append(json(l.get(i), ind + 1)).append(i + 1 < l.size() ? ",\n" : "\n"); return sb.append(end).append("]").toString(); }
        return json(o.toString(), ind);
    }
}
