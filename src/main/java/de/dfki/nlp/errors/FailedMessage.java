package de.dfki.nlp.errors;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FailedMessage {

    String id;

    String serverErrorCause;
    String serverErrorPayload;
    int communicationId;
    String failedMessagePayload;

}
