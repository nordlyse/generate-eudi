package eu.nordlyse.eudi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nordlyse.eudi.config.EudiProperties;
import eu.nordlyse.eudi.domain.AuditEvent;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AuditLogService {

    private final Path file;
    private final ObjectMapper mapper;
    private final List<AuditEvent> events = new ArrayList<>();

    public AuditLogService(EudiProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        this.file = Path.of(properties.dataDirectory()).resolve("audit-log.json");
        load();
    }

    public synchronized void record(
            String officer,
            String action,
            String credentialId,
            String documentNumber,
            String detail
    ) {
        events.add(new AuditEvent(Instant.now(), officer, action, credentialId, documentNumber, detail));
        persist();
    }

    public synchronized List<AuditEvent> list() {
        return events.stream()
                .sorted(Comparator.comparing(AuditEvent::at).reversed())
                .toList();
    }

    private void load() {
        try {
            if (!Files.exists(file)) {
                return;
            }
            events.addAll(mapper.readValue(file.toFile(), new TypeReference<>() {
            }));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load audit log from " + file, ex);
        }
    }

    private void persist() {
        try {
            Files.createDirectories(file.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), events);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to persist audit log to " + file, ex);
        }
    }
}
