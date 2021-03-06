package de.hpi.extractor.dependency_parse_tree;

import de.hpi.extractor.Extractor;
import de.hpi.extractor.ExtractorException;
import de.hpi.extractor.dependency_parse_tree.argument.*;
import de.hpi.nlp.dependency_parse_tree.Node;
import de.hpi.nlp.extraction.dependency_parse_tree.TreeExtraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Extracts all candidates for objects and complements of the verb.
 */
public class DepConIEArgument2Extractor extends Extractor<TreeExtraction, TreeExtraction> {

    private boolean childArguments;
    private boolean progressiveExtraction;

    public DepConIEArgument2Extractor() {
        this(false, false);
    }

    /**
     * Creates a argument 2 extractor.
     * @param childArguments extract second argument also from child nodes?
     * @param progressiveExtraction extract all extractions, which can be found (also those with many arguments)
     */
    public DepConIEArgument2Extractor(boolean childArguments, boolean progressiveExtraction) {
        this.childArguments = childArguments;
        this.progressiveExtraction = progressiveExtraction;
    }

    @Override
    protected Iterable<TreeExtraction> extractCandidates(TreeExtraction rel)
        throws ExtractorException {
        List<TreeExtraction> extrs = new ArrayList<>();

        // Extract all possible candidates
        List<Node> candidates = extractObjectComplementCandidates(rel);

        // There are no possible objects/complements of verb
        if (candidates.isEmpty()) return extrs;

        // Convert candidates into arguments depending on their typed dependency
        List<Argument2> arguments = new ArrayList<>();
        for (Node n : candidates) {
            switch (n.getLabelToParent()) {
                case "objd":
                    arguments.add(new Objd(n, rel));
                    break;
                case "obja":
                    arguments.add(new Obja(n, rel));
                    break;
                case "obja2":
                    arguments.add(new Obja2(n, rel));
                    break;
                case "objg":
                    arguments.add(new Objg(n, rel));
                    break;
                case "objp":
                    arguments.add(new Objp(n, rel));
                    break;
                case "pred":
                    arguments.add(new Pred(n, rel));
                    break;
                case "pp":
                    arguments.add(new Pp(n, rel));
                    break;
                case "kom":
                    arguments.add(new Kom(n, rel));
                    break;
            }
        }

        // If there is only one object, add it to the list of extractions
        // But only if it can act as object - exception: pp
        if (arguments.size() == 1) {
            Argument2 arg = arguments.get(0);
            if ((arg.getName().equals("PP") && arg.getRole() != Role.NONE) || (arg.getRole() != Role.COMPLEMENT && arg.getRole() != Role.NONE)) {
                extrs.addAll(arg.createTreeExtractions());
            }
            return extrs;
        }

        // Group the arguments into groups (complement, object, both (complement or object), and none
        List<Argument2> complements = arguments.stream()
                .filter(x -> x.getRole() == Role.COMPLEMENT).collect(Collectors.toList());
        List<Argument2> objects = arguments.stream()
                .filter(x -> x.getRole() == Role.OBJECT).collect(Collectors.toList());
        List<Argument2> both = arguments.stream()
                .filter(x -> x.getRole() == Role.BOTH).collect(Collectors.toList());
        List<Argument2> none = arguments.stream()
                .filter(x -> x.getRole() == Role.NONE && x instanceof Objp).collect(Collectors.toList());

        // There is an argument, which requires more information (pronominal adverbs, such as 'deswegen')
        // Extracting any kind of relation, would lead to uninformative and non factual relations
        if (!none.isEmpty()) {
            return extrs;
        }

        // Add reflexive pronouns to the relation phrase.
        if (!complements.isEmpty()) {
            Argument2 reflexivePronoun = complements.stream().filter(x -> x.getRootNode().getPos().equals("PRF")).findAny().orElse(null);
            if (reflexivePronoun != null) {
                addToRelation(rel, reflexivePronoun);
                arguments.remove(reflexivePronoun);
                complements.remove(reflexivePronoun);
            }
        }

        // If there are two arguments, create an extraction
        if (complements.size() + objects.size() + both.size() <= 2) {

            // If we have a 'pp' and a 'pred', which is an adverb, 'pp' becomes the object
            if (argContains(arguments, "PRED", 1) && argContains(arguments, "PP", 1)) {
                Argument2 pred = getArg(arguments, "PRED").get(0);
                Argument2 pp = getArg(arguments, "PP").get(0);

                if (pred.getRole().equals(Role.COMPLEMENT) && !pp.getRole().equals(Role.NONE)) {
                    objects.add(pp);
                    complements.remove(pp);
                }
            }

            // If we have a 'kom' and a 'pp', 'pp' becomes the object
            if (argContains(arguments, "KOM", 1) && argContains(arguments, "PP", 1)) {
                Argument2 kom = getArg(arguments, "KOM").get(0);
                Argument2 pp = getArg(arguments, "PP").get(0);

                if (kom.getRole().equals(Role.BOTH) && !pp.getRole().equals(Role.NONE)) {
                    objects.add(pp);
                    complements.remove(pp);
                    addToRelation(rel, kom);
                    both.remove(kom);
                }
            }

            // If both arguments are PP, one can be the object
            if (argContainsOnly(arguments, "PP")) {
                Argument2 object = getObject(arguments);
                if (!object.getRole().equals(Role.NONE)) {
                    objects.add(object);
                    complements.remove(object);
                }
            }

            // Add the complements to the relation
            addToRelation(rel, complements);

            // Handle the arguments, which can be both
            if (objects.isEmpty() && !both.isEmpty()) {
                if (both.size() > 1) {
                    // the argument, which has the maximum distance to relation, is the object
                    Argument2 object = getObject(both);
                    extrs.addAll(object.createTreeExtractions());
                    // the other arguments are complements
                    both.remove(object);
                    addToRelation(rel, both);
                } else {
                    extrs.addAll(both.get(0).createTreeExtractions());
                }
                return extrs;
            }

            // Handle the arguments, which are objects
            if (!objects.isEmpty() && both.isEmpty()) {
                // Add the objects, which are close to the relation to the relation
                // The object with the maximum distance to the relation is the real object
                List<Argument2> c = new ArrayList<>();
                while (objects.size() != 1) {
                    Argument2 complement = getComplement(objects);
                    objects.remove(complement);
                    c.add(complement);
                }
                addToRelation(rel, c);
                extrs.addAll(objects.get(0).createTreeExtractions());
                return extrs;
            }

            // Handle the case of one object and one argument, which can be both
            if (objects.size() == 1 && both.size() == 1) {
                // The argument, which can be both, is the complement
                addToRelation(rel, both);
                // The object becomes the seconds argument
                extrs.addAll(objects.get(0).createTreeExtractions());
                return extrs;
            }
        }

        if (progressiveExtraction) {
            // There exists more than two arguments
            // Determine the object
            Argument2 object;
            if (!objects.isEmpty()) {
                object = getObject(objects);
                objects.remove(object);
            } else {
                object = getObject(both);
                both.remove(object);
            }

            // Add the remaining arguments to the relation phrase
            for (Argument2 c : objects) {
                addToRelation(rel, c);
            }
            for (Argument2 c : both) {
                addToRelation(rel, c);
            }

            if (object != null) extrs.addAll(object.createTreeExtractions());
            return extrs;
        }
        return extrs;
    }

