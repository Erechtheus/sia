package de.dfki.nlp.annotator;

import org.junit.Test;

import de.dfki.nlp.domain.ParsedInputText;

public class BannerNERAnnotatorTest {

    @Test
    public void annotate() {

        ParsedInputText parsedInputText = new ParsedInputText("29144722", null, null, null);
        BannerNERAnnotator bannerNERAnnotator = new BannerNERAnnotator();
        bannerNERAnnotator.annotate(parsedInputText);

        parsedInputText.setText("We present a multipurpose technology to encapsulate hydrophobic substances in micron-size emulsion droplets and capsules. The encapsulating agent is a comb-like stimuli-responsive copolymer comprising side chain surfactants attached to a methacrylic acid/ethylacrylate polyelectrolyte backbone. The composition and structure of the hydrophobic moieties of the side chains are customized in order to tune the particle morphology and the processing conditions. The technology exploits the synergy of properties provided by the copolymer: interfacial activity, pH responsiveness, and viscoelasticity. A one-pot process produces emulsion gels or capsule dispersions consisting of a hydrophobic liquid core surrounded by a polymer shell. The dispersions resist to high ionic strengths and exhibit long-term stability. The versatility of the method is demonstrated by encapsulating various hydrophobic substances covering a broad range of viscosities and polarities -conventional and technical oils, perfumes, alkyd paints - with a high degree of morphological and rheological control.");

        bannerNERAnnotator.annotate(parsedInputText);

    }
}
