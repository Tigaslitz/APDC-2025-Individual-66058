package resources;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.*;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import util.AuthToken;

import java.util.logging.Logger;

@Path("/logout")
public class LogoutResource {

    private static final Logger LOG = Logger.getLogger(LogoutResource.class.getName());
    private final Gson g = new Gson();

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest request) {
        AuthToken token = (AuthToken) request.getAttribute("authToken");

        Key revokedKey = datastore.newKeyFactory().setKind("RevokedToken")
                .newKey(token.validity.verificador);

        Entity revokedToken = Entity.newBuilder(revokedKey)
                .set("revokedAt", Timestamp.now())
                .set("expiresAt", token.validity.to)
                .build();

        try {
            datastore.put(revokedToken);
            return Response.ok("Logout successful").build();
        } catch (DatastoreException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error revoking token")
                    .build();
        }
    }

}
