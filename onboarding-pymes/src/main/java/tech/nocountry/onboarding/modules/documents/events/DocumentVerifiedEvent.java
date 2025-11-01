package tech.nocountry.onboarding.modules.documents.events;

import org.springframework.context.ApplicationEvent;
import lombok.Getter;

@Getter
public class DocumentVerifiedEvent extends ApplicationEvent {
    private final String documentId;
    private final String applicationId;
    private final String userId;  // Usuario dueño del documento
    private final String verificationStatus;
    private final String verifiedByUserId;

    public DocumentVerifiedEvent(Object source, String documentId, String applicationId, 
                                  String userId, String verificationStatus, String verifiedByUserId) {
        super(source);
        this.documentId = documentId;
        this.applicationId = applicationId;
        this.userId = userId;
        this.verificationStatus = verificationStatus;
        this.verifiedByUserId = verifiedByUserId;
    }
}

