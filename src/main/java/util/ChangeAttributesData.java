package util;

import Enum.Roles;
import  Enum.States;

public class ChangeAttributesData {
    public String targetUsername;
    public String name;
    public String phone;
    public String privacy;
    public String role;
    public String accountStatus;
    public String nif;
    public String address;
    public String employer;
    public String employerNif;
    public String job;
    public String identification;
    public String photo;


    public boolean validRegistration() {
        return isValidPhone(phone) && 
                isValidPrivacy(privacy) &&
                isValidStatus(accountStatus) &&
                isValidRole(role);
    }


    private boolean isValidRole(String role) {
        if (role != null) {
            try {
                Roles.valueOf(role.toUpperCase()); // ou .equalsIgnoreCase()
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidStatus(String accountStatus) {
        if (accountStatus != null) {
            try {
                States.valueOf(accountStatus.toUpperCase()); // ou .equalsIgnoreCase()
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }


    private boolean isValidPhone(String phone) {
        return phone == null || phone.matches("\\d{9}");
    }

    private boolean isValidPrivacy(String privacy) {
        return privacy == null || privacy.equalsIgnoreCase("publico") || privacy.equalsIgnoreCase("privado");
    }
}

