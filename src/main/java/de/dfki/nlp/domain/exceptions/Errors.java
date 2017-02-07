package de.dfki.nlp.domain.exceptions;

import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;

public enum Errors {

    UNKNOWN("-1", "Unkown error code"),

    FORMAT_ERROR("1", "The introduced format is not correct. Please, revise it and send your petition again."),
    INCORRECT_TRANSFER_REQUEST_METHOD("2", "The request method is invalid. Please, use POST in order to send your data."),
    VALIDATIONS_ON_SAVE("3", "The data could not be saved due to an unexpected error. Please, try again later."),
    EMPTY_DATA("4", "There is no data in the response. Please, add some data in the POST message."),
    PREDICTION_WITHOUT_TYPE("5", "At least one of the types are not recognized in our servers. Please, contact us to find a solution."),
    UNKNOWN_TYPE("6", "At least one of the types are not recognized in our servers. Please, contact us to find a solution."),
    DOCUMENT_NOT_FOUND("7", "Incorrect document id. At least one of the documents are not present in the BeCalm request"),
    NOT_VALID_BY_SCHEMA("8", "The structure of the output annotation file is incorrect. Please, revise them and try again."),
    REQUEST_EXPIRED("9", "The time for responding to the request has expired. Please, try again."),
    UNKNOWN_COMMUNICATION_ID("10", "This communication id does not correspond to your annotation server. Please, contact us to find a solution."),
    SERVER_VERSION_CHANGED("11", "The version of the requested server has changed. Please, send the annotation request again."),
    NEED_PARAMETERS("12", "Some mandatory parameters are missing for the current operation. Please, check the API REST documentation and try again."),
    REQUEST_CLOSED("13", "The request has been executed properly."),
    FIELD_NEEDED("14", "There are some fields missing in the annotation response. Please, revise them and try again."),
    INCORRECT_FIELD("15", "There are some incorrect fields in the annotation response. Please, revise them and try again."),
    REPEATED_PREDICTION("16", "There are some annotations overlaped in the annotation response. Please, revise them and try again."),
    JSON_ERROR("17", "The JSON response is invalid and some fields has some errors. Please, revise them and try again."),
    INCORRECT_STATE("18", "The annotation server state is incorrect. Please, revise them or contact us to find a solution."),
    DISALLOWED_ENCODING("19", "Your annotations do not have a valid UTF-8 encoding.Please check your encoding and try again");

    private static final Map<String, Errors> LOOKUP = Maps.uniqueIndex(
            Arrays.asList(Errors.values()),
            Errors::getErrorCode
    );

    @Getter
    public final String errorCode;
    public final String message;

    Errors(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public static Errors lookup(Object errorCode) {
        return LOOKUP.getOrDefault(errorCode, UNKNOWN);
    }
}