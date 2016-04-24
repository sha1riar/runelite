package net.runelite.client;

import com.google.gson.Gson;
import java.applet.Applet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientLoader
{
	private static final Logger logger = LoggerFactory.getLogger(ClientLoader.class);

	private static final String CLIENT_ARTIFACT_URL = "https://static.runelite.net/client.json";
	
	public Applet load() throws MalformedURLException, ClassNotFoundException, IOException, InstantiationException, IllegalAccessException, DependencyResolutionException
	{
		ConfigLoader config = new ConfigLoader();

		config.fetch();

		ArtifactResolver resolver;
		if (!RuneLite.getOptions().has("developer-mode"))
		{
			resolver = new ArtifactResolver(RuneLite.REPO_DIR);
			resolver.addRepositories();
		}
		else
		{
			resolver = new ArtifactResolver(new File(System.getProperty("user.home"), ".m2/repository"));

			logger.info("In developer mode, not fetching updates for client");
		}

		List<ArtifactResult> results = resolver.resolveArtifacts(getClientArtifact());
		File client = results.get(0).getArtifact().getFile();

		String initialClass = config.getProperty(ConfigLoader.INITIAL_CLASS).replace(".class", "");

		URLClassLoader loader = new URLClassLoader(new URL[]{ client.toURI().toURL() }, this.getClass().getClassLoader());
		Class<?> clientClass = loader.loadClass(initialClass);

		Applet rs = (Applet) clientClass.newInstance();

		rs.setStub(new RSStub(config, rs));
		
		return rs;
	}

	private static Artifact getClientArtifact() throws MalformedURLException, IOException
	{
		URL u = new URL(CLIENT_ARTIFACT_URL);
		URLConnection conn = u.openConnection();
		conn.setRequestProperty("User-Agent", "Mozilla/5.0");
		try (InputStream i = conn.getInputStream())
		{
			Gson g = new Gson();
			return g.fromJson(new InputStreamReader(i), DefaultArtifact.class);
		}
	}
}
