package org.reduct.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reduct.client.config.ServerProperties;
import org.reduct.client.util.TokenConstants;
import org.reduct.common.TokenURL;
import org.reduct.common.exception.ReductException;
import org.reduct.model.token.AccessToken;
import org.reduct.model.token.TokenPermissions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ReductTokenClientTest {

   private static final String TOKEN_NAME = "test";

   private TokenClient reductTokenClient;
   private ServerProperties serverProperties;
   private HttpClient httpClient;
   private String accessToken;

   @BeforeEach
   public void setup() {
      accessToken = "testToken";
      httpClient = mock(HttpClient.class);
      serverProperties = new ServerProperties(false, "localhost", 8383);
      reductTokenClient = new ReductTokenClient(serverProperties, httpClient, accessToken);
   }

   @Test
   void createToken_validDetails_returnToken() throws IOException, InterruptedException {
      HttpRequest httpRequest = buildCreateTokenRequest();
      HttpResponse<String> httpResponse = mock(HttpResponse.class);
      doReturn(200).when(httpResponse).statusCode();
      doReturn(TokenConstants.CREATE_TOKEN_SUCCESSFUL_RESPONSE).when(httpResponse).body();
      doReturn(httpResponse).when(httpClient).send(httpRequest, HttpResponse.BodyHandlers.ofString());

      AccessToken token = reductTokenClient.createToken(TOKEN_NAME,
              TokenPermissions.of(true, List.of("test-bucket"), List.of("test-bucket")));

      assertEquals(TokenConstants.EXPECTED_TOKEN_VALUE, token.value());
      assertEquals(TokenConstants.EXPECTED_CREATION_DATE, token.createdAt());
   }

   @Test
   void createToken_invalidTokenProvided_throwException() throws IOException, InterruptedException {
      HttpRequest httpRequest = buildCreateTokenRequest();
      HttpResponse<String> httpResponse = mock(HttpResponse.class);
      doReturn(401).when(httpResponse).statusCode();
      doReturn(httpResponse).when(httpClient).send(httpRequest, HttpResponse.BodyHandlers.ofString());

      ReductException reductException = assertThrows(ReductException.class,
              () -> reductTokenClient.createToken(TOKEN_NAME,
                      TokenPermissions.of(true, List.of("test-bucket"), List.of("test-bucket"))));

      assertEquals("The access token is invalid.", reductException.getMessage());
      assertEquals(401, reductException.getStatusCode());
   }

   @Test
   void createToken_tokenWithoutPrivilegesToCreateToken_throwException() throws IOException, InterruptedException {
      HttpRequest httpRequest = buildCreateTokenRequest();
      HttpResponse<String> httpResponse = mock(HttpResponse.class);
      doReturn(403).when(httpResponse).statusCode();
      doReturn(httpResponse).when(httpClient).send(httpRequest, HttpResponse.BodyHandlers.ofString());

      ReductException reductException = assertThrows(ReductException.class,
              () -> reductTokenClient.createToken(TOKEN_NAME,
                      TokenPermissions.of(true, List.of("test-bucket"), List.of("test-bucket"))));

      assertEquals("The access token does not have the required permissions.", reductException.getMessage());
      assertEquals(403, reductException.getStatusCode());
   }

   @Test
   void createToken_tokenAlreadyExists_throwException() throws IOException, InterruptedException {
      HttpRequest httpRequest = buildCreateTokenRequest();
      HttpResponse<String> httpResponse = mock(HttpResponse.class);
      doReturn(409).when(httpResponse).statusCode();
      doReturn(httpResponse).when(httpClient).send(httpRequest, HttpResponse.BodyHandlers.ofString());

      ReductException reductException = assertThrows(ReductException.class,
              () -> reductTokenClient.createToken(TOKEN_NAME,
                      TokenPermissions.of(true, List.of("test-bucket"), List.of("test-bucket"))));

      assertEquals("A token already exists with this name.", reductException.getMessage());
      assertEquals(409, reductException.getStatusCode());
   }

   @Test
   void createToken_bucketDoesNotExist_throwException() throws IOException, InterruptedException {
      HttpRequest httpRequest = buildCreateTokenRequest();
      HttpResponse<String> httpResponse = mock(HttpResponse.class);
      doReturn(422).when(httpResponse).statusCode();
      doReturn(httpResponse).when(httpClient).send(httpRequest, HttpResponse.BodyHandlers.ofString());

      ReductException reductException = assertThrows(ReductException.class,
              () -> reductTokenClient.createToken(TOKEN_NAME,
                      TokenPermissions.of(true, List.of("test-bucket"), List.of("test-bucket"))));

      assertEquals("One of the bucket names provided does not exist on the server.",
              reductException.getMessage());
      assertEquals(422, reductException.getStatusCode());
   }

   @Test
   void createToken_serverReturnInternalServerError_throwException() throws IOException, InterruptedException {
      HttpRequest httpRequest = buildCreateTokenRequest();
      HttpResponse<String> httpResponse = mock(HttpResponse.class);
      doReturn(500).when(httpResponse).statusCode();
      doReturn(httpResponse).when(httpClient).send(httpRequest, HttpResponse.BodyHandlers.ofString());

      ReductException reductException = assertThrows(ReductException.class,
              () -> reductTokenClient.createToken(TOKEN_NAME,
                      TokenPermissions.of(true, List.of("test-bucket"), List.of("test-bucket"))));

      assertEquals("The server returned an unexpected response. Please try again later.",
              reductException.getMessage());
      assertEquals(500, reductException.getStatusCode());
   }

   @Test
   void createToken_serverReturnsInvalidJson_throwException() throws IOException, InterruptedException {
      HttpRequest httpRequest = buildCreateTokenRequest();
      HttpResponse<String> httpResponse = mock(HttpResponse.class);
      doReturn(200).when(httpResponse).statusCode();
      doReturn("{{}").when(httpResponse).body();
      doReturn(httpResponse).when(httpClient).send(httpRequest, HttpResponse.BodyHandlers.ofString());

      ReductException reductException = assertThrows(ReductException.class,
              () -> reductTokenClient.createToken(TOKEN_NAME,
                      TokenPermissions.of(true, List.of("test-bucket"), List.of("test-bucket"))));

      assertEquals("The server returned a malformed response.",
              reductException.getMessage());
   }

   @Test
   void createToken_ioExceptionOccurs_throwException() throws IOException, InterruptedException {
      HttpRequest httpRequest = buildCreateTokenRequest();
      doThrow(IOException.class).when(httpClient).send(httpRequest, HttpResponse.BodyHandlers.ofString());
      ReductException result = assertThrows(ReductException.class, () -> reductTokenClient.createToken(TOKEN_NAME,
              TokenPermissions.of(true, List.of("test-bucket"), List.of("test-bucket"))));

      assertEquals("An error occurred while processing the request", result.getMessage());
   }

   @Test
   void createToken_threadInterrupted_throwException() throws IOException, InterruptedException {
      HttpRequest httpRequest = buildCreateTokenRequest();
      doThrow(InterruptedException.class).when(httpClient).send(httpRequest, HttpResponse.BodyHandlers.ofString());
      ReductException result = assertThrows(ReductException.class,
              () -> reductTokenClient.createToken(TOKEN_NAME,
                      TokenPermissions.of(true, List.of("test-bucket"), List.of("test-bucket"))));

      assertEquals("Thread has been interrupted while processing the request", result.getMessage());
   }

   @Test
   void createToken_tokenNameIsNull_throwException() {
      assertThrows(IllegalArgumentException.class, () -> reductTokenClient.createToken(null,
              TokenPermissions.of(true, List.of("test-bucket"), List.of("test-bucket"))));
   }

   @Test
   void createToken_tokenNameIsEmpty_throwException() {
      assertThrows(IllegalArgumentException.class, () -> reductTokenClient.createToken("",
              TokenPermissions.of(true, List.of("test-bucket"), List.of("test-bucket"))));
   }

   @Test
   void createToken_permissionsObjectIsNull_throwException() {
      assertThrows(IllegalArgumentException.class, () -> reductTokenClient.createToken(TOKEN_NAME, null));
   }

   @Test
   void constructClient_serverPropertiesNull_throwException() {
      assertThrows(IllegalArgumentException.class,
              () -> new ReductTokenClient(null, httpClient, accessToken));
   }

   private HttpRequest buildCreateTokenRequest() {
      String createTokenPath = TokenURL.CREATE_TOKEN.getUrl().formatted(TOKEN_NAME);
      URI createTokenUri = URI.create("%s/%s".formatted(serverProperties.getBaseUrl(), createTokenPath));
      return HttpRequest.newBuilder()
              .uri(createTokenUri)
              .POST(HttpRequest.BodyPublishers.ofString(TokenConstants.CREATE_TOKEN_REQUEST_BODY))
              .header("Authorization", "Bearer %s".formatted(accessToken))
              .build();
   }
}