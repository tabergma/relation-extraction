package edu.washington.cs.knowitall.extractor.chunking.conf;

import edu.washington.cs.knowitall.nlp.extraction.chunking.ChunkedBinaryExtraction;

/**
 * Represents a confidence function for @{ChunkedBinaryExtraction} objects.
 *
 * @author afader
 */
public interface ConfidenceFunction {

    public double getConf(ChunkedBinaryExtraction extr) throws ConfidenceFunctionException;
}