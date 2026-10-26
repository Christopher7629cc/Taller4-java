#!/bin/sh
# Uso:  ./compilar_y_ejecutar.sh [semilla]
# Si no indica semilla, usa los 3 ultimos digitos de la cedula del integrante 1 (identidad.txt).
set -e
rm -rf out && mkdir out
javac -encoding UTF-8 -d out src/modelo/*.java src/app/*.java
java -cp out app.Main "$@"
