package ml.empee.ioc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
@RequiredArgsConstructor
public abstract class ScheduledTask extends BukkitRunnable {

  private final long delay;
  private final long period;
  private final boolean async;

}
