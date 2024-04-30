package com.copy.trader;

import com.copy.trader.task.UpdatePoolTask;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@AllArgsConstructor
public class SniperStartApplication implements CommandLineRunner {

        @Autowired
        private UpdatePoolTask newTask;

        public static void main(String[] args) {
            SpringApplication application = new SpringApplication(SniperStartApplication.class);
            application.run(args);
        }

        @Override
        public void run(String... args) {
            newTask.start();
        }
}