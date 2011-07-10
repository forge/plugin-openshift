package com.redhat.openshift.resteasy;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.redhat.openshift.resteasy.model.CloudAccount;

@Path("/cloud-accounts")
public interface CloudAccountDao {
	@GET
	@Consumes("application/json")
	List<CloudAccount> list(@CookieParam("rh_sso") String sso);
}
