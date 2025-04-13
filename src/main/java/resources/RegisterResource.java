package resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import util.RegisterData;

@Path("/register")
public class RegisterResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();


	public RegisterResource() {}	// Default constructor, nothing to do
	

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerUser(RegisterData data) {

		LOG.fine("Attempt to register user: " + data.username);
		if (!data.validRegistration()) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}

		Transaction txn = datastore.newTransaction();

		try {
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = txn.get(userKey);
			
			// If the entity does not exist null is returned...
			if (user != null) {
				txn.rollback();
				return Response.status(Status.CONFLICT).entity("User already exists.").build();
			} else {
				 // ... otherwise
				Entity.Builder builder = Entity.newBuilder(userKey).set("user_email", data.email)
						.set("user_pwd", DigestUtils.sha512Hex(data.password))
						.set("user_name", data.name)
						.set("user_phone", data.phone)
						.set("user_privacy", data.privacy)
						.set("user_role", "enduser")		//By default
						.set("account_status", "DESATIVADA")
						.set("user_creation_time", Timestamp.now());		//By default

				// Opcionais
				if (data.nif != null) builder.set("user_nif", data.nif);
				if (data.address != null) builder.set("user_address", data.address);
				if (data.employer != null) builder.set("user_employer", data.employer);
				if (data.employerNif != null) builder.set("employer_nif", data.employerNif);
				if (data.job != null) builder.set("user_job", data.job);
				if (data.identification != null) builder.set("user_identification", data.identification);
				if (data.photo != null) builder.set("user_photo", data.photo); // cuidado se for imagem binária

				// get() followed by put() inside a transaction is ok...
				txn.put(builder.build());
				txn.commit();
				LOG.info("User registered " + data.username);
				return Response.ok().build();
			}
		}
		catch (DatastoreException e) {
			LOG.fine("erro");
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.toString()).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
}
