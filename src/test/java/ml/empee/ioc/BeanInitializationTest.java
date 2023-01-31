package ml.empee.ioc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ml.empee.ioc.annotations.Bean;
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

  @Bean
  public static class ThirdBean {

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

  @Bean
  @RequiredArgsConstructor
  public static class SecondBean {

    private final FirstBean firstBean;

  }

  @Bean
  public static class FirstBean implements Stoppable {

    @Getter
    private boolean stopped = false;

    @Override
    public void stop() {
      stopped = true;
    }
  }

}
