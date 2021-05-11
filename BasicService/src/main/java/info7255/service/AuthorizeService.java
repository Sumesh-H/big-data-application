package info7255.service;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AuthorizeService {

	//parse client id
	//TODO: insert your own google secret id here
	private static final JacksonFactory jacksonFactory = new JacksonFactory();
	GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new ApacheHttpTransport(), jacksonFactory)
			.setAudience(Collections.singletonList("your secret id"))
			.build();

	//verify google token
	public boolean authorize(String idTokenString) {

		try {
			GoogleIdToken idToken = verifier.verify(idTokenString);
			if (idToken != null) return true;
			return false;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;}
	}
}
