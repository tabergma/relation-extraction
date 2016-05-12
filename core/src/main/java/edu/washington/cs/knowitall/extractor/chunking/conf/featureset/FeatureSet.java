package edu.washington.cs.knowitall.extractor.chunking.conf.featureset;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.TreeSet;

public abstract class FeatureSet<T> {

    protected final ImmutableList<String> featureNames;

    public FeatureSet(List<String> featureNames) {
        // this is stupid, but Weka IMPLICITLY requires attributes to be in
        // sorted order.
        this.featureNames = ImmutableList.copyOf(new TreeSet<String>(
            featureNames));
    }

    public abstract double featurize(String featureName, T object);

    public double[] featurizeToDouble(T object) {
        double[] values = new double[this.featureNames.size()];

        int i = 0;
        for (String name : this.featureNames) {
            values[i] = this.featurize(name, object);
            i++;
        }

        return values;
    }

    /**
     * @return the number of features in this feature set
     */
    public int getNumFeatures() {
        return this.featureNames.size();
    }

    /**
     * Returns the feature names as an ImmutableList, sorted using the default String comparator.
     */
    public List<String> getFeatureNames() {
        return this.featureNames;
    }
}