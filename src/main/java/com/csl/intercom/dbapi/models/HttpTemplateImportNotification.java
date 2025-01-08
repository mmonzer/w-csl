package com.csl.intercom.dbapi.models;

import com.csl.intercom.dbapi.enums.FileAction;
import com.ucsl.json.Json;

import java.time.OffsetDateTime;

/**
 * Holds the information received from the server to notify a file is available for download, either through MQTT or directly from the HMI.
 */
public class HttpTemplateImportNotification {
    public static final String FILE_ACTION_STATUS_ID = "file_action_status_id";
    public static final String FILE_PATH = "file_path";
    public static final String UPLOADED_AT = "uploaded_at";
    public static final String FILE_NAME = "file_name";
    public static final String CLIENT_ACTION_ID = "client_action_id";
    public static final String CLIENT_ACTION = "client_action";
    public static final String STATUS = "status";
    public static final String NEW_FILE_NAME = "new_file_name";
    private int id;
    private String filePath;
    private String fileName;
    private OffsetDateTime uploadedAt;
    private Type type;

    private HttpTemplateImportNotification(int id, String filePath, String fileName, OffsetDateTime uploadedAt, FileAction fileAction, Type type) {
        this.id = id;
        this.filePath = filePath;
        this.fileName = fileName;
        this.uploadedAt = uploadedAt;
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
        FileAction fileAction = null;
        Type type = null;

        if (data == null) {
            return null;
        }

        if (data.has(FILE_ACTION_STATUS_ID)) {
            if ( data.get(FILE_ACTION_STATUS_ID).isNumber()) {
                id = data.get(FILE_ACTION_STATUS_ID).asInteger();
            } else if (data.get(FILE_ACTION_STATUS_ID).isString()) {
                id = Integer.parseInt(data.get(FILE_ACTION_STATUS_ID).asString());
            }
        }

        if (data.has(FILE_PATH) && data.get(FILE_PATH).isString()) {
            filePath = data.get(FILE_PATH).asString();
        }

        if (data.has(FILE_NAME) && data.get(FILE_NAME).isString()) {
            fileName = data.get(FILE_NAME).asString();
        }

        if (data.has(UPLOADED_AT) && data.get(UPLOADED_AT).isString()) {
            uploadedAt = OffsetDateTime.parse(data.get(UPLOADED_AT).asString());
        }

        if (data.has(CLIENT_ACTION_ID) && data.get(CLIENT_ACTION_ID).isNumber()) {
           fileAction = FileAction.fromValue(data.get(CLIENT_ACTION_ID).asInteger());
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

        if (data.has(FILE_PATH) && data.get(FILE_PATH).isString()) {
            filePath = data.get(FILE_PATH).asString();
        } else {
            return null;
        }

        if (data.has(NEW_FILE_NAME) && data.get(NEW_FILE_NAME).isString()) {
            fileName = data.get(NEW_FILE_NAME).asString();
        } else {
            return null;
        }

        if (data.has(UPLOADED_AT) && data.get(UPLOADED_AT).isString()) {
            uploadedAt = OffsetDateTime.parse(data.get(UPLOADED_AT).asString());
        } else {
            return null;
        }

        if (data.has(CLIENT_ACTION) && data.get(CLIENT_ACTION).isNumber()) {
            fileAction = FileAction.fromValue(data.get(CLIENT_ACTION).asInteger());
        } else {
            return null;
        }

        if (data.has(STATUS)) {
            int status_id;
            if (data.get(STATUS).isNumber()) {
                status_id = data.get(STATUS).asInteger();
            } else if (data.get(STATUS).isString()) {
                status_id = Integer.parseInt(data.get(STATUS).asString());
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
                case 2, 3:
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
