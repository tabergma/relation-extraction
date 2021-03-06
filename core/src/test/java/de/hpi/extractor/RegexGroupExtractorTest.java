package de.hpi.extractor;

import com.google.common.collect.Iterables;
import de.hpi.extractor.chunking.RegexGroupExtractor;
import de.hpi.nlp.chunking.ChunkedSentence;
import de.hpi.nlp.extraction.chunking.SpanExtraction;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RegexGroupExtractorTest {

    public List<ChunkedSentence> sents = TestExtractions.sentences;

    @Before
    public void setUp() throws Exception {
    }

    public List<SpanExtraction> extract(RegexGroupExtractor extractor, ChunkedSentence sent)
        throws Exception {
        List<SpanExtraction> extrs = new ArrayList<SpanExtraction>();
        Iterables.addAll(extrs, extractor.extract(sent));
        return extrs;
    }

    @Test
    public void testExtract1() throws Exception {
        RegexGroupExtractor
            extractor =
            new RegexGroupExtractor(
                "(B-NP_np I-NP_np*) ist_tok der_tok Bürgermeister_tok von_tok (NE_pos)");
        List<SpanExtraction> extrs = extract(extractor, sents.get(0));
        assertEquals(1, extrs.size());
        SpanExtraction extr = extrs.get(0);
        assertEquals(2, extr.getNumFields());
        assertEquals("Mike", extr.getField(0).getTokensAsString());
        assertEquals("Seattle", extr.getField(1).getTokensAsString());
    }

    @Test
    public void testExtract2() throws Exception {
        RegexGroupExtractor
            extractor =
            new RegexGroupExtractor("(B-NP_np I-NP_np*) ist_tok der_tok (NN_pos) von_tok (NE_pos)");
        List<SpanExtraction> extrs = extract(extractor, sents.get(0));
        assertEquals(1, extrs.size());
        SpanExtraction extr = extrs.get(0);
        assertEquals(3, extr.getNumFields());
        assertEquals("Mike", extr.getField(0).getTokensAsString());
        assertEquals("Bürgermeister", extr.getField(1).getTokensAsString());
        assertEquals("Seattle", extr.getField(2).getTokensAsString());
    }

}
