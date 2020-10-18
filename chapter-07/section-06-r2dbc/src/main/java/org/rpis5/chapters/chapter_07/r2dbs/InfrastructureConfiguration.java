package org.rpis5.chapters.chapter_07.r2dbs;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.DefaultReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;

/**
 * @author Oliver Gierke
 */
@Configuration
class InfrastructureConfiguration {

   //@Bean
   BookRepository customerRepository2(ConnectionFactory connectionFactory) {
      DatabaseClient client =
              DatabaseClient.builder()
                      .connectionFactory(connectionFactory)
                      .build();
      DefaultReactiveDataAccessStrategy strategy = new DefaultReactiveDataAccessStrategy(PostgresDialect.INSTANCE);
      return new R2dbcRepositoryFactory(client, strategy)
         .getRepository(BookRepository.class);
   }

   @Bean
   BookRepository customerRepository(R2dbcRepositoryFactory factory) {
      return factory.getRepository(BookRepository.class);
   }

   @Bean
   R2dbcRepositoryFactory repositoryFactory(DatabaseClient databaseClient) {
      DefaultReactiveDataAccessStrategy strategy = new DefaultReactiveDataAccessStrategy(PostgresDialect.INSTANCE);
      return new R2dbcRepositoryFactory(databaseClient, strategy);
   }

   @Bean
   DatabaseClient databaseClient(ConnectionFactory connectionFactory) {
      return DatabaseClient.builder()
              .connectionFactory(connectionFactory)
              .build();
   }

   @Bean
   ConnectionFactory connectionFactory(DatabaseLocation databaseLocation) {

      PostgresqlConnectionConfiguration config = PostgresqlConnectionConfiguration.builder()
         .host(databaseLocation.getHost())
         .port(databaseLocation.getPort())
         .database(databaseLocation.getDatabase())
         .username(databaseLocation.getUser())
         .password(databaseLocation.getPassword())
         .build();

      return new PostgresqlConnectionFactory(config);
   }
}
