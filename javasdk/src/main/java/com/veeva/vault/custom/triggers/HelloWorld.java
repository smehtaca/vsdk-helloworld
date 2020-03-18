package com.veeva.vault.custom.triggers;

import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.TriggerOrder;
import com.veeva.vault.sdk.api.core.VaultCollections;
import com.veeva.vault.sdk.api.data.*;
import com.veeva.vault.sdk.api.core.ValueType;
import com.veeva.vault.sdk.api.query.QueryResponse;
import com.veeva.vault.sdk.api.query.QueryResult;
import com.veeva.vault.sdk.api.query.QueryService;
import com.veeva.vault.sdk.api.core.LogService;

import java.util.Map;
import java.util.Set;


/**
 * @class HelloWorld
 * HelloWorld implements RecordTrigger and adds the  "Hello, " prefix to any newly created record.
 * @author Sameer Mehta
 */

/**
 * This class annotation (@RecordTriggerInfo) indicates that this class is a record trigger.
 * It specifies the object that this trigger will run on(vsdk_hello_world__c), the events it will run on(BEFORE_INSERT) and the order(1st).
 */
@RecordTriggerInfo(object = "vsdk_hello_world__c", events = RecordEvent.BEFORE_INSERT, order=TriggerOrder.NUMBER_1)
public class HelloWorld implements RecordTrigger {

    public void execute(RecordTriggerContext recordTriggerContext) {

        /**
         * Get an instance of the Query Service used to find if record with same name exists.
         * The Query service is used to execute VQL queries to retrieve documents or object record.
         * VQL is a Structured Query Language(SQL) like querying language used to access document or object records.
         * More information about VQL Queries can be found here: https://developer.veevavault.com/vql/#introduction-to-vault-queries
         * Query Service Java Doc can be found here: https://repo.veevavault.com/javadoc/vault-sdk-api/19.3.5/docs/api/index.html
        */
        QueryService queryService = ServiceLocator.locate(QueryService.class);

        /**
         * Get an instance of the Log Service used to log errors and exceptions.
         * Log Service Java Doc can be found here: https://repo.veevavault.com/javadoc/vault-sdk-api/19.3.5/docs/api/index.html
         */
        LogService logService = ServiceLocator.locate(LogService.class);

        // Retrieve Names from all Hello World input records
        // This set is used to store our retrieved records.
        Set<String> helloObjects = VaultCollections.newSet();

        // Get the names for all the records being created
        recordTriggerContext.getRecordChanges().stream().forEach(recordChange -> {
            // Getting the name inputed by the user.
            String name = recordChange.getNew().getValue("name__v", ValueType.STRING);
            // Making sure to =not select records with Related to and Copy of in the name.
            if((name != null || !name.isEmpty()) && !name.startsWith("Related to") && !name.startsWith("Copy of")) {
                helloObjects.add(" 'Hello " + name + "'");
            }
        });

        // Converting the set to a string to be used when querying.
        String recordsToQuery = String.join (",", helloObjects);
        // Making sure our records query has names to query.
        if(recordsToQuery != null && !recordsToQuery.isEmpty()) {
            String queryRecord = "select id, name__v from vsdk_hello_world__c where name__v=" + recordsToQuery;
            // Logging our query.
            logService.info("Records to query:" + queryRecord);

            // Running our query and getting the response.
            QueryResponse queryResponse = queryService.query(queryRecord);

            // Build a Map of Record Name (key) and Hello World Records (value) from the query result
            Map<String, QueryResult> nameRecordMap = VaultCollections.newMap();
            queryResponse.streamResults().forEach(queryResult -> {
                String name = queryResult.getValue("name__v", ValueType.STRING);
                if (!nameRecordMap.containsKey(name)) {
                    nameRecordMap.put(name, queryResult);
                }
            });


            // Looping over each record that was inputted.
            for (RecordChange inputRecord : recordTriggerContext.getRecordChanges()) {
                // Getting the name that was inputted.
                String recordName = inputRecord.getNew().getValue("name__v", ValueType.STRING);
                String helloName = "Hello " + recordName;

                // Check if the inputted name prefixed with "Hello " exists
                QueryResult existingRecord = nameRecordMap.get(helloName);
                String existingRecordId = null;

                // If the record does not exist we get the id to use to set on the copy.
                if(existingRecord != null) {
                    existingRecordId = existingRecord.getValue("id", ValueType.STRING);
                }

                if(existingRecordId != null) {
                    // Create this record as a related record with name to be of type "Copy of 'Hello name__v'" if record already exists
                    inputRecord.getNew().setValue("name__v", "Copy of 'Hello, " + recordName + "'");
                    inputRecord.getNew().setValue("related_to__c", existingRecordId);
                } else {
                    // Record with same name does not exist, save
                    inputRecord.getNew().setValue("name__v", helloName);
                }
            }
        }

    }
}