    /**
     * Gets the argument, which has the longest distance to relation.
     * @param argument2s the arguments
     * @return the argument, which has the longest distance to relation
     */
    private Argument2 getObject(List<Argument2> argument2s) {
        List<Argument2> objectArguments = argument2s.stream().filter(x -> x.getName().startsWith("OBJ")).collect(Collectors.toList());
        if (objectArguments.size() == 1) {
            return objectArguments.get(0);
        }

        Argument2 object = null;
        int maxDistance = Integer.MIN_VALUE;
        for (Argument2 arg : argument2s) {
            int currDistance = arg.distanceToRelation();
            if (currDistance > maxDistance) {
                maxDistance = currDistance;
                object = arg;
            }
        }
        return object;
    }

    /**
     * Gets the argument, which has the longest distance to relation.
     * @param argument2s the arguments
     * @return the argument, which has the longest distance to relation
     */
    private Argument2 getComplement(List<Argument2> argument2s) {
        Argument2 complement = null;
        int minDistance = Integer.MAX_VALUE;
        for (Argument2 arg : argument2s) {
            int currDistance = arg.distanceToRelation();
            if (currDistance < minDistance) {
                minDistance = currDistance;
                complement = arg;
            }
        }
        return complement;
    }

    /**
     * Adds the complement nodes to the given relation.
     * @param rel         the relation
     * @param complements the list of complements
     */
    private void addToRelation(TreeExtraction rel, List<Argument2> complements) {
        if (complements.isEmpty()) {
            return;
        }

        List<Integer> ids = new ArrayList<>();
        for (Argument2 arg : complements) {
            ids.addAll(arg.getIds(false));
            if (arg.getPreposition() != null) {
                ids.add(arg.getPreposition().getId());
            }
        }

        ids.addAll((Collection<? extends Integer>) rel.getNodeIds());
        rel.setNodeIds(ids);
    }

