package eu.nordlyse.eudi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nordlyse.eudi.config.EudiProperties;
import eu.nordlyse.eudi.domain.IssuedCredential;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CredentialRegistry {

    private final Path file;
    private final ObjectMapper mapper;
    private final Map<String, IssuedCredential> credentials = new ConcurrentHashMap<>();

    public CredentialRegistry(EudiProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        this.file = Path.of(properties.dataDirectory()).resolve("issued-pids.json");
        load();
    }

    public synchronized void save(IssuedCredential credential) {
        credentials.put(credential.id(), credential);
        persist();
    }

    public Optional<IssuedCredential> find(String id) {
        return Optional.ofNullable(credentials.get(id));
    }

    public List<IssuedCredential> list() {
        return credentials.values().stream()
                .sorted(Comparator.comparing(IssuedCredential::issuedAt).reversed())
                .toList();
    }

    public int size() {
        return credentials.size();
    }

    public synchronized int nextStatusIndex() {
        return credentials.values().stream()
                .mapToInt(IssuedCredential::statusIndex)
                .max()
                .orElse(-1) + 1;
    }

    public boolean[] revocationFlags() {
        int size = Math.max(nextStatusIndex(), 1);
        boolean[] flags = new boolean[size];
        for (IssuedCredential credential : credentials.values()) {
            if (credential.statusIndex() >= 0 && credential.statusIndex() < flags.length) {
                flags[credential.statusIndex()] = credential.revoked();
            }
        }
        return flags;
    }

    private void load() {
        try {
            if (!Files.exists(file)) {
                return;
            }
            List<IssuedCredential> stored = mapper.readValue(file.toFile(), new TypeReference<>() {
            });
            for (IssuedCredential credential : stored) {
                credentials.put(credential.id(), credential);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load issued PIDs from " + file, ex);
        }
    }

    private void persist() {
        try {
            Files.createDirectories(file.getParent());
            List<IssuedCredential> snapshot = new ArrayList<>(credentials.values());
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), snapshot);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to persist issued PIDs to " + file, ex);
        }
    }
}
