package ml.empee.ioc.utility;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import ml.empee.ioc.IocException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReflectionUtils {

  public static Class<?> getCallerClass() {
    String clazzName = Thread.currentThread().getStackTrace()[3].getClassName();
    try {
      return Class.forName(clazzName);
    } catch (ClassNotFoundException e) {
      throw new IocException("Unable to find the caller class", e);
    }
  }

  @SneakyThrows
  public static Object safeInvoke(Object source, Method method, Object... args) {
    try {
      return method.invoke(source);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  @SneakyThrows
  public static Object newInstance(Constructor<?> constructor, Object... args) {
    try {
      return constructor.newInstance(args);
    } catch (InvocationTargetException e) {
      throw new IocException("Unable to instantiate " + constructor.getDeclaringClass().getName(), e.getCause());
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IocException("Unable to instantiate " + constructor.getDeclaringClass().getName(), e);
    }
  }

}
