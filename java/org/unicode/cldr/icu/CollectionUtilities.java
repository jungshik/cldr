//##header
/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.icu;

import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;

/**
 * Utilities that ought to be on collections, but aren't
 */
public final class CollectionUtilities {
    
    public static String join(Object[] array, String separator) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            if (i != 0) result.append(separator);
            result.append(array[i]);
        }
        return result.toString();
    }

    public static String join(Collection collection, String separator) {
        StringBuffer result = new StringBuffer();
        boolean first = true;
        for (Iterator it = collection.iterator(); it.hasNext();) {
            if (first) first = false;
            else result.append(separator);
            result.append(it.next());
        }
        return result.toString();
    }

    /**
     * Utility like Arrays.asList()
     */
    public static Map asMap(Object[][] source, Map target, boolean reverse) {
        int from = 0, to = 1;
        if (reverse) {
            from = 1; to = 0;
        }
        for (int i = 0; i < source.length; ++i) {
            target.put(source[i][from], source[i][to]);
        }
        return target;
    }
    
    public static Collection addAll(Iterator source, Collection target) {
        while (source.hasNext()) {
            target.add(source.next());
        }
        return target; // for chaining
    }
    
    public static int size(Iterator source) {
        int result = 0;
        while (source.hasNext()) {
            source.next();
            ++result;
        }
        return result;
    }
    

    public static Map asMap(Object[][] source) {
        return asMap(source, new HashMap(), false);
    }
    
    /**
     * Utility that ought to be on Map
     */
    public static Map removeAll(Map m, Collection itemsToRemove) {
        for (Iterator it = itemsToRemove.iterator(); it.hasNext();) {
            Object item = it.next();
            m.remove(item);
        }
        return m;
    }
    
    public Object getFirst(Collection c) {
        Iterator it = c.iterator();
        if (!it.hasNext()) return null;
        return it.next();
    }
    
    public static Object getBest(Collection c, Comparator comp, int direction) {
        Iterator it = c.iterator();
        if (!it.hasNext()) return null;
        Object bestSoFar = it.next();
        if (direction < 0) {
            while (it.hasNext()) {
                Object item = it.next();
                int compValue = comp.compare(item, bestSoFar);
                if (compValue < 0) {
                    bestSoFar = item;
                }
            }
        } else {
            while (it.hasNext()) {
                Object item = it.next();
                int compValue = comp.compare(item, bestSoFar);
                if (compValue > 0) {
                    bestSoFar = item;
                }
            }
        }
        return bestSoFar;
    }
    
    public interface ObjectMatcher {
        /**
         * Must handle null, never throw exception
         */
        boolean matches(Object o);
    }
    
    public static class InverseMatcher implements ObjectMatcher {
        ObjectMatcher other;
        public ObjectMatcher set(ObjectMatcher toInverse) {
            other = toInverse;
            return this;
        }
        public boolean matches(Object value) {
            return !other.matches(value);
        }
    }

    public static Collection removeAll(Collection c, ObjectMatcher f) {
        for (Iterator it = c.iterator(); it.hasNext();) {
            Object item = it.next();
            if (f.matches(item)) it.remove();
        }
        return c;
    }
    
    public static Collection retainAll(Collection c, ObjectMatcher f) {
        for (Iterator it = c.iterator(); it.hasNext();) {
            Object item = it.next();
            if (!f.matches(item)) it.remove();
        }
        return c;
    }
    
    public static boolean containsSome(Collection a, Collection b) {
        // fast paths
        if (a.size() == 0 || b.size() == 0) return false;
        if (a == b) return true; // must test after size test.

        if (a instanceof SortedSet && b instanceof SortedSet) {
            SortedSet aa = (SortedSet) a;
            SortedSet bb = (SortedSet) b;
            aa.containsAll(null);
            Comparator bbc = bb.comparator();
            Comparator aac = aa.comparator();
            if (bbc == null) {
                if (aac == null) {
                    Iterator ai = aa.iterator();
                    Iterator bi = bb.iterator();
                    Comparable ao = (Comparable) ai.next(); // these are ok, since the sizes are != 0
                    Comparable bo = (Comparable) bi.next();
                    while (true) {
                        int rel = ao.compareTo(bo);
                        if (rel < 0) {
                            if (!ai.hasNext()) return false;
                            ao = (Comparable) ai.next();
                        } else if (rel > 0) {
                            if (!bi.hasNext()) return false;
                            bo = (Comparable) bi.next();
                        } else {
                                return true;  
                        }
                    }
                }
            } else if (bbc.equals(a)) {
                Iterator ai = aa.iterator();
                Iterator bi = bb.iterator();
                Object ao = ai.next(); // these are ok, since the sizes are != 0
                Object bo = bi.next();
                while (true) {
                    int rel = aac.compare(ao, bo);
                    if (rel < 0) {
                        if (!ai.hasNext()) return false;
                        ao = ai.next();
                    } else if (rel > 0)  {
                        if (!bi.hasNext()) return false;
                        bo = bi.next();
                    } else {
                        return true;  
                    }
                }
            }           
        }
        for (Iterator it = a.iterator(); it.hasNext();) {
            if (b.contains(it.next())) return true;
        }
        return false;
    }
    
    public static boolean containsAll(Collection a, Collection b) {
        // fast paths
        if (a == b) return true;
        if (b.size() == 0) return true;
        if (a.size() == 0) return false;

        if (a instanceof SortedSet && b instanceof SortedSet) {
            SortedSet aa = (SortedSet) a;
            SortedSet bb = (SortedSet) b;
            Comparator bbc = bb.comparator();
            Comparator aac = aa.comparator();
            if (bbc == null) {
                if (aac == null) {
                    Iterator ai = aa.iterator();
                    Iterator bi = bb.iterator();
                    Comparable ao = (Comparable) ai.next(); // these are ok, since the sizes are != 0
                    Comparable bo = (Comparable) bi.next();
                    while (true) {
                        int rel = ao.compareTo(bo);
                        if (rel == 0) {
                            if (!bi.hasNext()) return true;
                            if (!ai.hasNext()) return false;
                            bo = (Comparable) bi.next();
                            ao = (Comparable) ai.next();
                        } else if (rel < 0) {
                            if (!ai.hasNext()) return false;
                            ao = (Comparable) ai.next();
                        } else {
                            return false;  
                        }
                    }
                }
            } else if (bbc.equals(a)) {
                Iterator ai = aa.iterator();
                Iterator bi = bb.iterator();
                Object ao = ai.next(); // these are ok, since the sizes are != 0
                Object bo = bi.next();
                while (true) {
                    int rel = aac.compare(ao, bo);
                    if (rel == 0) {
                        if (!bi.hasNext()) return true;
                        if (!ai.hasNext()) return false;
                        bo = bi.next();
                        ao = ai.next();
                    } else if (rel < 0) {
                        if (!ai.hasNext()) return false;
                        ao = ai.next();
                    } else {
                        return false;  
                    }
                }
            }           
        }
        return a.containsAll(b);
    }
    
    public static boolean containsNone(Collection a, Collection b) {
        return !containsSome(a, b);
    }
    
    /**
     * Used for results of getContainmentRelation
     */
    public static final int
        ALL_EMPTY = 0,
        NOT_A_SUPERSET_B = 1,
        NOT_A_DISJOINT_B = 2,
        NOT_A_SUBSET_B = 4,
        NOT_A_EQUALS_B = NOT_A_SUBSET_B | NOT_A_SUPERSET_B,
        A_PROPER_SUBSET_OF_B = NOT_A_DISJOINT_B | NOT_A_SUPERSET_B,
        A_PROPER_SUPERSET_B = NOT_A_SUBSET_B | NOT_A_DISJOINT_B,
        A_PROPER_OVERLAPS_B = NOT_A_SUBSET_B | NOT_A_DISJOINT_B | NOT_A_SUPERSET_B;
    
    /**
     * Assesses all the possible containment relations between collections A and B with one call.<br>
     * Returns an int with bits set, according to a "Venn Diagram" view of A vs B.<br>
     * NOT_A_SUPERSET_B: a - b != {}<br>
     * NOT_A_DISJOINT_B: a * b != {}  // * is intersects<br>
     * NOT_A_SUBSET_B: b - a != {}<br>
     * Thus the bits can be used to get the following relations:<br>
     * for A_SUPERSET_B, use (x & CollectionUtilities.NOT_A_SUPERSET_B) == 0<br>
     * for A_SUBSET_B, use (x & CollectionUtilities.NOT_A_SUBSET_B) == 0<br>
     * for A_EQUALS_B, use (x & CollectionUtilities.NOT_A_EQUALS_B) == 0<br>
     * for A_DISJOINT_B, use (x & CollectionUtilities.NOT_A_DISJOINT_B) == 0<br>
     * for A_OVERLAPS_B, use (x & CollectionUtilities.NOT_A_DISJOINT_B) != 0<br>
     */
     public static int getContainmentRelation(Collection a, Collection b) {
        if (a.size() == 0) {
            return (b.size() == 0) ? ALL_EMPTY : NOT_A_SUPERSET_B;
        } else if (b.size() == 0) {
            return NOT_A_SUBSET_B;
        }
        int result = 0;
        // WARNING: one might think that the following can be short-circuited, by looking at
        // the sizes of a and b. However, this would fail in general, where a different comparator is being
        // used in the two collections. Unfortunately, there is no failsafe way to test for that.
        for (Iterator it = a.iterator(); result != 6 && it.hasNext();) {
            result |= (b.contains(it.next())) ? NOT_A_DISJOINT_B : NOT_A_SUBSET_B;
        }
        for (Iterator it = b.iterator(); (result & 3) != 3 && it.hasNext();) {
            result |= (a.contains(it.next())) ? NOT_A_DISJOINT_B : NOT_A_SUPERSET_B;
        }
        return result;
    }

    public static String remove(String source, UnicodeSet removals) {
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            if (!removals.contains(cp)) UTF16.append(result, cp);
        }
        return result.toString();
    }

