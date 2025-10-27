
package tech.nocountry.onboarding.dto;

import lombok.Getter;
import lombok.Setter;
import tech.nocountry.onboarding.entities.Role;

@Getter
@Setter
public class RoleDTO {
    
    private String id;
    private String name;
    private String description;
    private String permissions;
    
    // Constructor que recibe Role entity
    public RoleDTO(Role role) {
        this.id = role.getRoleId();
        this.name = role.getName();
        this.description = role.getDescription();
        this.permissions = role.getPermissions();
    }
}
