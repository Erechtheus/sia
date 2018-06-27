package de.dfki.nlp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.dfki.nlp.domain.rest.ServerRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@ToString
public class IdList {
    String source;
    List<ServerRequest.Document> documents;

    @JsonIgnore
    public List<String> getIds() {
        return documents.stream().map(ServerRequest.Document::getDocument_id).collect(Collectors.toList());
    }

    public static IdList withIds(String source, List<String> ids) {
        return new IdList(source, ids.stream().map(i -> new ServerRequest.Document(i, source)).collect(Collectors.toList()));
    }
}