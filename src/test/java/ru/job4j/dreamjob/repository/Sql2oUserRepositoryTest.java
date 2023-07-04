package ru.job4j.dreamjob.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.job4j.dreamjob.configuration.DatasourceConfiguration;
import ru.job4j.dreamjob.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class Sql2oUserRepositoryTest {

    private static Sql2oUserRepository sql2oUserRepository;

    @BeforeAll
    public static void initRepositories() throws Exception {
        var properties = new Properties();
        try (var inputStream = Sql2oUserRepositoryTest.class.getClassLoader().getResourceAsStream("connection.properties")) {
            properties.load(inputStream);
        }
        var url = properties.getProperty("datasource.url");
        var username = properties.getProperty("datasource.username");
        var password = properties.getProperty("datasource.password");

        var configuration = new DatasourceConfiguration();
        var datasource = configuration.connectionPool(url, username, password);
        var sql2o = configuration.databaseClient(datasource);

        sql2oUserRepository = new Sql2oUserRepository(sql2o);
    }

    @AfterEach
    public void deleteUsers() {
        var users = sql2oUserRepository.findAll();
        for (var u : users) {
            sql2oUserRepository.delete(u.getEmail(), u.getPassword());
        }
    }

    @Test
    public void whenSaveThenGetSame() {
        User user = new User(0, "ivan@mail.ru", "Ivan", "password");
        Optional<User> saveUser = sql2oUserRepository.save(user);
        Optional<User> findUser = sql2oUserRepository.findByEmailAndPassword(user.getEmail(), user.getPassword());
        assertThat(findUser).usingRecursiveComparison().isEqualTo(saveUser);
    }

    @Test
    public void whenSaveSeveralThenGetAll() {
        Optional<User> user1 = sql2oUserRepository.save(new User(0, "ivan@mail.ru", "Ivan", "password"));
        Optional<User> user2 = sql2oUserRepository.save(new User(0, "Petr@gmail.com", "Petr", "123"));
        Optional<User> user3 = sql2oUserRepository.save(new User(0, "Sidor@yandex.ru", "Sidor", "321"));
        var result = sql2oUserRepository.findAll();
        assertThat(result).isEqualTo(List.of(user1.get(), user2.get(), user3.get()));
    }

    @Test
    public void whenDontSaveThenNothingFound() {
        assertThat(sql2oUserRepository.findAll()).isEqualTo(emptyList());
    }

    @Test
    public void whenDeleteThenGetEmptyOptional() {
        User user = new User(0, "ivan@mail.ru", "Ivan", "password");
        sql2oUserRepository.save(user);
        var isDeleted = sql2oUserRepository.delete(user.getEmail(), user.getPassword());
        var findUser = sql2oUserRepository.findByEmailAndPassword(user.getEmail(), user.getPassword());
        assertThat(isDeleted).isTrue();
        assertThat(findUser).isEqualTo(empty());
    }

    @Test
    public void whenDeleteByInvalidIdThenGetFalse() {
        assertThat(sql2oUserRepository.delete("email", "password")).isFalse();
    }

    @Test
    public void whenUserWithSuchMailExistsThenGetFalse() {
        sql2oUserRepository.save(new User(0, "ivan@mail.ru", "Ivan", "password"));
        User user = new User(0, "ivan@mail.ru", "Vanya", "123");
        Optional<User> saveUser2 = sql2oUserRepository.save(user);
        assertThat(saveUser2).isEqualTo(empty());
    }
}