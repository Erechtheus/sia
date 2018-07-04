# Scalable Interoperable Annotation Server (SIA)
[![Build Status](https://api.travis-ci.org/Erechtheus/sia.svg?branch=master)](https://travis-ci.org/Erechtheus/sia)
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

Issue the following `curl` request to trigger a new annotation request with a sample payload

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
Afterwards, for correct message routing, it is necessary to define the input channel. Input channels can be freely named, but we recommend to use the name of the annotator.
For example:

```java
@Transformer(inputChannel = "yourAnnotator")
```

This annotation placed on the annotator defines that inputs are coming from the yourAnnotator channel. Internally channels are mapped to queues automatically.

* Add your annotator as recipient in [FlowHandler](https://github.com/Erechtheus/sia/blob/master/src/main/java/de/dfki/nlp/flow/FlowHandler.java#L248-L256) and 
  define the set of [PredictionType](https://github.com/Erechtheus/sia/blob/master/src/main/java/de/dfki/nlp/domain/PredictionType.java) your annotator responds to accordingly. 

For example:

```java
.recipientMessageSelector("yourAnnotator", message -> headerContains(message, CHEMICAL) && enabledAnnotators.yourAnnotator)
```

Here the `yourAnnotator` has to match the transformer `inputChannel` definition. And defines that all requests that need to be tagged with `CHEMICAL` will be send to the yourAnnotator channel.
 `headerContains(message, CHEMICAL)` is a helper method to check if in the header a field called `types` contains the enum CHEMICAL.
 The header is automatically populated from the request message containing the annotator types requested.
 
* Furthermore `enabledAnnotators` is an injected configuration [bean](https://github.com/Erechtheus/sia/blob/master/src/main/java/de/dfki/nlp/config/EnabledAnnotators.java)
  which allows to specify which annotators to enable.

Simply add a new boolean property with `yourAnnotator` to the class allows to control which annotators to enable. 
Check [application.properties](https://github.com/Erechtheus/sia/blob/master/src/main/resources/application.properties#L48).

 

Available Annotators
====================

* [BannerNER](#BannerNER)
* [DiseasesNER](#DiseasesNER)
* [Linnaeus](#Linnaeus)
* [MirNER](#MirNER)
* [SETH](#SETH)
* [ChemSpot](#ChemSpot-(external))
* [DNorm (external)](#DNorm-(external))

#### BannerNER

BANNER is a named entity recognition system, primarily intended for biomedical text.

<http://banner.sourceforge.net/>

#### DiseasesNER
DiseasesNER is using a large dictionary of desease mentiones.

#### Linnaeus
Species name recognition and normalization software.

<http://linnaeus.sourceforge.net/>

#### MirNER
mirNer is a simple regex based tool to detect MicroRna mentions in text, following the mi-RNA definition of Victor Ambroset al., (2003). 
A uniform system for microRNA annotation. RNA 2003 9(3):277-279.

<https://github.com/Erechtheus/mirNer>

#### SETH
SNP Extraction Tool for Human Variations.

SETH is a software that performs named entity recognition (NER) of genetic variants (with an emphasis on single nucleotide polymorphisms (SNPs) and other short sequence variations) from natural language texts. 

<https://rockt.github.io/SETH/>

#### ChemSpot (external)
ChemSpot is a named entity recognition tool for identifying mentions of chemicals in natural language texts, including trivial names, drugs, abbreviations, molecular formulas and IUPAC entities.

<https://www.informatik.hu-berlin.de/de/forschung/gebiete/wbi/resources/chemspot/chemspot>


#### DNorm (external)
DNorm is an automated method for determining which diseases are mentioned in biomedical text, the task of disease normalization. Diseases have a central role in many lines of biomedical research, making this task important for many lines of inquiry, including etiology (e.g. gene-disease relationships) and clinical aspects (e.g. diagnosis, prevention, and treatment).
 
<https://www.ncbi.nlm.nih.gov/CBBresearch/Lu/Demo/tmTools/DNorm.html>
 
### External annotators

DNorm and ChemSpot are integrated out of process. This means that you need to start the annotators before you can use them.
Communication is handled via a dedicated queue for each handler respectively.

- Start DNorm

      ./mvnw -f tools/dnorm/pom.xml -DskipTests package
      java -Xmx8g -jar tools/dnorm/target/dnorm-0.0.1-SNAPSHOT.jar

- Start ChemSpot

      ./mvnw -f tools/chemspot/pom.xml package
      java -Xmx12g -jar tools/chemspot/target/chemspot-0.0.1-SNAPSHOT.jar


## Tagging PubMed Dumps

You can simply tag pubmed articles from <ftp://ftp.ncbi.nlm.nih.gov/pubmed/baseline/> by putting them into the directory `tools/pubmedcache`.

Configure the annotators to use by creating an `application.properties` file in the current directory and add the annotators you want to use.
Then start any external annotators that you want to use.

If you don't customize the annotators, the following default configuration is applied:

```properties
sia.annotators.banner=false
sia.annotators.diseaseNer=false
sia.annotators.mirNer=false
sia.annotators.linnaeus=false
sia.annotators.seth=true

# external
sia.annotators.dnorm=false
sia.annotators.chemspot=false
```

Finally start the `SiaPubmedAnnotator` class with the _driver_ and _backend_ profile enabled.
The _driver_  profile ensures that output is collected into the directory `annotated`,
while the _backend_ profile ensures that the any internal annotators are started as well.


```bash
./mvnw -DskipTests package
java -cp target/sia-0.0.1-SNAPSHOT.jar \
     -Dloader.main=de.dfki.nlp.SiaPubmedAnnotator \
     org.springframework.boot.loader.PropertiesLauncher \
     --spring.profiles.active=backend,driver
```
    
Example output
    
```bash
$ ls -lh annotated
1.0K Jun 28 23:15 annotation-results_2018-06-28_11-15-07.json 
$ head annotated/a*
{"predictionResults":[{"document_id":"10022392","section":"A","init":1085,"end":1090,"score":1.0,"annotated_text":"T337A","type":"MUTATION"} ....
