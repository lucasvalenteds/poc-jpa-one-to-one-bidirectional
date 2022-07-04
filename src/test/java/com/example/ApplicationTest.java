package com.example;

import com.example.document.Document;
import com.example.document.DocumentRepository;
import com.example.person.Person;
import com.example.person.PersonRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplicationTest {

    @Container
    private static final PostgreSQLContainer<?> CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres"));

    @DynamicPropertySource
    private static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", CONTAINER::getUsername);
        registry.add("spring.datasource.password", CONTAINER::getPassword);
    }

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private PersonRepository personRepository;

    @Test
    @Order(1)
    void creatingDocument() {
        final var document = new Document();
        document.setCode("XD892342");

        final var documentSaved = documentRepository.save(document);

        assertNotNull(documentSaved.getId());
        assertEquals(document.getCode(), documentSaved.getCode());
        assertEquals(document.getPerson(), documentSaved.getPerson());
    }

    @Test
    @Order(2)
    void creatingPerson() {
        final var person = new Person();
        person.setName("John Smith");

        final var personSaved = personRepository.save(person);

        assertNotNull(person.getId());
        assertEquals(person.getName(), personSaved.getName());
        assertEquals(person.getDocument(), personSaved.getDocument());
    }

    @Test
    @Order(3)
    void assigningDocumentToPerson() {
        final var document = documentRepository.findById(1L).orElseThrow();
        final var person = personRepository.findById(1L).orElseThrow();
        person.setDocument(document);

        final var personUpdated = personRepository.save(person);
        final var documentUpdated = documentRepository.findById(document.getId()).orElseThrow();

        assertEquals(person.getId(), personUpdated.getId());
        assertEquals(person.getName(), personUpdated.getName());
        assertNotNull(person.getDocument());
        assertEquals(person.getDocument().getId(), documentUpdated.getId());
        assertEquals(person.getDocument().getCode(), documentUpdated.getCode());

        assertNotNull(documentUpdated.getPerson());
        assertEquals(documentUpdated.getPerson().getId(), personUpdated.getId());
        assertEquals(documentUpdated.getPerson().getName(), personUpdated.getName());
    }
}
