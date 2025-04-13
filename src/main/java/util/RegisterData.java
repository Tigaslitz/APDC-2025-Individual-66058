package util;

public class RegisterData {

	public String username;
	public String password;
	public String confirmation;
	public String email;
	public String name;
	public String phone;
	public String privacy;

	// Opcionais
	public String nif;
	public String address;
	public String role;
	public String accountStatus;
	public String employer;
	public String employerNif;
	public String job;
	public String identification;
	public String photo;


	public RegisterData() {

	}

	public RegisterData(String username, String password, String confirmation, String email, String name, String phone, String privacy) {
		this.username = username;
		this.password = password;
		this.confirmation = confirmation;
		this.email = email;
		this.name = name;
		this.phone = phone;
		this.privacy = privacy;

	}
	
	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}

	public boolean validRegistration() {
		return nonEmptyOrBlankField(username) &&
				nonEmptyOrBlankField(password) &&
				nonEmptyOrBlankField(confirmation) &&
				nonEmptyOrBlankField(email) &&
				nonEmptyOrBlankField(name) &&
				nonEmptyOrBlankField(phone) &&
				nonEmptyOrBlankField(privacy) &&

				isValidEmail(email) &&
				password.equals(confirmation) &&
				isValidPassword(password) &&
				isValidPhone(phone) &&
				isValidPrivacy(privacy);
	}

	private boolean isValidEmail(String email) {
		return email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
	}

	private boolean isValidPassword(String password) {
		// Pelo menos 8 caracteres, 1 maiúscula, 1 minúscula, 1 número e 1 símbolo
		return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$");
	}

	private boolean isValidPhone(String phone) {
		return phone.matches("\\d{9}");
	}

	private boolean isValidPrivacy(String privacy) {
		return privacy.equalsIgnoreCase("publico") || privacy.equalsIgnoreCase("privado");
	}

	private boolean isValidNif(String nif){
		return nif != null && nif.matches("\\d{9}");
	}


}
