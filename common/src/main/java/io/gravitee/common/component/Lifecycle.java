package io.gravitee.common.component;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class Lifecycle {

  public static enum State {
    INITIALIZED,
    STOPPED,
    STARTED,
    CLOSED
  }

  private volatile State state = State.INITIALIZED;

  public State state() {
    return this.state;
  }

  /**
   * Returns <tt>true</tt> if the state is initialized.
   */
  public boolean initialized() {
    return state == State.INITIALIZED;
  }

  /**
   * Returns <tt>true</tt> if the state is started.
   */
  public boolean started() {
    return state == State.STARTED;
  }

  /**
   * Returns <tt>true</tt> if the state is stopped.
   */
  public boolean stopped() {
    return state == State.STOPPED;
  }

  /**
   * Returns <tt>true</tt> if the state is closed.
   */
  public boolean closed() {
    return state == State.CLOSED;
  }

  public boolean moveToStarted() {
    State localState = this.state;
    if (localState == State.INITIALIZED || localState == State.STOPPED) {
      state = State.STARTED;
      return true;
    }
    if (localState == State.STARTED) {
      return false;
    }
    if (localState == State.CLOSED) {
      throw new IllegalStateException("Can't move to started state when closed");
    }
    throw new IllegalStateException("Can't move to started with unknown state");
  }

  public boolean moveToStopped() {
    State localState = state;
    if (localState == State.STARTED) {
      state = State.STOPPED;
      return true;
    }
    if (localState == State.INITIALIZED || localState == State.STOPPED) {
      return false;
    }
    if (localState == State.CLOSED) {
      throw new IllegalStateException("Can't move to started state when closed");
    }
    throw new IllegalStateException("Can't move to started with unknown state");
  }

  @Override
  public String toString() {
    return state.toString();
  }
}