//#ifndef FOUNDATION
     /**
      * Does one string contain another, starting at a specific offset?
      * @param text
      * @param offset
      * @param other
      * @return
      */
        public static int matchesAt(CharSequence text, int offset, CharSequence other) {
            int len = other.length();
            int i = 0;
            int j = offset;
            for (; i < len; ++i, ++j) {
                char pc = other.charAt(i);
                char tc = text.charAt(j);
                if (pc != tc) return -1;
            }
            return i;
        }

        /**
         * Returns the ending offset found by matching characters with testSet, until a position is found that doen't match
         * @param string
         * @param offset
         * @param testSet
         * @return
         */
        public int span(CharSequence string, int offset, UnicodeSet testSet) {
            while (true) {
                int newOffset = testSet.matchesAt(string, offset);
                if (newOffset < 0) return offset;
            }
        }

        /**
         * Returns the ending offset found by matching characters with testSet, until a position is found that does match
         * @param string
         * @param offset
         * @param testSet
         * @return
         */
        public int spanNot(CharSequence string, int offset, UnicodeSet testSet) {
            while (true) {
                int newOffset = testSet.matchesAt(string, offset);
                if (newOffset >= 0) return offset;
                ++offset; // try next character position
                // we don't have to worry about surrogates for this.
            }
        }
