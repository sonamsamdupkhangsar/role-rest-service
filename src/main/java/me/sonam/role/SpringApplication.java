package me.sonam.role;

import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

//put the following scanBasePackages because jwt-validator needs to be scanned from me.sonam.security package
// also scan this application too.
@SpringBootApplication(scanBasePackages = {"me.sonam"})
public class SpringApplication {
    private static final Logger LOG = LoggerFactory.getLogger(SpringApplication.class);
    public static void main(String[] args) {
        LOG.info("starting springApplication");
        System.out.println("starting spring application");
        org.springframework.boot.SpringApplication.run(SpringApplication.class, args);
    }

    @Bean()
    ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        // This will create our database table and schema
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));

        // This will drop our table after we are done so we can have a fresh start next run
        //initializer.setDatabaseCleaner(new ResourceDatabasePopulator(new ClassPathResource("cleanup.sql")));
        return initializer;
    }
}
