package com.motoservice;

import static org.junit.jupiter.api.Assertions.*;

import com.motoservice.exception.BusinessException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class EvaluationIT extends PostgresTestBase {
  record Attempt(boolean accepted, double millis) {}

  @Test
  void resetOnly() {
    assertEquals(0, occupancy());
    assertEquals(60, count("SELECT count(*) FROM motorcycles"));
  }

  @Test
  void evaluateSyntheticWorkloads() throws Exception {
    Path out = Path.of("target/evaluation");
    Files.createDirectories(out);
    List<String> rows =
        new ArrayList<>(
            List.of(
                "approach,repetition,requests,capacity,confirmed,rejected,overbooking,occupancy,duration_ms,median_ms,p95_ms"));
    for (int[] scenario : new int[][] {{20, 2}, {50, 5}})
      for (int rep = 1; rep <= 3; rep++)
        for (boolean safe : new boolean[] {false, true}) {
          resetSyntheticData();
          capacity(scenario[1]);
          long start = System.nanoTime();
          var results =
              ConcurrentRunner.run(
                  scenario[0],
                  i -> {
                    long t = System.nanoTime();
                    boolean accepted;
                    if (safe) {
                      try {
                        service.book("c" + (i + 1), input(i + 1));
                        accepted = true;
                      } catch (BusinessException e) {
                        if (e.status().value() != 409) throw e;
                        accepted = false;
                      }
                    } else { // Unsafe baseline exists ONLY in test code. No sleep, forced overlap,
                      // or fabricated result.
                      long occupied = occupancy();
                      accepted = occupied < scenario[1];
                      if (accepted)
                        jdbc.update(
                            "INSERT INTO"
                                + " bookings(customer_id,motorcycle_id,slot_id,service_type,status,created_at)"
                                + " VALUES (?,?,1,'REGULAR_SERVICE','CONFIRMED',now())",
                            i + 1,
                            i + 1);
                    }
                    return new Attempt(accepted, (System.nanoTime() - t) / 1_000_000.0);
                  });
          double duration = (System.nanoTime() - start) / 1_000_000.0;
          long accepted = results.stream().filter(Attempt::accepted).count(),
              occupied = occupancy();
          double[] times = results.stream().mapToDouble(Attempt::millis).sorted().toArray();
          double median = (times[(times.length - 1) / 2] + times[times.length / 2]) / 2;
          double p95 = times[(int) Math.ceil(times.length * .95) - 1];
          rows.add(
              String.format(
                  Locale.ROOT,
                  "%s,%d,%d,%d,%d,%d,%d,%d,%.3f,%.3f,%.3f",
                  safe ? "transactional" : "naive",
                  rep,
                  scenario[0],
                  scenario[1],
                  accepted,
                  scenario[0] - accepted,
                  Math.max(0, occupied - scenario[1]),
                  occupied,
                  duration,
                  median,
                  p95));
          if (safe) {
            assertEquals(scenario[1], occupied);
            assertEquals(scenario[1], accepted);
          }
        }
    Files.write(out.resolve("overbooking.csv"), rows);
    List<String> recovery =
        new ArrayList<>(
            List.of(
                "approach,capacity,initial_confirmed,waitlist,cancelled,recovered,unused,fill_rate,promoted"));
    for (boolean safe : new boolean[] {false, true}) {
      resetSyntheticData();
      capacity(5);
      List<Long> ids = new ArrayList<>();
      for (int i = 1; i <= 5; i++) ids.add(service.book("c" + i, input(i)).id());
      for (int i = 6; i <= 15; i++) service.join("c" + i, input(i));
      for (int i = 0; i < 2; i++) {
        if (safe) service.cancel("c" + (i + 1), ids.get(i));
        else jdbc.update("UPDATE bookings SET status='CANCELLED' WHERE id=?", ids.get(i));
      }
      long promoted = count("SELECT count(*) FROM waitlist_entries WHERE status='PROMOTED'"),
          occupied = occupancy();
      recovery.add(
          String.format(
              Locale.ROOT,
              "%s,5,5,10,2,%d,%d,%.3f,%d",
              safe ? "fifo" : "no_reallocation",
              promoted,
              5 - occupied,
              occupied / 5.0,
              promoted));
      assertEquals(safe ? 2 : 0, promoted);
      assertEquals(safe ? 5 : 3, occupied);
    }
    Files.write(out.resolve("recovery.csv"), recovery);
    String environment =
        "SYNTHETIC; Java "
            + System.getProperty("java.version")
            + "; Spring Boot "
            + org.springframework.boot.SpringBootVersion.getVersion()
            + "; "
            + jdbc.queryForObject("SELECT version()", String.class)
            + "; OS "
            + System.getProperty("os.name")
            + " "
            + System.getProperty("os.arch")
            + "; logical processors "
            + Runtime.getRuntime().availableProcessors()
            + "; JVM max memory "
            + Runtime.getRuntime().maxMemory()
            + " bytes. Dataset: deterministic IDs 1–60; no RNG. Hikari pool 60. Three repetitions"
            + " per scenario. Nearest-rank p95; median is midpoint of central samples.";
    Files.writeString(out.resolve("environment.txt"), environment);
    Files.writeString(
        out.resolve("results.md"),
        "# Executed synthetic evaluation\n\n"
            + environment
            + "\n\n## A: Overbooking\n\n```csv\n"
            + String.join("\n", rows)
            + "\n```\n\n## B: Recovery\n\n```csv\n"
            + String.join("\n", recovery)
            + "\n"
            + "```\n\n"
            + "Latency is measured at the Java operation boundary, not HTTP. Baseline uses direct"
            + " JDBC while proposed uses full service/JPA transactions; timings are descriptive,"
            + " not an algorithm speed comparison. No warm-up or production-scale claim. H1 is"
            + " supported only for these finite tested workloads; tests do not prove a universal"
            + " never claim. H2 is supported for the seeded eligible-demand scenario, not real"
            + " revenue or satisfaction.\n");
  }
}
