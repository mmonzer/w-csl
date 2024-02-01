package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.FileAction;
import com.csl.intercom.dbapi.enums.FileActionStatus;
import com.ucsl.json.Json;

import java.time.OffsetDateTime;

/**
 * Holds the information received from the server to notify a file is available for download, either through MQTT or directly from the HMI.
 */
public class HttpTemplateImportNotification {
    private int id;
    private String filePath;
    private String fileName;
    private OffsetDateTime uploadedAt;
    private boolean fileReceived;
    private FileAction fileAction;
    private Type type;

    private HttpTemplateImportNotification(int id, String filePath, String fileName, OffsetDateTime uploadedAt, FileAction fileAction, Type type) {
        this.id = id;
        this.filePath = filePath;
        this.fileName = fileName;
        this.uploadedAt = uploadedAt;
        this.fileAction = fileAction;
        this.type = type;
    }

    /**
     * Create a new HttpTemplateImportNotification from a JSON object received from the HMI.
     *
     * @param data The JSON object received from the HMI.
     * @return The HttpTemplateImportNotification created from the JSON object, or null.
     */
    public static HttpTemplateImportNotification fromHMIJson(Json data) {
        if (data == null || !data.isObject()) {
            return null;
        }

        return fromMQTTMessage(data.get("results"));
    }

    /**
     * Create a new HttpTemplateImportNotification from a JSON object received from MQTT.
     *
     * @param data The JSON object received from MQTT.
     * @return The HttpTemplateImportNotification created from the JSON object, or null.
     */
    public static HttpTemplateImportNotification fromMQTTMessage(Json data) {
        int id = 0;
        String filePath = null;
        String fileName = null;
        OffsetDateTime uploadedAt = null;
        FileActionStatus fileActionStatus = null;
        FileAction fileAction = null;
        Type type = null;

        if (data == null) {
            return null;
        }

        if (data.has("file_action_status_id")) {
            if ( data.get("file_action_status_id").isNumber()) {
                id = data.get("file_action_status_id").asInteger();
            } else if (data.get("file_action_status_id").isString()) {
                id = Integer.parseInt(data.get("file_action_status_id").asString());
            }
        }

        if (data.has("file_path") && data.get("file_path").isString()) {
            filePath = data.get("file_path").asString();
        }

        if (data.has("file_name") && data.get("file_name").isString()) {
            fileName = data.get("file_name").asString();
        }

        if (data.has("uploaded_at") && data.get("uploaded_at").isString()) {
            uploadedAt = OffsetDateTime.parse(data.get("uploaded_at").asString());
        }

        if (data.has("client_action_id") && data.get("client_action_id").isNumber()) {
           fileAction = FileAction.fromValue(data.get("client_action_id").asInteger());
        }

        for (Type possibleType : Type.values()) {
            if (data.has(possibleType.getField()) && data.get(possibleType.getField()).isBoolean()) {
                type = possibleType;
                break;
            }
        }

        return new HttpTemplateImportNotification(id, filePath, fileName, uploadedAt, fileAction, type);
    }

    public static HttpTemplateImportNotification fromDbapiJson(Json data) {
        int id;
        String filePath;
        String fileName;
        OffsetDateTime uploadedAt;
        FileAction fileAction;
        Type type;

        if (data == null) {
            return null;
        }

        if (data.has("id") && data.get("id").isNumber()) {
            id = data.get("id").asInteger();
        } else if (data.has("id") && data.get("id").isString()) {
            id = Integer.parseInt(data.get("id").asString());
        } else {
            return null;
        }

        if (data.has("file_path") && data.get("file_path").isString()) {
            filePath = data.get("file_path").asString();
        } else {
            return null;
        }

        if (data.has("new_file_name") && data.get("new_file_name").isString()) {
            fileName = data.get("new_file_name").asString();
        } else {
            return null;
        }

        if (data.has("uploaded_at") && data.get("uploaded_at").isString()) {
            uploadedAt = OffsetDateTime.parse(data.get("uploaded_at").asString());
        } else {
            return null;
        }

        if (data.has("client_action") && data.get("client_action").isNumber()) {
            fileAction = FileAction.fromValue(data.get("client_action").asInteger());
        } else {
            return null;
        }

        if (data.has("status")) {
            int status_id;
            if (data.get("status").isNumber()) {
                status_id = data.get("status").asInteger();
            } else if (data.get("status").isString()) {
                status_id = Integer.parseInt(data.get("status").asString());
            } else {
                return null;
            }
            switch (status_id) {
                case 0:
                    type = Type.FILE_RECEIVED;
                    break;
                case 1:
                    type = Type.FILE_PROCESSING;
                    break;
                case 2:
                case 3:
                    type = Type.FILE_IMPORTED_FINISHED;
                    break;
                default:
                    return null;
            }
        } else {
            return null;
        }

        return new HttpTemplateImportNotification(id, filePath, fileName, uploadedAt, fileAction, type);
    }

    public int getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }

    public Type getType() {
        return type;
    }

    /**
     * The type of notification.
     * Actually, we are only interested in the file_received notification, but we need to be able to tell which type of notification we received,
     * as the server sends all notifications in the same topic.
     */
    public enum Type {
        FILE_RECEIVED("file_received"),
        FILE_PROCESSING("file_processing"),
        FILE_IMPORTED_FINISHED("file_imported_finished"),
        ;

        private final String field;

        private Type(String field) {
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }
}
