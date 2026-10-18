#!/usr/bin/env bash
# Prueba de humo del JAR (A9): lo copia a una carpeta vacía, fuera del repositorio, lo arranca con
# un solo comando (java -jar) y revisa que la app responda como en la demo. Solo necesita Java 21,
# bash y curl: nada de Maven ni del código fuente.
#
# Uso (Linux, macOS o Git Bash en Windows):
#   ./mvnw package
#   scripts/smoke-test-jar.sh                       # perfil normal, puerto 8085
#   scripts/smoke-test-jar.sh --demo                # con los datos de demostración
#   PORT=9090 scripts/smoke-test-jar.sh --demo
#
# Termina con código 0 si todo pasa y 1 si algo falla. Siempre detiene la app y borra la carpeta.

set -u

PORT="${PORT:-8085}"
PROFILE=""
if [ "${1:-}" = "--demo" ]; then
    PROFILE="demo"
fi

JAR="$(ls target/inventiq-*.jar 2>/dev/null | grep -v -- '-plain' | head -n 1)"
if [ -z "$JAR" ]; then
    echo "No hay JAR en target/. Ejecuta primero ./mvnw package" >&2
    exit 1
fi

WORK="$(mktemp -d)"
BASE="http://localhost:$PORT"
COOKIES="$WORK/cookies.txt"
LOG="$WORK/app.log"
FAILURES=0
APP_PID=""

cleanup() {
    if [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null; then
        kill "$APP_PID" 2>/dev/null
        wait "$APP_PID" 2>/dev/null
    fi
    rm -rf "$WORK"
}
trap cleanup EXIT

check() { # descripción, condición (0 = pasa)
    if [ "$2" -eq 0 ]; then
        echo "  OK    $1"
    else
        echo "  FALLA $1"
        FAILURES=$((FAILURES + 1))
    fi
}

cp "$JAR" "$WORK/inventiq.jar"
cd "$WORK" || exit 1
echo "Carpeta limpia: $WORK"
echo "Java: $(java -version 2>&1 | head -n 1)"

ARGS=(--server.port="$PORT")
if [ -n "$PROFILE" ]; then
    ARGS+=(--spring.profiles.active="$PROFILE")
fi
echo "Comando: java -jar inventiq.jar ${ARGS[*]}"
START=$(date +%s)
java -jar inventiq.jar "${ARGS[@]}" > "$LOG" 2>&1 &
APP_PID=$!

# Espera a que responda (máximo 120 s) o a que el proceso termine solo.
READY=1
for _ in $(seq 1 120); do
    if curl -s -o /dev/null "$BASE/login"; then
        READY=0
        break
    fi
    if ! kill -0 "$APP_PID" 2>/dev/null; then
        break
    fi
    sleep 1
done
check "arranca con un solo comando ($(( $(date +%s) - START )) s)" "$READY"
if [ "$READY" -ne 0 ]; then
    echo "--- últimas líneas del log ---"
    tail -n 40 "$LOG"
    exit 1
fi

status() { curl -s -o /dev/null -w "%{http_code}" -b "$COOKIES" -c "$COOKIES" "$@"; }
location() { curl -s -o /dev/null -w "%{redirect_url}" -b "$COOKIES" -c "$COOKIES" "$@"; }

LOGIN_PAGE="$(curl -s -c "$COOKIES" "$BASE/login")"
echo "$LOGIN_PAGE" | grep -q "Iniciar sesión"; check "la página de login se muestra" $?
[ "$(status "$BASE/css/app.css")" = "200" ]; check "los estilos se sirven desde el JAR" $?
[ "$(status "$BASE/webjars/chart.js/4.5.1/dist/chart.umd.min.js")" = "200" ]; check "Chart.js se sirve desde el JAR" $?
# Sin cookies: si usara la misma sesión, Spring recordaría /pos y llevaría ahí después del login.
[ "$(curl -s -o /dev/null -w "%{redirect_url}" "$BASE/pos")" = "$BASE/login" ]; check "sin sesión, Ventas manda al login" $?

TOKEN="$(echo "$LOGIN_PAGE" | grep -o 'name="_csrf" value="[^"]*"' | sed 's/.*value="//;s/"$//')"
[ "$(location --data-urlencode username=admin --data-urlencode password=admin123 \
    --data-urlencode "_csrf=$TOKEN" "$BASE/login")" = "$BASE/" ]; check "el administrador inicia sesión" $?
curl -s -b "$COOKIES" "$BASE/" | grep -q "Ventas de hoy"; check "el inicio del administrador carga" $?
[ "$(status "$BASE/admin/history")" = "200" ]; check "el historial carga" $?

if [ -n "$PROFILE" ]; then
    curl -s -b "$COOKIES" "$BASE/inventory" | grep -q "Yogur de fresa"; check "trae los datos de demostración" $?
    [ -d data ] && ls data | grep -q "inventiq-demo"; check "crea la base demo junto al JAR (./data)" $?
else
    [ -d data ] && ls data | grep -q "^inventiq.mv.db$"; check "crea la base junto al JAR (./data)" $?
fi

grep -q "Using generated security password" "$LOG"; [ $? -ne 0 ]; check "no crea usuarios por defecto de Spring" $?
grep -E "^[0-9T:.+-]+ +ERROR" "$LOG" > /dev/null; [ $? -ne 0 ]; check "el log no tiene errores" $?

echo
if [ "$FAILURES" -eq 0 ]; then
    echo "Prueba de humo del JAR: todo pasó."
    exit 0
fi
echo "Prueba de humo del JAR: $FAILURES comprobaciones fallaron."
echo "--- últimas líneas del log ---"
tail -n 40 "$LOG"
exit 1
