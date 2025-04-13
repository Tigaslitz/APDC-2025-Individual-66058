package util;

import com.google.cloud.datastore.Entity;

public class ListUsersData {
    public String username;
    public String email;
    public String name;
    public String phone;
    public String privacy;
    public String role;
    public String status;
    public String nif;
    public String employer;
    public String employerNif;
    public String job;
    public String identification;
    public String photo;

    // Para ENDUSERs (menos info)
    public ListUsersData(String username, String email, String name) {
        this.username = username;
        this.email = email;
        this.name = name;
    }


    public ListUsersData(Entity user) {
        this.username = user.getKey().getName();
        this.email = get(user, "user_email");
        this.name = get(user, "user_name");
        this.phone = get(user, "user_phone");
        this.privacy = get(user, "user_privacy");
        this.role = get(user, "user_role");
        this.status = get(user, "account_status");
        this.nif = get(user, "user_nif");
        this.employer = get(user, "user_employer");
        this.employerNif = get(user, "employer_nif");
        this.job = get(user, "user_job");
        this.identification = get(user, "user_identification");
        this.photo = get(user, "user_photo");
    }

    private String get(Entity user, String field) {
        return user.contains(field) ? user.getString(field) : "NOT DEFINED";
    }
}
