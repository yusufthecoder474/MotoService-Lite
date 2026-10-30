package com.motoservice.config;

import com.motoservice.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Fail closed before readiness when the cloud profile is misconfigured. */
@Component
@Profile("cloud")
public class CloudSafetyGuard implements ApplicationRunner {
  private final Environment env;
  private final UserRepository users;

  public CloudSafetyGuard(Environment env, UserRepository users) {
    this.env = env;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (env.getProperty("app.demo", Boolean.class, false))
      throw new IllegalStateException("Cloud demo seeding must be disabled");
    String url = env.getRequiredProperty("spring.datasource.url");
    var query = java.net.URI.create(url.substring(5)).getRawQuery();
    var params = new java.util.HashMap<String, String>();
    if (query != null)
      for (String part : query.split("&")) {
        String[] kv = part.split("=", 2);
        if (kv.length != 2 || params.putIfAbsent(kv[0], kv[1]) != null)
          throw new IllegalStateException("Invalid JDBC TLS parameters");
      }
    if (!"verify-full".equals(params.get("sslmode"))
        || !"org.postgresql.ssl.DefaultJavaSSLFactory".equals(params.get("sslfactory")))
      throw new IllegalStateException(
          "Cloud database requires verified TLS using the JVM trust store");
    for (String name : new String[] {"customer1", "customer2", "customer3", "customer4", "advisor"})
      if (users.findByUsername(name).isPresent())
        throw new IllegalStateException(
            "Use a separate cloud database without published demo accounts");
  }
}
