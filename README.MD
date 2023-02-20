# SimpleIoC [![](https://jitpack.io/v/Mr-EmPee/SimpleIoC.svg)](https://jitpack.io/#Mr-EmPee/SimpleIoC)
A lightweight InvesrionOfControl container for minecraft,
share and create instances of your classes with ease!

# Additional features
- Beans that are an instance of `RegisteredListener` will be automatically registered

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
Make the classes that you want accessible implements the Bean interface (These classes must have at least 1 accessible public constructor).

- Create an instance of SimpleIoC
- Use #SimpleIoC.initialize inside your onEnable
- Use #SimpleIoC.removeAllBeans inside your onDisable
## Tips
> Every bean constructor can depends on one or more beans 

> If there is more then 1 constructur you must mark the one that will be used by the lib with `@InversionOfControl`

> Make sure to not create a circular dependency, otherwise the plugin is not going to enable!

> Your plugin instance is the first bean of the container
### Example
```java
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

  public static class SecondBean implements Bean {

    private final FirstBean firstBean;
    
    public SecondBean(FirstBean firstBean) {
      this.firstBean = firstBean;
    }

  }

  public static class FirstBean implements Bean {

    @Getter
    private boolean stopped = false;

    @Override
    public void onStop() {
      stopped = true;
    }
  }
```