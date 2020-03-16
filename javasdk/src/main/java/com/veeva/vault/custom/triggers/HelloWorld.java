package com.veeva.vault.custom.triggers;

import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.TriggerOrder;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.*;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.query.QueryResponse;
import com.veeva.vault.sdk.api.query.QueryResult;
import com.veeva.vault.sdk.api.query.QueryService;

import java.util.Map;
import java.util.Set;


/**
 * @class HelloWorld
 * HelloWorld implements RecordTrigger and adds the  "Hello, " prefix to any newly created object.
 */

@RecordTriggerInfo(object = "vsdk_hello_world__c", events = RecordEvent.BEFORE_INSERT, order=TriggerOrder.NUMBER_1)
public class HelloWorld implements RecordTrigger {

    public void execute(RecordTriggerContext recordTriggerContext) {

        // Get an instance of the Query Service used to find if record with same name exists
        QueryService queryService = ServiceLocator.locate(QueryService.class);

        // Retrieve Names from all Hello World input records
        Set<String> helloObjects = VaultCollections.newSet();

        // Get the names for all the records being created
        recordTriggerContext.getRecordChanges().stream().forEach(recordChange -> {
            String name = recordChange.getNew().getValue("name__v", ValueType.STRING);
            if(name != null || !name.isEmpty()) {
                helloObjects.add("'Hello, " + name + "'");
            }
        });

        String recordsToQuery = String.join (",", helloObjects);
        String queryRecord = "select id, name__v from vsdk_hello_world__c where name__v contains(" + recordsToQuery + ")" ;
        QueryResponse queryResponse = queryService.query(queryRecord);

        // Build a Map of Record Name (key) and Hello World Records (value) from the query result
        Map<String, QueryResult> nameRecordMap = VaultCollections.newMap();
        queryResponse.streamResults().forEach(queryResult -> {
            String name = queryResult.getValue("name__v", ValueType.STRING);
            if (!nameRecordMap.containsKey(name)) {
                nameRecordMap.put(name, queryResult);
            }
        });

        for (RecordChange inputRecord : recordTriggerContext.getRecordChanges()) {
            String recordName = inputRecord.getNew().getValue("name__v", ValueType.STRING);
            String helloName = "Hello, " + recordName;
            QueryResult existingRecord = nameRecordMap.get(helloName);
            String existingRecordId = null;
            if(existingRecord != null) {
                existingRecordId = existingRecord.getValue("id", ValueType.STRING);
            }

            if(existingRecordId != null) {
                // Create this record as a related record with name to be of type "Copy of 'Hello, name__v'" if record already exists
                inputRecord.getNew().setValue("name__v", "Copy of 'Hello, " + recordName + "'");
                inputRecord.getNew().setValue("related_to__c", existingRecordId);
            } else {
                // Record with same name does not exist, save
                inputRecord.getNew().setValue("name__v", helloName);
            }
        }
    }
}