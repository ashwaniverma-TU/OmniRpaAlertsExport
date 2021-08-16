package helloworld;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.ErrorDto;
import dto.ResponseDto;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String TABLE_NAME_POSTFIX = "_Alert";
    private static Map<String, String> headers = new HashMap<>();

    {
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
    }

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);


        LambdaLogger logger = context.getLogger();
        logger.log("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
        logger.log("CONTEXT: " + gson.toJson(context));
        logger.log("EVENT: " + gson.toJson(input.getBody()));
        logger.log("EVENT TYPE: " + input.getClass().toString());


        List<Object> errorList = new ArrayList<>();
        try {
            AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
            DynamoDB dynamoDB = new DynamoDB(client);
            String requestString = input.getBody();
            //  Map<TABLE_NAME,Item>
            Map<String, List<Item>> tables = new HashMap<>();


            JSONArray jsonArray = new JSONArray(requestString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                String key = null;
                try {
                    key = object.getString("Key");
                } catch (Exception e) {

                    logger.log("Error::" + "Key not found");
                    object.put("Error", "Key json field is missing.");
                    errorList.add(convertToErrorMap(object));
                    continue;
                }

                if (StringUtils.isNullOrEmpty(key)) {
                    object.put("Error", "Empty Key");
                    errorList.add(convertToErrorMap(object));
                    continue;
                }


                String campaign = object.getString("Campaign");
                if (StringUtils.isNullOrEmpty(campaign)) {
                    object.put("Error", "Empty Campaign");
                    errorList.add(convertToErrorMap(object));
                    continue;
                }

                String tableName = campaign + TABLE_NAME_POSTFIX;
                if (tables.containsKey(tableName)) {
                    tables.get(tableName).add(new Item()
                            .withPrimaryKey("Key", key)
                            .withJSON("document", object.toString()));
                } else {
                    List<Item> items = new ArrayList<>();
                    items.add(new Item()
                            .withPrimaryKey("Key", key)
                            .withJSON("document", object.toString()));
                    tables.put(tableName, items);
                }
            }

            validateTables(tables, logger, dynamoDB, errorList);

            save(tables, logger, dynamoDB);

            ResponseDto responseDto = new ResponseDto();
            if (errorList.isEmpty()) {
                responseDto.setStatus("Success");
                responseDto.setCode(String.valueOf(HttpStatus.SC_CREATED));
            } else {
                responseDto.setStatus("Partially Success");
                responseDto.setCode(String.valueOf(HttpStatus.SC_CREATED));
                responseDto.setError(errorList);
            }

            return response
                    .withStatusCode(HttpStatus.SC_CREATED)
                    .withBody(gson.toJson(responseDto));
        } catch (Exception e) {
            logger.log("Error:: "+e.getMessage());
            ErrorDto error = new ErrorDto();
            error.setMessage(e.getMessage());
            error.setStatus("Error");
            return response
                    .withStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .withBody(gson.toJson(error));
        }
    }

    private void validateTables(Map<String, List<Item>> tables, LambdaLogger logger, DynamoDB dynamoDB, List<Object> errorList) {
        // validate table
        List<String> invalidTables = new ArrayList<>();
        for (String tableName : tables.keySet()) {
            try{
                dynamoDB.getTable(tableName).describe().getTableStatus();
            }catch (ResourceNotFoundException e){
                logger.log("Error: Table not found. TableName:: " + tableName + ". Please contact administrator for more information.");
                for (Item item : tables.get(tableName)) {
                    JSONObject object = new JSONObject(gson.toJson(item.get("document")));
                    object.put("Error", "Table not found. TableName:: " + tableName + ". Please contact administrator for more information.");
                    errorList.add(convertToErrorMap(object));
                }
                invalidTables.add(tableName);
            }
        }

        invalidTables.forEach(it -> {
            tables.remove(it);
        });
    }

    private void save(Map<String, List<Item>> tables, LambdaLogger logger, DynamoDB dynamoDB) {
        for (String tableName : tables.keySet()) {
            logger.log("Started Saving Data into " + tableName + " table");
            TableWriteItems tableWriteItems = new TableWriteItems(tableName)
                    .withItemsToPut(tables.get(tableName));
            BatchWriteItemOutcome outcome = dynamoDB.batchWriteItem(tableWriteItems);
            logger.log("Finished Saving Data into " + tableName + " table");
        }
    }
    private Map<String,Object> convertToErrorMap(JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        for (String key : jsonObject.keySet()) {
            map.put(key, jsonObject.get(key));
        }
        return map;
    }
}
