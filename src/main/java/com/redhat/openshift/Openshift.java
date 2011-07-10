package com.redhat.openshift;

import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.jboss.forge.project.Project;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeIn;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.redhat.openshift.resteasy.CloudAccountDao;
import com.redhat.openshift.resteasy.RedHatSSODao;
import com.redhat.openshift.resteasy.model.CloudAccount;

@Alias("openshift")
@Singleton
public class Openshift implements org.jboss.forge.shell.plugins.Plugin {
	@Inject
	private ShellPrompt prompt;
	
	@Inject
	private Project project;
	
	private ClientExecutor clientExecutor;
	
	public Openshift() throws Exception {
		ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
		RegisterBuiltin.register(providerFactory);
		clientExecutor = createExecutor();
	}
	@Produces
	public ClientExecutor createExecutor(){
		SSLSocketFactory sslFactory = null;
		try{
			SSLContext ctx = SSLContext.getInstance("TLS");
			if(true){
		        X509TrustManager tm = new X509TrustManager() {
		
		            public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
		            }
		
		            public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
		            }
		
		            public X509Certificate[] getAcceptedIssuers() {
		                return null;
		            }
		        };
				ctx.init(null, new TrustManager[]{tm}, null);
			}
			sslFactory = new SSLSocketFactory(ctx);
			sslFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		}catch(Exception e){
			e.printStackTrace();
		}
		
	    HttpParams params = new BasicHttpParams();
	    ConnManagerParams.setMaxTotalConnections(params, 3);
	    ConnManagerParams.setTimeout(params, 60000);
	    SchemeRegistry schemeRegistry = new SchemeRegistry();
	    schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", sslFactory, 443));
	    ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
	    HttpClient httpClient = new DefaultHttpClient(cm, params);
	    return new ApacheHttpClient4Executor(httpClient);
	}
	private String login(String login, String password, PipeOut out){
		do{
			if(login == null){
				out.print("Login: ");
				login = prompt.prompt();
			}
			if(login.trim().length() < 6){
				out.print(ShellColor.RED, "Login must be at least 6 characters\n");
				login = null;
			}
			if(login.matches("[\"$\\^<>|%/;:,\\\\*=~]")){
				out.print(ShellColor.RED, "Login may not contain any of these characters: (\") ($) (^) (<) (>) (|) (%) (/) (;) (:) (,) (\\) (*) (=) (~)\n");
				login = null;
			}
		}while(login == null);
		
		if(password == null || password.trim().equals("")){
			out.print("Password: ");
			password = prompt.prompt();
		}
		
		out.print(ShellColor.BOLD, "Logging into Openshift Flex as " + login + "\n");
		RedHatSSODao loginClient = ProxyFactory.create(RedHatSSODao.class, "https://www.redhat.com");
		Response response = loginClient.login(login, password); //6M3RhfbTQV
		String ssoCookie = (String) response.getMetadata().get("Set-Cookie").get(0);
		String cookieValue = ssoCookie.split(";")[0].split("=")[1];
		return cookieValue;
	}
	
	private String repeat(String pattern, int times){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<times;i++)
			sb.append(pattern);
		return sb.toString();
	}
	
	private void printRowDelim(int[] columnSizes, int indent, PipeOut out){
		out.print(repeat("    ", indent));
		out.print("+");
		for(int i=0;i<columnSizes.length;i++){
			out.print(repeat("-", columnSizes[i] + 2));
			out.print("+");
		}
		out.println();
	}
	
	private void printTable( String[] columnHeader, String[] fieldNames, int[] columnSizes, List<?> data, int indent, PipeOut out) throws Exception{
		printRowDelim(columnSizes, indent, out);
		out.print(repeat("    ", indent));
		out.print("| ");
		for(int i=0;i<columnHeader.length;i++){
			out.print(String.format("%" + columnSizes[i] + "s ", columnHeader[i]));
			out.print("| ");
		}
		out.println();
		printRowDelim(columnSizes, indent, out);
		
		for (Object object : data) {
			out.print(repeat("    ", indent));
			out.print("| ");
			for(int i=0;i<fieldNames.length;i++){
				Method mth = object.getClass().getMethod("get" + fieldNames[i], new Class[]{});
				String str = mth.invoke(object).toString();
				out.print(String.format("%" + columnSizes[i] + "s ", str));
				out.print("| ");
			}
			out.println();
		}
		printRowDelim(columnSizes, indent, out);
	}
	
	@Command("list-clouds")
	public void listClouds(@PipeIn String in, PipeOut out,
			@Option(name="loginName",required=false) String loginName,
			@Option(name="password",required=false) String password){
		String sso = login(loginName,password,out);
		
		CloudAccountDao cloudAccountDao = ProxyFactory.create(CloudAccountDao.class, "https://192.168.10.6/cosmodrome/rest", clientExecutor);
		List<CloudAccount> list = cloudAccountDao.list(sso);
		try {
			printTable(new String[]{"Environment Id", "Name", "Type"},
					   new String[]{"Id"			,"Name"	, "Type"},
					   new int[]{   10				,15		,20},
					   list, 0, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	@Command("deploy")
//	public void deploy(@PipeIn String in, PipeOut out,
//			@Option(name="loginName") String loginName, 
//			@Option(name="cloudProvider", completer=CloudProviderCompleter.class) String cloudProvider 
//			)
//	{
//		String password = prompt.prompt("Enter your OpenShift password.");
//		
//		FileResource<?> finalArtifact = (FileResource<?>) project.getFacet(PackagingFacet.class).getFinalArtifact();
//		finalArtifact.exists();
//	}
//	
//	@Command
//	public void undeploy(@PipeIn InputStream in, PipeOut out)
//	{
//		
//	}
	
}
