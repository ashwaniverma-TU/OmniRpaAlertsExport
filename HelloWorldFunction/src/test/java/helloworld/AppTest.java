package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;

public class AppTest {
  @Test
  public void successfulResponse() throws Exception{
//    App app = new App();
//    APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
//    JSONParser parser = new JSONParser();
//    Object obj = parser.parse(new FileReader("src/test/java/helloworld/json/Badger.json"));
//    event.setBody(obj.toString());
//    Context context = new TestContext();
//    APIGatewayProxyResponseEvent result = app.handleRequest(event, context);
//    assertEquals(result.getStatusCode().intValue(), 200);
//    assertEquals(result.getHeaders().get("Content-Type"), "application/json");
//    String content = result.getBody();
//    assertNotNull(content);
//    assertTrue(content.contains("\"message\""));
//    assertTrue(content.contains("\"hello world\""));
//    assertTrue(content.contains("\"location\""));
  }
}
