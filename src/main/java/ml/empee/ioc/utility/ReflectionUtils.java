package ml.empee.ioc.utility;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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

  public static Object newInstance(Constructor<?> constructor, Object... args) {
    try {
      return constructor.newInstance(args);
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new IocException("Unable to create bean" + constructor.getDeclaringClass().getName(), e);
    }
  }

}
