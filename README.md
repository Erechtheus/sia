# Scalable Interoperable Annotation Server (SIA)
[![Build Status](https://travis-ci.org/Erechtheus/sia.svg?branch=master)](https://travis-ci.org/Erechtheus/sia)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Project description
SIA is an annotation service according to the BioCreative V.5. BeCalm task [TIPS](http://www.becalm.eu/files/material/BioCreative.V.5_CFP.pdf).
Annotations for mutation mentions are generated using [SETH](https://github.com/rockt/SETH), [mirNer](https://github.com/Erechtheus/mirNer), and diseases using a dictionary lookup. Results are returned in JSON according to these  [definitions](http://www.becalm.eu/files/schemas/jsonSchema.json). 

## Citation
To cite SIA, please use the following reference:
```bibtex
@InProceedings{Kirschnick2017,
  Title                    = {SIA: Scalable Interoperable Annotation Server},
  Author                   = {Johannes Kirschnick and Philippe Thomas},
  Booktitle                = {Proceedings of the BioCreative V.5 Challenge Evaluation Workshop.},
  Year                     = {2017},
  Address                  = {Barcelona, Spain},
  Pages                    = {138--145}
}
```
A PDF version is freely available [here](http://www.biocreative.org/media/store/files/2017/BioCreative_V5_paper19.pdf)


## Getting Started

> #### Note
> The system uses RabbitMQ to load balance, so make sure it is running locally before starting the application, refer to [how to install RabbitMQ](https://www.rabbitmq.com/download.html) for help.

> If you want to skip the RabbitMQ installation, for convenience, you can just start it via maven (this might not work on your machine)

     ./mvnw rabbitmq:start

> And issue the following to tear down RabbitMQ afterwards

     ./mvnw rabbitmq:stop

To start the system in development mode issue

    ./mvnw spring-boot:run

This starts the backend without submitting results to the tips server, instead results are printed to the console.
The server is listening on port `8080` per default.

### getAnnotation

Issue the following `curl` request to trigger a new annotation request

    curl -vX POST http://localhost:8080/call -d @src/test/resources/samplepayloadGetannotations.json --header "Content-Type: application/json"

### getStatus

To trigger a get status report, use the following `curl` request

    curl -vX POST http://localhost:8080/call -d @src/test/resources/sampleplayloadGetStatus.json --header "Content-Type: application/json"

#### close hanging requests

    curl -H "Content-Type: application/json" -X POST -d  '' http://www.becalm.eu/api/saveAnnotations/JSON?apikey={apikey}&communicationId={communicationId}


## Adding custom annotators
To extend SIA for additional Named Entity Recognition tools you have to:

* Implement the Annotator [interface](https://github.com/Erechtheus/sia/blob/master/src/main/java/de/dfki/nlp/annotator/Annotator.java)

Consult the examples in the corresponding [package](https://github.com/Erechtheus/sia/tree/master/src/main/java/de/dfki/nlp/annotator) for implementation details. 
Afterwards, for correct message routing, it is necessary to define the input and output channels. Input channels can be freely named, but the output channel requires the name **parsed**.
For example:

```java
@Transformer(inputChannel = "yourAnnotator", outputChannel = "parsed")
```

This annotation placed on the annotator defines that inout are coming from the yourAnnotator channel and results of the method will be routed by the system to the _parsed_ channel. Inernally channels are mapped to queues automatically.

* Add the your annotator as recipient in [FlowHandler](https://github.com/Erechtheus/sia/blob/master/src/main/java/de/dfki/nlp/flow/FlowHandler.java#L169-L171) and set the [PredictionType](https://github.com/Erechtheus/sia/blob/master/src/main/java/de/dfki/nlp/domain/PredictionType.java) accordingly. 

For example:

```java
.recipient("yourAnnotator", "headers['types'].contains(T(de.dfki.nlp.domain.PredictionType).CHEMICAL)")
```

Here the `yourAnnotator` has to match the transformer `inputChannel` definition. And defines that all requests that need to be tagged with `CHEMICAL` will be send to the yourAnnotator channel. `headers['types'].contains` is a SPeL expression which can be used to evaluate scripts against message. In this case, checking if in the header a field called `types` contains the enum CHEMICAL.
