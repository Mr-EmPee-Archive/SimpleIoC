package ml.empee.ioc;

public interface Bean {
  default void onStop() {}
  default void onStart() {}
  default boolean isEnabled() { return true; }
}
