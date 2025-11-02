package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.PasswordHistory;
import tech.nocountry.onboarding.repositories.PasswordHistoryRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PasswordHistoryService {

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Value("${password.history-size:5}")
    private int historySize;

    /**
     * Agregar contraseña al historial
     */
    public void addPasswordToHistory(String userId, String passwordHash) {
        PasswordHistory history = PasswordHistory.builder()
                .historyId(java.util.UUID.randomUUID().toString())
                .userId(userId)
                .passwordHash(passwordHash)
                .createdAt(LocalDateTime.now())
                .build();

        passwordHistoryRepository.save(history);

        // Limpiar historial antiguo (mantener solo las últimas N)
        cleanupOldPasswords(userId);
    }

    /**
     * Verificar si una contraseña está en el historial
     */
    public boolean isPasswordInHistory(String userId, String passwordHash) {
        return passwordHistoryRepository.countByUserIdAndPasswordHash(userId, passwordHash) > 0;
    }

    /**
     * Obtener historial de contraseñas de un usuario
     */
    public List<PasswordHistory> getPasswordHistory(String userId) {
        return passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Limpiar contraseñas antiguas (mantener solo las últimas N)
     */
    private void cleanupOldPasswords(String userId) {
        List<PasswordHistory> history = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        if (history.size() > historySize) {
            // Eliminar las más antiguas (mantener solo las últimas N)
            List<PasswordHistory> toDelete = history.subList(historySize, history.size());
            passwordHistoryRepository.deleteAll(toDelete);
        }
    }
}

