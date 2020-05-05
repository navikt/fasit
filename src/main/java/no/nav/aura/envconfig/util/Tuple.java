package no.nav.aura.envconfig.util;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;

@SuppressWarnings("serial")
public class Tuple<F, S> implements Serializable {

    public final F fst;
    public final S snd;

    public Tuple(F fst, S snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public static <F, S> Tuple<F, S> of(F fst, S snd) {
        return new Tuple<F, S>(fst, snd);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, false);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }

    @SuppressWarnings("rawtypes")
    public static <F extends Comparable> Comparator<? super Tuple<? extends F, ?>> fstComparator(final Ordering<Comparable> comparator) {
        return new Comparator<Tuple<? extends F, ?>>() {
            public int compare(Tuple<? extends F, ?> o1, Tuple<? extends F, ?> o2) {
                return comparator.compare(o1.fst, o2.fst);
            }
        };
    }

    @Override
    public String toString() {
        return "Tuple(" + fst + ", " + snd + ")";
    }

    public static <F, S> FluentIterable<F> fsts(Iterable<Tuple<F, S>> iterable) {
        return FluentIterable.from(iterable).transform(new SerializableFunction<Tuple<F, S>, F>() {
            public F process(Tuple<F, S> input) {
                return input.fst;
            }
        });
    };

}
