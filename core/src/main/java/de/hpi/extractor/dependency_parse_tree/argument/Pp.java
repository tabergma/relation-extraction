package de.hpi.extractor.dependency_parse_tree.argument;


import de.hpi.nlp.dependency_parse_tree.Node;
import de.hpi.nlp.extraction.dependency_parse_tree.TreeExtraction;

import java.util.List;

/**
 * Represents the typed dependency 'prepositional phrase'
 */
public class Pp extends Argument2 {

    private Node preopsition;

    public Pp(Node rootNode, TreeExtraction relation) {
        super(rootNode, relation, "PP");
        this.preopsition = null;

        // the noun of this object is connected via 'pn' to the preposition,
        // which is the root of an 'pp'
        // if there exists a 'pn', it becomes root, because possible conjunctions
        // are connected to it
        // the preposition is stored and added to each extraction in the end
        List<Node> pn = rootNode.getChildrenOfType("pn", "pp");
        if (pn.size() == 1) {
            this.rootNode = pn.get(0);
            this.preopsition = rootNode;
        }
    }

    @Override
    public Role getRole() {
        // if there is no preposition, 'pp' is not a valid argument
        if (preopsition == null || hasRelativeClause() || !this.containsNoun()) {
            return Role.NONE;
        }

        return Role.COMPLEMENT;
    }

    @Override
    public Node getPreposition() {
        return preopsition;
    }


}
