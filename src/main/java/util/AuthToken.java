package util;

import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.util.UUID;

public class AuthToken {

	public static final long EXPIRATION_TIME = 1000*60*60*2;
	
	public String username;
	public String role;
	public ValidityData validity;
	
	public AuthToken() {

	}
	
	public AuthToken(String username, String role, ValidityData validity) {
		this.username = username;
		this.role = role;
		this.validity = validity;
	}
	
}
