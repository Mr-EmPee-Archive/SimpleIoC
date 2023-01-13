# SimpleIoC
A lightweigth InvesrionOfControl container for minecraft,
share and create instances of your classes with ease!

# Installation
If you are using maven you can add this to your `pom.xml`
```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.Mr-EmPee</groupId>
  <artifactId>SimpleIoC</artifactId>
  <version>1.0.0</version>
</dependency>
```
# Usage
Mark the classes that you want accessibile with `@Bean` these classes
must have at least 1 accessibile public constructor.
## Tips
> Every bean constructor can depends on one or more beans 

> If there is more then 1 constructur you must mark the one that will be used by the lib with `@InversionOfControl`

> Make sure to not create a circular dependency, otherwise the plugin is not going to enable!

> Your plugin instance is the first bean of the container
### Example
```java
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
  public static class SecondBean {

    private final FirstBean firstBean;
    
    public SecondBean(FirstBean firstBean) {
      this.firstBean = firstBean;
    }

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
```
# Features
- Beans that are instance of `Listener` will be autmatically registered
- Beans that are instance of `Stoppable` will invoke the `stop()` method when being removed
