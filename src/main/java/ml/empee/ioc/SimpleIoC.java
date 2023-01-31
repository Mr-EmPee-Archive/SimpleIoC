package ml.empee.ioc;

import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import ml.empee.ioc.annotations.Bean;
import ml.empee.ioc.annotations.InversionOfControl;
import ml.empee.ioc.utility.ReflectionUtils;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Simple IoC container for Bukkit plugins. <br>
 * Use {@link #initialize(String...)} to initialize the container. <br>
 */
public final class SimpleIoC {

  private final JavaPlugin plugin;
  private final List<Object> beans = new ArrayList<>();

  private static Constructor<?> findBeanConstructor(Class<?> bean) throws IocException {
    if(!bean.isAnnotationPresent(Bean.class)) {
      throw new IocException("The class " + bean.getName() + " isn't a bean");
    }

    Constructor<?>[] constrcutors = bean.getConstructors();
    if(constrcutors.length == 1) {
      return constrcutors[0];
    }

    for(Constructor<?> constructor : constrcutors) {
      if(constructor.isAnnotationPresent(InversionOfControl.class)) {
        return constructor;
      }
    }

    throw new IocException("Unable to find a constructor for the bean " + bean.getName());
  }

  private static List<Class<?>> findAllBeans(ClassPath classPath, String packageToScan,  List<String> exclusions) {
    return classPath.getAllClasses().stream()
        .filter(c -> c.getPackageName().startsWith(packageToScan))
        .filter(c -> exclusions.stream().noneMatch(e -> c.getPackageName().startsWith(packageToScan + "." + e)))
        .map(c -> c.load())
        .filter(c -> c.isAnnotationPresent(Bean.class))
        .collect(Collectors.toList());
  }


  public SimpleIoC(JavaPlugin plugin) {
    this.plugin = plugin;

    addBean(plugin);
  }

  /**
   * Deep scan all the classes from the provided package. <br>
   * All the beans will be instantiated and if they implement {@link Listener}, registered. <br>
   * @param exclusions Packages to exclude from the scan
   */
  public void initialize(String packageToScan, List<String> exclusions) {
    ClassPath classPath;
    try {
      classPath = ClassPath.from(plugin.getClass().getClassLoader());
    } catch (IOException e) {
      throw new IocException("Unable to scan all the packages", e);
    }

    List<Class<?>> beans = findAllBeans(classPath, packageToScan, exclusions);
    loadBeans(beans);
  }

  /**
   * Deep scan all the classes from the where the plugin main class is located. <br>
   * All the beans will be instantiated and if they implement {@link Listener}, registered. <br>
   * @param exclusions Packages to exclude from the scan
   */
  public void initialize(String... exclusions) {
    initialize(plugin.getClass().getPackage().getName(), Arrays.asList(exclusions));
  }

  private void loadBeans(List<Class<?>> beans) {
    if(beans.isEmpty()) {
      return;
    }

    for(Class<?> bean : beans) {
      if(getBean(bean) != null) {
        continue;
      }

      addBean(
          loadBean(bean, new HashSet<>())
      );
    }
  }

  private Object loadBean(Class<?> bean, Set<Class<?>> parentBeans) throws IocException {
    if(!parentBeans.add(bean)) {
      throw new IocException("Circular dependency detected for " + parentBeans);
    }

    int i=0;
    Constructor<?> constructor = findBeanConstructor(bean);
    Object[] args = new Object[constructor.getParameterCount()];
    for(Parameter parameter : constructor.getParameters()) {
      args[i] = getBean(parameter.getType());
      if(args[i] == null) {
        args[i] = loadBean(parameter.getType(), parentBeans);
        addBean(args[i]);
      }

      i += 1;
    }

    return ReflectionUtils.newInstance(constructor, args);
  }

  @SuppressWarnings("unchecked")
  public <T> T getBean(Class<T> clazz) {
    return (T) beans.stream()
        .filter(b -> clazz.isAssignableFrom(b.getClass()))
        .findFirst().orElse(null);
  }

  public void addBean(Object bean) {
    if(bean instanceof Listener) {
      plugin.getServer().getPluginManager().registerEvents((Listener) bean, plugin);
    }

    beans.add(bean);
  }

  /**
   * Unregister and stop the bean if it implements {@link Listener} or {@link Stoppable}. <br>
   * Remove the bean from the container.
   */
  public void removeBean(Object bean) {
   if(bean instanceof Stoppable) {
     ((Stoppable) bean).stop();
   }

   if(bean instanceof Listener) {
     HandlerList.unregisterAll((Listener) bean);
   }

    beans.remove(bean);
  }

  /**
   * Unregister and stop all the beans that implement {@link Listener} or {@link Stoppable}. <br>
   * Clears the container.
   */
  public void removeAllBeans() {
    for(Object bean : beans) {
      if(bean instanceof Stoppable) {
        ((Stoppable) bean).stop();
      }

      if(bean instanceof Listener) {
        HandlerList.unregisterAll((Listener) bean);
      }
    }

    beans.clear();
  }

}