    private void addToRelation(TreeExtraction rel, Argument2 complement) {
        List<Argument2> complements = new ArrayList<>();
        complements.add(complement);
        addToRelation(rel, complements);
    }

    /**
     * Extract candidates for objects and verb complements.
     * @param rel relation extraction
     * @return list of root nodes of candidates
     */
    private List<Node> extractObjectComplementCandidates(TreeExtraction rel) {
        List<Node> relNodes = rel.getRootNode().find(rel.getNodeIds());
        // First check if there is an argument directed connected to main verb of the relation
        List<Node> fullVerbs = relNodes.stream().filter(x -> x.getPos().startsWith("VV") || x.getPos().equals("VAFIN")).collect(
            Collectors.toList());
        List<Node> arguments = getArguments(fullVerbs);
        // If not also consider the other verb forms
        if (arguments.isEmpty()) {
            arguments = getArguments(relNodes);
        }

        if (this.childArguments) {
            // Check if there are arguments connected to conjunction child nodes
            if (arguments.isEmpty()) {
                relNodes = rel.getRootNode().find(rel.getKonNodeIds());
                arguments = getArguments(relNodes);
            }
            // Check if the root node of the relation has an argument
            if (arguments.isEmpty()) {
                relNodes = new ArrayList<>();
                relNodes.add(rel.getRootNode());
                arguments = getArguments(relNodes);
            }
        }

        return arguments;
    }

    private List<Node> getArguments(List<Node> relNodes) {
        // objects are directly connected to verbs
        List<Node> arguments = new ArrayList<>();
        for (Node n : relNodes) {
            List<Node> a = n.getChildren().stream()
                .filter(x -> x.getLabelToParent().equals("obja") ||
                        x.getLabelToParent().equals("obja2") ||
                        x.getLabelToParent().equals("objd") ||
                        x.getLabelToParent().equals("objg") ||
                        x.getLabelToParent().equals("objp") ||
                        x.getLabelToParent().equals("pred") ||
                        (x.getLabelToParent().equals("kom") && x.getWord().toLowerCase().equals("als")) ||
                        (x.getLabelToParent().equals("pp") && !x.getPos().equals("PROAV")))
                .collect(Collectors.toList());
            arguments.addAll(a);
        }

        return arguments.stream().filter(this::filterArgumentsWithRelativeClause).collect(Collectors.toList());
    }

    /**
     * If an argument has only two children, on of them being a relative clause, the argument should not be extracted.
     * It does not lead to an informative relation.
     * @param argument the argument
     * @return true, if the argument does not have a relative clause, false otherwise
     */
    private boolean filterArgumentsWithRelativeClause(Node argument) {
        return argument.getChildren().size() > 2 || argument.getChildrenOfType("rel").isEmpty();
    }

    private boolean argContains(List<Argument2> arguments, String type, int count) {
        return arguments.stream().filter(x -> x.getName().equals(type)).collect(Collectors.toList()).size() == count;
    }

    private boolean argContainsOnly(List<Argument2> arguments, String type) {
        return arguments.stream().filter(x -> !x.getName().equals(type)).collect(Collectors.toList()).isEmpty();
    }

    private List<Argument2> getArg(List<Argument2> arguments, String type) {
        return arguments.stream().filter(x -> x.getName().equals(type)).collect(Collectors.toList());
    }

}


