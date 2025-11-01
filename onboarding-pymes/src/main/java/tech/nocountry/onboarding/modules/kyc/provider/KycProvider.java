package tech.nocountry.onboarding.modules.kyc.provider;

import tech.nocountry.onboarding.modules.kyc.dto.KycVerificationResult;

/**
 * Interfaz para proveedores de verificación KYC/AML
 * Implementaciones: MockKycProvider, DataCreditoProvider, etc.
 */
public interface KycProvider {

    /**
     * Verifica la identidad de un usuario
     * @param userId ID del usuario
     * @param dni Documento de identidad
     * @param fullName Nombre completo
     * @return Resultado de la verificación
     */
    KycVerificationResult verifyIdentity(String userId, String dni, String fullName);

    /**
     * Verifica documentos (biométricos, etc.)
     * @param userId ID del usuario
     * @param documentData Datos del documento
     * @return Resultado de la verificación
     */
    KycVerificationResult verifyDocument(String userId, Object documentData);

    /**
     * Realiza verificación completa (identidad + documentos)
     * @param userId ID del usuario
     * @param verificationData Datos para la verificación
     * @return Resultado de la verificación
     */
    KycVerificationResult performFullVerification(String userId, Object verificationData);

    /**
     * Obtiene el nombre del proveedor
     * @return Nombre del proveedor
     */
    String getProviderName();
}

