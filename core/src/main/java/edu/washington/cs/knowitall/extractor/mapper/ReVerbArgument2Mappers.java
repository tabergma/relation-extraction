package edu.washington.cs.knowitall.extractor.mapper;

import edu.washington.cs.knowitall.nlp.extraction.ChunkedArgumentExtraction;

/**
 * A list of mappers for <code>ReVerbExtractor</code>'s first arguments.
 *
 * @author afader
 */
public class ReVerbArgument2Mappers extends
                                    MapperList<ChunkedArgumentExtraction> {

    public ReVerbArgument2Mappers() {
        init();
    }

    private void init() {
        // Second argument can't be a Wh word
        addFirstPosTagNotEqualsFilter("PWS");
        addFirstPosTagNotEqualsFilter("PWAT");
        addFirstPosTagNotEqualsFilter("PWAV");

        // Second argument can't be a number
        addFirstPosTagNotEqualsFilter("CARD");

        // Can't be reflexive pronoun
        addFirstPosTagNotEqualsFilter("PRF");   // sich
        addFirstPosTagNotEqualsFilter("PDS");   // dieser, jener
        addFirstPosTagNotEqualsFilter("PPOSS"); // meins, deiner
        addFirstPosTagNotEqualsFilter("PIS");   // man
        addFirstPosTagNotEqualsFilter("PPER");  // er

        // Can't be
        addFirstTokenNotEqualsFilter("diese");
        addFirstTokenNotEqualsFilter("dieser");
        addFirstTokenNotEqualsFilter("diese");

        // First argument can't match "REL, ARG2" or "REL and ARG2"
        addMapper(new ConjunctionCommaRightArgumentFilter());

        // Second argument should be closest to relation that passes through
        // filters
        addMapper(new ClosestArgumentMapper());

        // Second argument should be adjacent to the relation
        addMapper(new AdjacentToRelationFilter());
    }

    private void addFirstPosTagNotEqualsFilter(String posTag) {
        final String posTagCopy = posTag;
        addMapper(new FilterMapper<ChunkedArgumentExtraction>() {
            public boolean doFilter(ChunkedArgumentExtraction extr) {
                return !extr.getPosTags().get(0).equals(posTagCopy);
            }
        });
    }

    private void addFirstTokenNotEqualsFilter(String token) {
        final String tokenCopy = token;
        addMapper(new FilterMapper<ChunkedArgumentExtraction>() {
            public boolean doFilter(ChunkedArgumentExtraction extr) {
                return !extr.getPosTags().get(0).equals(tokenCopy);
            }
        });
    }

}
