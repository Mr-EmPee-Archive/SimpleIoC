package ml.empee.ioc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ml.empee.ioc.annotations.DependsOn;
import ml.empee.ioc.annotations.InversionOfControl;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BeanInitializationTest {

  private final JavaPlugin plugin = Mockito.mock(JavaPlugin.class);

  @Test
  void shouldInitializeBeans() {
    SimpleIoC container = new SimpleIoC(plugin);
    container.initialize("ml.empee.ioc", Collections.emptyList());

    assertNotNull(container.getBean(FirstBean.class));
    assertNotNull(container.getBean(SecondBean.class));
    assertNotNull(container.getBean(ThirdBean.class));
  }

  @Test
  void shouldStopBeans() {
    SimpleIoC container = new SimpleIoC(plugin);
    container.initialize("ml.empee.ioc", Collections.emptyList());

    FirstBean firstBean = container.getBean(FirstBean.class);
    container.removeAllBeans();
    assertTrue(firstBean.isStopped());
  }

  @Test
  void shouldNotCreateSomeConditionalBeans() {
    SimpleIoC container = new SimpleIoC(plugin);
    container.initialize("ml.empee.ioc", Collections.emptyList());
    assertTrue(container.hasBean(FifthBean.class));
    container.removeAllBeans();
    FourthBean.enabled = false;
    assertThrows(IocException.class, () -> {
      container.initialize("ml.empee.ioc", Collections.emptyList());
    });
  }

  public static class ThirdBean implements Bean {

    private final FirstBean firstBean;
    private final SecondBean secondBean;

    @InversionOfControl
    public ThirdBean(FirstBean firstBean, SecondBean secondBean) {
      this.firstBean = firstBean;
      this.secondBean = secondBean;
    }

    public ThirdBean(String test) {
      this.firstBean = null;
      this.secondBean = null;
    }

  }

  @RequiredArgsConstructor
  public static class SecondBean implements Bean {

    private final FirstBean firstBean;

  }

  public static class FirstBean implements Bean {

    @Getter
    private boolean stopped = false;

    @Override
    public void onStop() {
      stopped = true;
    }
  }

  public static class FourthBean implements Bean {

    public static boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }
  }

  @DependsOn(FourthBean.class)
  public static class FifthBean implements Bean {
  }

}