//#endif

    public static String prettyPrint(UnicodeSet uset, boolean compressRanges, UnicodeSet toQuote, StringTransform quoter, 
            Comparator ordering, Comparator spaceComparator) {
        PrettyPrinter pp = new PrettyPrinter().setCompressRanges(compressRanges);
        if (toQuote != null) pp.setToQuote(toQuote);
        if (quoter != null) pp.setQuoter(quoter);
        if (ordering != null) pp.setOrdering(ordering);
        if (spaceComparator != null) pp.setSpaceComparator(spaceComparator);
        return pp.toPattern(uset);
    }
    
    public static class MultiComparator implements Comparator {
        private Comparator[] comparators;
    
        public MultiComparator (Comparator... comparators) {
            this.comparators = comparators;
        }
    
        /* Lexigraphic compare. Returns the first difference
         * @return zero if equal. Otherwise +/- (i+1) 
         * where i is the index of the first comparator finding a difference
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object arg0, Object arg1) {
            for (int i = 0; i < comparators.length; ++i) {
                int result = comparators[i].compare(arg0, arg1);
                if (result == 0) continue;
                if (result > 0) return i+1;
                return -(i+1);
            }
            return 0;
        }
    }

    /**
     * Modifies Unicode set to flatten the strings. Eg [abc{da}] => [abcd]
     * Returns the set for chaining.
     * @param exemplar1
     * @return
     */
    public static UnicodeSet flatten(UnicodeSet exemplar1) {
        UnicodeSet result = new UnicodeSet();
        boolean gotString = false;
        for (UnicodeSetIterator it = new UnicodeSetIterator(exemplar1); it.nextRange();) {
            if (it.codepoint == UnicodeSetIterator.IS_STRING) {
                result.addAll(it.string);
                gotString = true;
            } else {
                result.add(it.codepoint, it.codepointEnd);
            }
        }
        if (gotString) exemplar1.set(result);
        return exemplar1;
    }

    /**
     * For producing filtered iterators
     */
    public static abstract class FilteredIterator implements Iterator {
        private Iterator baseIterator;
        private static final Object EMPTY = new Object();
        private static final Object DONE = new Object();
        private Object nextObject = EMPTY;
        public FilteredIterator set(Iterator baseIterator) {
            this.baseIterator = baseIterator;
            return this;
        }
        public void remove() {
            throw new UnsupportedOperationException("Doesn't support removal");
        }
        public Object next() {
            Object result = nextObject;
            nextObject = EMPTY;
            return result;
        }       
        public boolean hasNext() {
            if (nextObject == DONE) return false;
            if (nextObject != EMPTY) return true;
            while (baseIterator.hasNext()) {
                nextObject = baseIterator.next();
                if (isIncluded(nextObject)) {
                    return true;
                }
            }
            nextObject = DONE;
            return false;
        }
        abstract public boolean isIncluded(Object item);
    }
    
    public static class PrefixIterator extends FilteredIterator {
        private String prefix;
        public PrefixIterator set(Iterator baseIterator, String prefix) {
            super.set(baseIterator);
            this.prefix = prefix;
            return this;
        }
        public boolean isIncluded(Object item) {
            return ((String)item).startsWith(prefix);
        }
    }
    
//#ifndef FOUNDATION
    public static class RegexIterator extends FilteredIterator {
        private Matcher matcher;
        public RegexIterator set(Iterator baseIterator, Matcher matcher) {
            super.set(baseIterator);
            this.matcher = matcher;
            return this;
        }
        public boolean isIncluded(Object item) {
            return matcher.reset((String)item).matches();
        }
    }
//#endif
}
