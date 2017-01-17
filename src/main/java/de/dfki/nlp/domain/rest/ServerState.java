package de.dfki.nlp.domain.rest;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class ServerState {

    State state;
    String version;
    String version_changes;
    String max_analyzable_documents;

    public enum State {
        Running("The server is enabled and working properly. This is the normal state. This state includes conditions when the database server is available and waiting for jobs."),
        Suspended("The server has been disabled by the administrator (e.g. server down for maintenance)."),
        Shutdown("The server is shutdown or not responds."),
        Overloaded("All resources in the server are busy."),
        Unknown("The state of the server is unknown."),
        Working("The server is already working in one project and can not be used at the moment."),
        Pending("There is no response from the server yet.");

        private String description;

        State(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

}
