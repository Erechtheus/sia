# seth-tips
[![Build Status](https://travis-ci.org/Erechtheus/seth-tips.svg?branch=master)](https://travis-ci.org/Erechtheus/seth-tips)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

##Project description
Seth-tips is an annotation service according to the BioCreative V.5. BeCalm task (http://www.becalm.eu/files/material/BioCreative.V.5_CFP.pdf)[TIPS].
Annotations for mutation mentions are generated using (https://github.com/rockt/SETH)[SETH], and returned in JSON according to these  (http://www.becalm.eu/files/schemas/jsonSchema.json)[definitions]. 


### getAnnotation

    curl -vX POST http://localhost:8080/call -d @src/test/resources/samplepayloadGetannotations.json --header "Content-Type: application/json"

### getStatus

    curl -vX POST http://localhost:8080/call -d @src/test/resources/sampleplayloadGetStatus.json --header "Content-Type: application/json"

