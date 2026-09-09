package eu.nordlyse.eudi.service;

import eu.nordlyse.eudi.domain.IssuedCredential;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CredentialRegistry {

    private final Map<String, IssuedCredential> credentials = new ConcurrentHashMap<>();

    public void save(IssuedCredential credential) {
        credentials.put(credential.id(), credential);
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
}
