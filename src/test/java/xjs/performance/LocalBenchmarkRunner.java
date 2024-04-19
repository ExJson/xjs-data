package xjs.performance;

import org.openjdk.jmh.annotations.Benchmark;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class LocalBenchmarkRunner {

  private LocalBenchmarkRunner() {}

  public static void runIfEnabled() throws Exception {
    final String[] args = generateArgs(getEnabled(getCaller()));
    org.openjdk.jmh.Main.main(args);
  }

  private static Class<?> getCaller() {
    final String name = Thread.currentThread().getStackTrace()[3].getClassName();
    try {
      return Class.forName(name);
    } catch (final ClassNotFoundException ignored) {
      throw new AssertionError("unreachable");
    }
  }

  private static List<Method> getEnabled(final Class<?> c) {
    final List<Method> enabled = new ArrayList<>();
    for (final Method m : c.getMethods()) {
      if (!m.isAnnotationPresent(Benchmark.class)) {
        continue;
      }
      final Enabled e = m.getAnnotation(Enabled.class);
      if (e != null && e.value()) {
        System.out.println("Running benchmark: " + m.getName());
        enabled.add(m);
      } else {
        System.out.println("Skipping benchmark: " + m.getName());
      }
    }
    return enabled;
  }

  private static String[] generateArgs(final List<Method> enabled) {
    return new String[] { createPattern(enabled) };
  }

  private static String createPattern(final List<Method> enabled) {
    if (enabled.isEmpty()) {
      return "garbage." + enabled.hashCode();
    }
    final String className = enabled.get(0).getDeclaringClass().getCanonicalName();
    final List<String> names = enabled.stream().map(Method::getName).toList();
    final StringBuilder sb = new StringBuilder(className).append(".(");
    for (int i = 0; i < names.size(); i++) {
      if (i > 0) {
        sb.append('|');
      }
      sb.append(names.get(i));
    }
    return sb.append(")").toString();
  }
}