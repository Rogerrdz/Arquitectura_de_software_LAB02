# Snake Race — ARSW Lab #2 (Java 21, Virtual Threads)
### Autor : Roger Rodriguez
----
### Parte I - wait/notify en un programa multihilo
  1. Modificamos el programa prime finder paara que cada t milisegundos :
  Se pausen los hilos pasados t milisegundos ; Esto lo realizamos con un ciclo que este infinitamente ejecutandos y que el mismo duerma a los hilos pasados los t milisegundos , muestre la cantidad de primos encontrados que estos van a ser iguales a la longitud de la lista donde se almacenan y espere a el salto de linea (ENTER):
     

```phyton

while (true) {
            try {
                Thread.sleep(TMILISECONDS);
            } catch (InterruptedException e) {
                break;
            }

            int totalPrimes = 0;
            for (PrimeFinderThread t : pft) {
                totalPrimes += t.getPrimesCount();
            }

            System.out.println("\nPrimes found so far: " + totalPrimes);
            System.out.println("Press ENTER to continue...");
            scanner.nextLine();

            synchronized (lock) {
                lock.notifyAll(); 
            }
        } 

```
  2. La sincronización usa synchronized, wait(), notify() / notifyAll() sobre el mismo monitor (sin busy-waiting).
  3.  observaciones y/o comentarios explicando el diseño de sincronización:
      - LOCK
        - Utilizamos un lock compartido :
        ```phyton
          private final Object lock = new Object();
        ```
        - Todos dentro de un bloqueo:
        ```phyton
          synchronized (lock) {}
        ```
        - De esta manera garantizamos la exclusion mutua y la visibilidad de los cambios entre los hilos
      - Condicion
        - La condicion que maneja la logica es una variable de tipo booleano que cada hilo verifica antes de procesar cada numero (true = los hilos detienen su ejecucion , false = los hilos pueden continuar):
        ```phyton
          private boolean paused = false;
        ```
      - Wait()
        ```phyton
            synchronized (lock) {
              while (paused) {
                 lock.wait();
              }
            }
        ```
        cada hilo detecta que paused = true y entra en estado de espera , esto evita el busy-waiting 

      - NotifyAll()
        ```phyton
            synchronized (lock) {
                paused = false;
                lock.notifyAll();
            }
        ```
        Utilizamos notifyAll() por que existen muchos hilos esperando y deben de ser activados correctamente

      - Prevencion de los lost wakeups se realiza ya que siempre se evalua la condicion dentro de un bloque de sincronizacion y se modifica la condicion paused antes de llamar al notifyAll() 
      - 
Un pequeño ejemplo del funcionamiento con pocos milisegundos por que los hilos encuentran los numero muy rapido :

![PrimeFinder](assets/PrimeFinder.png)

### Parte II - SnakeRace concurrente (núcleo del laboratorio)

1. Analisis de Concurrencia
  1. Como se usan los hilos ?  
  Cada serpiente del juego es controlada por un hilo independiente representado por la clase SnakeRunner, dandole la autonomia en el movimiento y comportamiento.
  El hilo se ejecuta continuamente siempre y cuando no sea interrumpido antes de eso deside si gira , intenta ,moverse , puede chocar o duerme un tiempo 
  2.  Posibles condiciones de carrera:
  - El tablero(Board) es compartido por todos los hilos SnakeRunner. El metodo step() es invocado concurrentemente, lo que puede generar condiciones de carrera si el acceso a el mismo no esta sincronizado  
  - Multiples hilos acceden a Snake (SnakeRunner y UI), lo que puede producir lecturas inconsistentes si no se protege el estado.
  3. Colecciones no seguras:
  Las estructuras internas del Board pueden ser colecciones no seguras para concurrencia, accedidas simultaneamente por varios hilos SnakeRunner.
  4. Espera activa:
  - El hilo no presenta espera activa, ya que la temporizacion se maneja mediante Thread.sleep().
  - El codigo no implementa mecanismos de sincronizacion, dejandole el correcto manejo al diseño del tablero y de las clases compartidas.
    
**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Laboratorio de programación concurrente: condiciones de carrera, sincronización y colecciones seguras.

---

## Requisitos

- **JDK 21** (Temurin recomendado)
- **Maven 3.9+**
- SO: Windows, macOS o Linux

---

## Cómo ejecutar

```bash
mvn clean verify
mvn -q -DskipTests exec:java -Dsnakes=4
```

- `-Dsnakes=N` → inicia el juego con **N** serpientes (por defecto 2).
- **Controles**:
  - **Flechas**: serpiente **0** (Jugador 1).
  - **WASD**: serpiente **1** (si existe).
  - **Espacio** o botón **Action**: Pausar / Reanudar.
---

## Reglas del juego (resumen)

- **N serpientes** corren de forma autónoma (cada una en su propio hilo).
- **Ratones**: al comer uno, la serpiente **crece** y aparece un **nuevo obstáculo**.
- **Obstáculos**: si la cabeza entra en un obstáculo hay **rebote**.
- **Teletransportadores** (flechas rojas): entrar por uno te **saca por su par**.
- **Rayos (Turbo)**: al pisarlos, la serpiente obtiene **velocidad aumentada** temporal.
- Movimiento con **wrap-around** (el tablero “se repite” en los bordes).
---

