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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    @Order(4)
    void twoPeopleCannotHaveTheSameDocument() {
        // Creating the document and assigning it to John
        final var document = new Document();
        document.setCode("XYZ12345");
        final var documentSaved = documentRepository.save(document);

        final var person1 = new Person();
        person1.setName("John Smith");
        person1.setDocument(document);
        personRepository.save(person1);

        // Trying to assign the same document to Mary
        final var person2 = new Person();
        person2.setName("Mary Jane");
        person2.setDocument(document);

        // Asserting the second assignment does not work
        final var exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> personRepository.save(person2)
        );

        assertThat(exception)
                .getRootCause()
                .hasMessageContainingAll(
                        "ERROR: duplicate key value violates unique constraint \"person_document_id_key\"",
                        "Detail: Key (document_id)=(2) already exists."
                );
    }
}
