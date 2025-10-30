package tech.nocountry.onboarding.services.notifications;

public interface NotificationPublisher {
    void notifyStatusChange(String userId, String applicationId, String previousStatus, String newStatus);
}