## Arquitectura (carpetas)


```
co.eci.snake
├─ app/                 # Bootstrap de la aplicación (Main)
├─ core/                # Dominio: Board, Snake, Direction, Position
├─ core/engine/         # GameClock (ticks, Pausa/Reanudar)
├─ concurrency/         # SnakeRunner (lógica por serpiente con virtual threads)
└─ ui/legacy/           # UI estilo legado (Swing) con grilla y botón Action
```

---

# Actividades del laboratorio

## Parte I — (Calentamiento) `wait/notify` en un programa multi-hilo

1. Toma el programa [**PrimeFinder**](https://github.com/ARSW-ECI/wait-notify-excercise).
2. Modifícalo para que **cada _t_ milisegundos**:
   - Se **pausen** todos los hilos trabajadores.
   - Se **muestre** cuántos números primos se han encontrado.
   - El programa **espere ENTER** para **reanudar**.
3. La sincronización debe usar **`synchronized`**, **`wait()`**, **`notify()` / `notifyAll()`** sobre el **mismo monitor** (sin _busy-waiting_).
4. Entrega en el reporte de laboratorio **las observaciones y/o comentarios** explicando tu diseño de sincronización (qué lock, qué condición, cómo evitas _lost wakeups_).

> Objetivo didáctico: practicar suspensión/continuación **sin** espera activa y consolidar el modelo de monitores en Java.

---

## Parte II — SnakeRace concurrente (núcleo del laboratorio)

### 1) Análisis de concurrencia

- Explica **cómo** el código usa hilos para dar autonomía a cada serpiente.
- **Identifica** y documenta en **`el reporte de laboratorio`**:
  - Posibles **condiciones de carrera**.
  - **Colecciones** o estructuras **no seguras** en contexto concurrente.
  - Ocurrencias de **espera activa** (busy-wait) o de sincronización innecesaria.

### 2) Correcciones mínimas y regiones críticas

- **Elimina** esperas activas reemplazándolas por **señales** / **estados** o mecanismos de la librería de concurrencia.
- Protege **solo** las **regiones críticas estrictamente necesarias** (evita bloqueos amplios).
- Justifica en **`el reporte de laboratorio`** cada cambio: cuál era el riesgo y cómo lo resuelves.

### 3) Control de ejecución seguro (UI)

- Implementa la **UI** con **Iniciar / Pausar / Reanudar** (ya existe el botón _Action_ y el reloj `GameClock`).
- Al **Pausar**, muestra de forma **consistente** (sin _tearing_):
  - La **serpiente viva más larga**.
  - La **peor serpiente** (la que **primero murió**).
- Considera que la suspensión **no es instantánea**; coordina para que el estado mostrado no quede “a medias”.

### 4) Robustez bajo carga

- Ejecuta con **N alto** (`-Dsnakes=20` o más) y/o aumenta la velocidad.
- El juego **no debe romperse**: sin `ConcurrentModificationException`, sin lecturas inconsistentes, sin _deadlocks_.
- Si habilitas **teleports** y **turbo**, verifica que las reglas no introduzcan carreras.

> Entregables detallados más abajo.

---

## Entregables

1. **Código fuente** funcionando en **Java 21**.
2. Todo de manera clara en **`**el reporte de laboratorio**`** con:
   - Data races encontradas y su solución.
   - Colecciones mal usadas y cómo se protegieron (o sustituyeron).
   - Esperas activas eliminadas y mecanismo utilizado.
   - Regiones críticas definidas y justificación de su **alcance mínimo**.
3. UI con **Iniciar / Pausar / Reanudar** y estadísticas solicitadas al pausar.

---

## Criterios de evaluación (10)

- (3) **Concurrencia correcta**: sin data races; sincronización bien localizada.
- (2) **Pausa/Reanudar**: consistencia visual y de estado.
- (2) **Robustez**: corre **con N alto** y sin excepciones de concurrencia.
- (1.5) **Calidad**: estructura clara, nombres, comentarios; sin _code smells_ obvios.
- (1.5) **Documentación**: **`reporte de laboratorio`** claro, reproducible;

---

## Tips y configuración útil

- **Número de serpientes**: `-Dsnakes=N` al ejecutar.
- **Tamaño del tablero**: cambiar el constructor `new Board(width, height)`.
- **Teleports / Turbo**: editar `Board.java` (métodos de inicialización y reglas en `step(...)`).
- **Velocidad**: ajustar `GameClock` (tick) o el `sleep` del `SnakeRunner` (incluye modo turbo).

---

## Cómo correr pruebas

```bash
mvn clean verify
```

Incluye compilación y ejecución de pruebas JUnit. Si tienes análisis estático, ejecútalo en `verify` o `site` según tu `pom.xml`.

---

## Créditos

Este laboratorio es una adaptación modernizada del ejercicio **SnakeRace** de ARSW. El enunciado de actividades se conserva para mantener los objetivos pedagógicos del curso.

**Base construida por el Ing. Javier Toquica.**
