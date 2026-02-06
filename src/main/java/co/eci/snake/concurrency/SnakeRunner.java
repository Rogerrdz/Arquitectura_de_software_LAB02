package co.eci.snake.concurrency;

import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Snake;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

public final class SnakeRunner implements Runnable {
  private final Snake snake;
  private final Board board;
  private final int baseSleepMs = 80;
  private final int turboSleepMs = 40;
  private int turboTicks = 0;
  private final Lock lock = new ReentrantLock();
  private final Condition canRun = lock.newCondition();
  private volatile boolean paused = false;

  public SnakeRunner(Snake snake, Board board) {
    this.snake = snake;
    this.board = board;
  }

  @Override
  public void run() {
    try {
      while (!Thread.currentThread().isInterrupted()) {
    
        lock.lock();
        try {
          while (paused) {
            canRun.await(); 
          }
        } finally {
          lock.unlock();
        }
    
        maybeTurn();
        var res = board.step(snake);
    
        if (res == Board.MoveResult.HIT_OBSTACLE) {
          randomTurn();
        } else if (res == Board.MoveResult.ATE_TURBO) {
          turboTicks = 100;
        }
    
        int waitTime = (turboTicks > 0) ? turboSleepMs : baseSleepMs;
        if (turboTicks > 0) turboTicks--;
    
        lock.lock();
        try {
          canRun.await(waitTime, TimeUnit.MILLISECONDS); 
        } finally {
          lock.unlock();
        }
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  private void maybeTurn() {
    double p = (turboTicks > 0) ? 0.05 : 0.10;
    if (ThreadLocalRandom.current().nextDouble() < p) randomTurn();
  }

  private void randomTurn() {
    var dirs = Direction.values();
    snake.turn(dirs[ThreadLocalRandom.current().nextInt(dirs.length)]);
  }

  public void pause() {
    lock.lock();
    try {
      paused = true;
    } finally {
      lock.unlock();
    }
  }
  
  public void resume() {
    lock.lock();
    try {
      paused = false;
      canRun.signalAll();
    } finally {
      lock.unlock();
    }
  }
  
}
