package resources;

import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import util.AuthToken;
import util.ChangeAttributesData;
import Enum.Roles;
import Enum.States;
import util.ChangePasswordData;

import java.util.logging.Logger;

@Path("/changeAttributes")
public class UpdateResource {

    private static final Logger LOG = Logger.getLogger(UpdateResource.class.getName());
    private final Gson g = new Gson();

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeAttributes(ChangeAttributesData data,
                                     @Context HttpServletRequest request) {
        AuthToken token = (AuthToken) request.getAttribute("authToken");

        if (!data.validRegistration())
            return Response.status(Status.BAD_REQUEST).entity("Parameters invalid").build();

        Roles loggedInRole = Roles.valueOf(token.role);
        String targetUsername = data.targetUsername != null ? data.targetUsername : token.username;
        boolean isSelf = token.username.equals(targetUsername);


        Key userKey = datastore.newKeyFactory().setKind("User").newKey(targetUsername);
        Key loggedKey = datastore.newKeyFactory().setKind("User").newKey(token.username);
        Transaction txn = datastore.newTransaction();

        try {
            Entity user = txn.get(userKey);
            Entity loggedUser = txn.get(loggedKey);

            if (user == null) {
                txn.rollback();
                return Response.status(Status.NOT_FOUND).entity("User not found").build();
            }

            Roles targetRole =Roles.valueOf(user.getString("user_role"));

            if (!isSelf && !isAuthorizedToEdit(loggedInRole, targetRole)) {
                txn.rollback();
                return Response.status(Status.FORBIDDEN).entity("Permission denied").build();
            }

            if (States.valueOf(loggedUser.getString("account_status")) != States.ATIVADA){
                txn.rollback();
                return Response.status(Status.FORBIDDEN).entity("Account not active").build();
            }

            Entity.Builder builder = Entity.newBuilder(user);

            if(data.phone != null)
                builder.set("user_phone", data.phone);
            if(data.privacy != null)
                builder.set("user_privacy",data.privacy);
            if(data.nif != null)
                builder.set("user_nif", data.nif);
            if(data.address != null)
                builder.set("user_address",data.address);
            if(data.employer != null)
                builder.set("user_employer",data.employer);
            if(data.employerNif != null)
                builder.set("employer_nif", data.employerNif);
            if(data.job != null)
                builder.set("user_job", data.job);
            if(data.identification != null)
                builder.set("user_identification", data.identification);
            if(data.photo != null)
                builder.set("user_photo", data.photo);

            //BACKOFFICE
            if(loggedInRole.equals(Roles.BACKOFFICE) || loggedInRole.equals(Roles.ADMIN)) {
                if (data.role != null && !isSelf)
                    builder.set("user_role", data.role.toUpperCase());
                if (data.accountStatus != null && !isSelf)
                    builder.set("account_status", data.accountStatus.toUpperCase());
                if (data.name != null)
                    builder.set("user_name",data.name);
            }


            txn.put(builder.build());
            txn.commit();
            return Response.ok("Account updated successfully").build();

        } catch (Exception e) {
            txn.rollback();
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error updating user").build();
        } finally {
            if (txn.isActive()) txn.rollback();
        }
    }
    private boolean isAuthorizedToEdit(Roles role,Roles targetRole) {
        if (Roles.ADMIN.equals(role)) return true;
        if (Roles.BACKOFFICE.equals(role)) {
            return targetRole.equals(Roles.ENDUSER) || targetRole.equals(Roles.PARTNER);
        }
        return false;
    }



    @POST
    @Path("/password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(ChangePasswordData data,
                                   @Context HttpServletRequest request) {

        AuthToken token = (AuthToken) request.getAttribute("authToken");
        String username = token.username;
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
        Transaction txn = datastore.newTransaction();

        try {
            Entity user = txn.get(userKey);
            if (user == null) {
                txn.rollback();
                return Response.status(Status.NOT_FOUND).entity("User not found").build();
            }

            String storedPassword = user.getString("user_pwd");
            if (!storedPassword.equals(DigestUtils.sha512Hex(data.oldPassword))) {
                txn.rollback();
                return Response.status(Status.FORBIDDEN).entity("Incorrect old password").build();
            }

            if (!data.newPassword.equals(data.confirmation)) {
                txn.rollback();
                return Response.status(Status.BAD_REQUEST).entity("Password confirmation does not match").build();
            }

            if (!isValidPassword(data.newPassword)) {
                txn.rollback();
                return Response.status(Status.BAD_REQUEST).entity("Password does not meet requirements").build();
            }

            Entity updatedUser = Entity.newBuilder(user)
                    .set("user_pwd", DigestUtils.sha512Hex(data.newPassword))
                    .build();

            txn.put(updatedUser);
            txn.commit();
            return Response.ok("Password changed successfully").build();

        } catch (Exception e) {
            txn.rollback();
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing password").build();
        } finally {
            if (txn.isActive()) txn.rollback();
        }
    }

    public boolean isValidPassword(String password) {
        // Pelo menos 8 caracteres, 1 maiúscula, 1 minúscula, 1 número e 1 símbolo
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$");
    }

}
