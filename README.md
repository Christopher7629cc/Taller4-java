# Taller 4 - Programación 1 (Java)

**Tema:** 2.6 Modificadores de acceso · 2.7 Declaración y creación de un objeto · 2.8 Acceso a datos y métodos
**Modalidad:** grupos de 2 · **Duración:** 60+ minutos · **Entrega:** Pull Request hacia `SarmientoEdu/Taller4-java`

> Las instrucciones completas (Git paso a paso, ejercicios, formatos de salida EXACTOS, rúbrica,
> integridad académica) están en el **Documento del estudiante** que entrega el profesor.
> Este README es solo la referencia rápida.

## Estructura

```
identidad.txt              <- COMPLETAR (datos del equipo; de aquí sale la semilla)
src/modelo/Titular.java        EJ1   (TODO)
src/modelo/CuentaAhorro.java   EJ2   (TODO)
src/modelo/Banco.java          EJ3   (TODO)
src/app/Main.java              EJ4   (TODO)
src/app/Contexto.java          OFICIAL - no modificar
src/app/Escenario.java         OFICIAL - no modificar
autograde.java                 OFICIAL - no modificar
.github/workflows/autograde.yml  OFICIAL - no modificar
compilar_y_ejecutar.sh / .bat  ayudas para compilar y correr
```

## Compilar y ejecutar

```
Linux/Mac:  ./compilar_y_ejecutar.sh          (o:  ./compilar_y_ejecutar.sh 123  para probar la semilla 123)
Windows:    compilar_y_ejecutar.bat
```

## Autograde local (JDK 17+; nada más que instalar)

```
java autograde.java
```
Escribe `resultado_autograde.json` (ignorado por Git). No envía nada a ningún servidor.

## Entrega (resumen)

1. Fork de este repo → habilitar Actions en su fork → clonar.
2. Crear la rama `taller4-apellido-nombre` (en minúsculas, sin tildes).
3. Commits con prefijo por ejercicio: `EJ1: ...`, `EJ2: ...`, `EJ3: ...`, `EJ4: ...`.
4. `git push` de la rama y abrir un **Pull Request** hacia `SarmientoEdu/Taller4-java` (rama `main`). **Nunca se fusiona.**
5. El resultado aparece en la pestaña *Checks* → *Autograde Taller 4* → *Summary*. Nota mínima para aprobar el chequeo: 60.
