package com.veeva.vault.custom.triggers;

import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.*;

import java.util.List;



/**
 * @class HelloCreatedRelatedRecords
 * This class implements RecordTrigger and creates two Related records for each newly created record that starts with a "Hello" prefix.
 */

@RecordTriggerInfo(object = "vsdk_hello_world__c", events = RecordEvent.AFTER_INSERT, order=TriggerOrder.NUMBER_2)
public class HelloCreateRelatedRecords implements RecordTrigger {

    public void execute(RecordTriggerContext recordTriggerContext) {

        // Get an instance of the Query Service used to find if record with same name exists
        RecordService recordService = ServiceLocator.locate(RecordService.class);

        // Loop over created records.
        for (RecordChange inputRecord : recordTriggerContext.getRecordChanges()) {
            List<Record> recordList = VaultCollections.newList();

            String recordName = inputRecord.getNew().getValue("name__v", ValueType.STRING);
            String recordId = inputRecord.getNew().getValue("id", ValueType.STRING);

            // Only create Related To records if there is a record that was found.
            if(recordName.startsWith("Hello") && recordId != null) {

                // Crete first related record with name "Related to 'Hello name' 1"
                Record firstRelatedRecord = recordService.newRecord("vsdk_hello_world__c");
                firstRelatedRecord.setValue("name__v", "Related to '" + recordName + "' 1");
                // This field is used to create the relation between the original record and this one.
                firstRelatedRecord.setValue("related_to__c", recordId);

                // Create second related record.
                Record secondRelatedRecord = recordService.newRecord("vsdk_hello_world__c");
                secondRelatedRecord.setValue("name__v", "Related to '" + recordName + "' 2");
                // This field is used to create the relation between the original record and this one.
                secondRelatedRecord.setValue("related_to__c", recordId);

                // Add to our list.
                recordList.add(firstRelatedRecord);
                recordList.add(secondRelatedRecord);
            }
            // Save and list any errors that may have occured.
            recordService.batchSaveRecords(recordList).onErrors(batchOperationErrors -> {
                batchOperationErrors.stream().findFirst().ifPresent(error -> {
                    String errMsg = error.getError().getMessage();
                    int errPosition = error.getInputPosition();
                    String name = recordList.get(errPosition).getValue("name__v", ValueType.STRING);
                    throw new RollbackException("OPERATION_NOT_ALLOWED", "Unable to create vSDK records: "
                            + name + "related to record " + recordId + "with name: "  + recordName + " due to " + errMsg);
                });
            }).execute();
        }

    }
}