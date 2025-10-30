package tech.nocountry.onboarding.modules.applications.events;

import org.springframework.context.ApplicationEvent;
import lombok.Getter;

@Getter
public class ApplicationStatusChangedEvent extends ApplicationEvent {
    private final String applicationId;
    private final String previousStatus;
    private final String newStatus;
    private final String userId;

    public ApplicationStatusChangedEvent(Object source, String applicationId, String previousStatus, String newStatus, String userId) {
        super(source);
        this.applicationId = applicationId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.userId = userId;
    }
}
