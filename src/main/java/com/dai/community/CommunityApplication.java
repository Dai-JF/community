package com.dai.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author DaiJF
 */
@SpringBootApplication
public class CommunityApplication {



  public static void main(String[] args) {
    System.setProperty("es.set.netty.runtime.available.processors", "false");
    SpringApplication.run(CommunityApplication.class, args);
  }

}
