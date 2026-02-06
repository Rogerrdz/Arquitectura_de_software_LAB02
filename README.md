# Snake Race — ARSW Lab #2 (Java 21, Virtual Threads)
### Autor : Roger Rodriguez
**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Laboratorio de programación concurrente: condiciones de carrera, sincronización y colecciones seguras.
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

**1. Analisis de Concurrencia**
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
    
**2. Correcciones mínimas y regiones críticas**
  - En el metodo run del hilo SnakeRunner tenemos:
  Que este se mueve y luego se duerme activamente no responde inmediatamente a pausa / reanudar y visualmente nos damos cuenta ya que al pausar el programa se congelan visualmente pero por detras se sigue moviendo ademas que la pausa no es inmediata y el estado del juego queda a medias:

  ```phyton
      while (!Thread.currentThread().isInterrupted()) {
      board.step(snake);
      Thread.sleep(sleep);
      }
  ```

  Agregamos un lock y una condicion :

  ```phyton
      while (running) {
      waitIfPaused();
      board.step(snake);
      waitForNextTick();
      }

  ``` 

  Lo resolvemos utilizamos mecanismos de concurrencia como lo es wait/notify y la condicion , en lugar desde que se duerma el hilo este espera una señal.

  - En el tablero tenemos la funcion step() ya que esta modifica un estado compartido de raton ,obstaculo , el turbo y los teletransportes por lo cual podemos tener e problema como ejemplo que dos serpientes se coman un mismo raton o un simple estado inconsistente del tablero:
  ```
    mice.remove(next);
    mice.add(randomEmpty());
    obstacles.add(randomEmpty());
    turbo.remove(next);
    teleports.get(next);   
  ```
  Se corrige alteraldo mice , obstacles , turbo y teleports 
  - En el acceso desde la UI al tablero se consulta los ratones , obstaculos  , turbo y teletransportes es un problema de lectura ya que los hilos tamben acceden a leer esto :

  
```
  board.mice()
  board.obstacles()
  board.turbo()
  board.teleports()
```

  La UI accede a copias defensivas y no al estado real por lo cual no hay escritura concurrente eliminando las modificaciones concurrentes 

  ```
    return new HashSet<>(mice);
  ```
  - Cambiamos la funcionde TogglePause en SnakeApp ay que necesitamos pueda pausar y reanudar cada hilo , asi como pausar tods cuando le damos al boton.
  ```
    private void togglePause() {
        if ("Action".equals(actionButton.getText())) {
          actionButton.setText("Resume");
          clock.pause();
        } else {
          actionButton.setText("Action");
          clock.resume();
        }
      }
  ```
    Agregamoso una lista que almacene los Runners asi como la parte donde se crean y se guardan todos los runners 
  ```
    private final List<SnakeRunner> runners = new ArrayList<>();
    var exec = Executors.newVirtualThreadPerTaskExecutor();
    snakes.forEach(s -> {
        SnakeRunner r = new SnakeRunner(s, board);
        runners.add(r);
        exec.submit(r);
    });
  
  ``` 
    Tendriamos la siguiente nueva funcion TogglePause 

  ```
    private void togglePause() {
    if ("Action".equals(actionButton.getText())) {
        actionButton.setText("Resume");
        clock.pause();
        runners.forEach(SnakeRunner::pause);
        try {
            for (SnakeRunner r : runners) {
                r.waitUntilPaused();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        showStatistics();
    } else {
        actionButton.setText("Action");
        clock.resume();
        runners.forEach(SnakeRunner::resume);
    }
    }

  ````
**3.Control de ejecución seguro (UI)** 

Con lo trabajamos en el punto anterior continuamos con la implementacion y para esto creamos la clase showStadistics() para msitrar estadisticas cuando se pause el juego , estan muestran la serpiente mas larga y la mas corta:

  ```
  private void showStatistics() {
    if (snakes.isEmpty()) return;
    Snake longest = snakes.stream()
        .max(Comparator.comparingInt(s -> s.snapshot().size()))
        .orElse(null);
    Snake worst = snakes.stream()
        .min(Comparator.comparingInt(s -> s.snapshot().size()))
        .orElse(null);

    String message = String.format(
        "Serpiente más larga: %d bloques\nPeor serpiente: %d bloques",
        (longest != null ? longest.snapshot().size() : 0),
        (worst != null ? worst.snapshot().size() : 0)
    );

    JOptionPane.showMessageDialog(this, message, "Estadisticas", JOptionPane.INFORMATION_MESSAGE);
    }

  ```
**4.Robustez bajo carga**
Se deben ejecutar las pruebas usando :
  ```
  mvn -q -DskipTests exec:java -Dsnakes=20
  ```
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
