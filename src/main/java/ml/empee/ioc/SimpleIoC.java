package ml.empee.ioc;

import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import ml.empee.ioc.annotations.Bean;
import ml.empee.ioc.annotations.InversionOfControl;
import ml.empee.ioc.utility.ReflectionUtils;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public final class SimpleIoC {

  private final JavaPlugin plugin;
  private final List<Object> beans = new ArrayList<>();

  private SimpleIoC(JavaPlugin plugin) {
    this.plugin = plugin;
    beans.add(plugin);
  }

  /**
   * Deep scan all the classes from the provided package
   * @param exclusions Packages to exclude from the scan
   */
  public static SimpleIoC initialize(JavaPlugin plugin, String packageToScan, List<String> exclusions) {
    ClassPath classPath;
    try {
      classPath = ClassPath.from(plugin.getClass().getClassLoader());
    } catch (IOException e) {
      throw new IocException("Unable to scan all the packages", e);
    }

    List<Class<?>> beans = findAllBeans(classPath, packageToScan, exclusions);
    return loadBeans(plugin, beans);
  }

  /**
   * Deep scan all the classes from the where the plugin main class is located
   * @param exclusions Packages to exclude from the scan
   */
  public static SimpleIoC initialize(JavaPlugin plugin, String... exclusions) {
    return initialize(plugin, plugin.getClass().getPackage().getName(), Arrays.asList(exclusions));
  }

  private static SimpleIoC loadBeans(JavaPlugin plugin, List<Class<?>> beans) {
    SimpleIoC container = new SimpleIoC(plugin);
    while (beans.size() != 0) {
      Iterator<Class<?>> beanIterator = beans.iterator();
      while (beanIterator.hasNext()) {
        Class<?> beanClazz = beanIterator.next();
        Constructor<?> beanConstructor = findBeanConstructor(beanClazz);
        Optional<Object> bean = createBean(container, beanConstructor);
        if(bean.isPresent()) {
          container.addBean((bean.get()));
          beanIterator.remove();
        }
      }
    }

    return container;
  }

  private static Constructor<?> findBeanConstructor(Class<?> bean) {
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
  private static Optional<Object> createBean(SimpleIoC container, Constructor<?> constructor) {
    if(constructor.getParameterCount() == 0) {
      return Optional.of(ReflectionUtils.newInstance(constructor));
    }

    Parameter[] parameters = constructor.getParameters();
    Object[] args = new Object[parameters.length];
    for(int i=0; i<parameters.length; i++) {
      Object bean = container.getBean(parameters[i].getType());
      if(bean != null) {
        args[i] = bean;
      } else {
        return Optional.empty();
      }
    }

    return Optional.of(
        ReflectionUtils.newInstance(constructor, args)
    );
  }

  private static List<Class<?>> findAllBeans(ClassPath classPath, String packageToScan,  List<String> exclusions) {
    return classPath.getAllClasses().stream()
        .filter(c -> c.getPackageName().startsWith(packageToScan))
        .filter(c -> exclusions.stream().noneMatch(e -> c.getPackageName().startsWith(packageToScan + "." + e)))
        .map(c -> c.load())
        .filter(c -> c.isAnnotationPresent(Bean.class))
        .collect(Collectors.toList());
  }

  public <T> T getBean(Class<T> clazz) {
    return (T) beans.stream()
        .filter(b -> b.getClass().equals(clazz))
        .findFirst().orElse(null);
  }

  public void addBean(Object bean) {
    if(bean instanceof Listener) {
      plugin.getServer().getPluginManager().registerEvents((Listener) bean, plugin);
    }

    beans.add(bean);
  }

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
