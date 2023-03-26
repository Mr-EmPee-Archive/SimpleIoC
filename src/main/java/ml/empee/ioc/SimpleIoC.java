package ml.empee.ioc;

import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import ml.empee.ioc.annotations.DependsOn;
import ml.empee.ioc.annotations.InversionOfControl;
import ml.empee.ioc.utility.ReflectionUtils;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * Simple IoC container for Bukkit plugins. <br> Use {@link #initialize(String...)} to initialize the
 * container. <br>
 */
public final class SimpleIoC {

  private final JavaPlugin plugin;
  private final List<Object> beans = new ArrayList<>();

  private static Constructor<? extends Bean> findBeanConstructor(Class<?> bean) throws IocException {
    if (!Bean.class.isAssignableFrom(bean)) {
      throw new IocException("The class " + bean.getName() + " isn't a bean");
    }

    Constructor<?>[] constructors = bean.getConstructors();
    if (constructors.length == 1) {
      return (Constructor<? extends Bean>) constructors[0];
    }

    for (Constructor<?> constructor : constructors) {
      if (constructor.isAnnotationPresent(InversionOfControl.class)) {
        return (Constructor<? extends Bean>) constructor;
      }
    }

    throw new IocException("Unable to find a constructor for the bean " + bean.getName());
  }

  private static List<Class<?>> findAllBeans(
      ClassPath classPath, String packageToScan, List<String> exclusions
  ) {
    return classPath.getAllClasses().stream()
        .filter(c -> c.getPackageName().startsWith(packageToScan))
        .filter(
            c -> exclusions.stream().noneMatch(e -> c.getPackageName().startsWith(packageToScan + "." + e)))
        .map(c -> c.load())
        .filter(c -> !c.equals(Bean.class))
        .filter(Bean.class::isAssignableFrom)
        .collect(Collectors.toList());
  }

  public SimpleIoC(JavaPlugin plugin) {
    this.plugin = plugin;

    addBean(plugin);
  }

  /**
   * Deep scan all the classes from the provided package. <br> All the beans will be instantiated and if they
   * implement {@link RegisteredListener}, registered. <br>
   *
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
   * Deep scan all the classes from the where the plugin main class is located. <br> All the beans will be
   * instantiated and if they implement {@link RegisteredListener}, registered. <br>
   *
   * @param exclusions Packages to exclude from the scan
   */
  public void initialize(String... exclusions) {
    initialize(plugin.getClass().getPackage().getName(), Arrays.asList(exclusions));
  }

  private void loadBeans(List<Class<?>> beans) {
    if (beans.isEmpty()) {
      return;
    }

    for (Class<?> bean : beans) {
      if (hasBean(bean)) {
        continue;
      }

      addBean(
          loadBean(bean, new HashSet<>())
      );
    }
  }

  private Bean loadBean(Class<?> bean, Set<Class<?>> parentBeans) throws IocException {
    if (!parentBeans.add(bean)) {
      throw new IocException("Circular dependency detected for " + parentBeans);
    }

    DependsOn dependencies = bean.getAnnotation(DependsOn.class);
    if(dependencies != null) {
      for (Class<?> dependency : dependencies.value()) {
        Object dependencyBean = getOrLoadBean(dependency, parentBeans);
        if(dependencyBean == null) {
          throw new IocException(
              "The bean " + bean.getName() + " depends on a conditional bean that isn't enabled!"
          );
        }
      }
    }

    Constructor<? extends Bean> constructor = findBeanConstructor(bean);
    Object[] args = new Object[constructor.getParameterCount()];

    int i = 0;
    for (Parameter parameter : constructor.getParameters()) {
      args[i] = getOrLoadBean(parameter.getType(), parentBeans);
      if(args[i] == null) {
        throw new IocException(
            "The bean " + bean.getName() + " depends on a conditional bean that isn't enabled!"
        );
      }

      i += 1;
    }

    return (Bean) ReflectionUtils.newInstance(constructor, args);
  }

  private Object getOrLoadBean(Class<?> clazz, Set<Class<?>> parentBeans) {
    Object bean = getBean(clazz);
    if (bean == null) {
      bean = loadBean(clazz, parentBeans);
      if (!((Bean) bean).isEnabled()) {
        return null;
      }

      addBean(bean);
    }

    return bean;
  }

  public boolean hasBean(Class<?> clazz) {
    return getBean(clazz) != null;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T getBean(Class<T> clazz) {
    return (T) beans.stream()
        .filter(b -> clazz.isAssignableFrom(b.getClass()))
        .findFirst().orElse(null);
  }

  /**
   * Add the bean to the container and register every scheduled repeated method or listener
   */
  public void addBean(Object bean) {
    if (bean instanceof Bean) {
      if (!((Bean) bean).isEnabled()) {
        return;
      }

      ((Bean) bean).onStart();
    }

    if (bean instanceof RegisteredListener) {
      plugin.getServer().getPluginManager().registerEvents((RegisteredListener) bean, plugin);
    }

    if(bean instanceof ScheduledTask) {
      ScheduledTask task = (ScheduledTask) bean;
      if(task.isAsync()) {
        task.runTaskTimerAsynchronously(plugin, task.getDelay(), task.getPeriod());
      } else {
        task.runTaskTimer(plugin, task.getDelay(), task.getPeriod());
      }
    }

    beans.add(bean);
  }

  private void unregisterBean(Object bean) {
    if (bean instanceof Bean) {
      ((Bean) bean).onStop();
    }

    if (bean instanceof Listener) {
      HandlerList.unregisterAll((Listener) bean);
    }

    if (bean instanceof ScheduledTask) {
      ((ScheduledTask) bean).cancel();
    }

    ReflectionUtils.pruneFieldsOf(bean);
  }

  /**
   * Remove the bean from the container and unregister every scheduled repeated method or listener
   */
  public void removeBean(Object bean) {
    unregisterBean(bean);
    beans.remove(bean);
  }

  /**
   * Remove all the beans from the container and unregister every scheduled repeated method or listener
   */
  public void removeAllBeans() {
    for (Object bean : beans) {
      unregisterBean(bean);
    }

    beans.clear();
  }

}
