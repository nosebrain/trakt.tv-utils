package de.nosebrain.trakt.util;

import com.uwetrottmann.trakt.v2.TraktV2;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import java.util.Scanner;

public class AuthUtil {

  public static TraktV2 getAccess(final String clientId, final String clientSecret) throws OAuthProblemException, OAuthSystemException {
      final TraktV2 trakt = new TraktV2();
      trakt.setApiKey(clientId);

      System.out.println("Enter authCode: ");
      final Scanner sc = new Scanner(System.in);
      final String authCode = sc.next();
      sc.close();
      System.out.println("getting token");
      final OAuthAccessTokenResponse code = TraktV2.getAccessToken(clientId, clientSecret, "urn:ietf:wg:oauth:2.0:oob", authCode);
      final String accessToken = code.getAccessToken();

        trakt.setAccessToken(accessToken);
      return trakt;
    }

}