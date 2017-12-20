package pl.poznan.put.cs.si.puttalky;

import io.vavr.collection.Array;

public class Permutator {
    static <T> Array<Array<T>> permute(Array<Array<T>> toPermute) {
        toPermute = toPermute.filter(a -> !a.isEmpty());
        if (toPermute.isEmpty()) {
            return toPermute;
        }
        if (toPermute.length() == 1) {
            return toPermute.get(0).map(Array::of);
        }
        return permute(toPermute.get(0), toPermute.drop(1));
    }

    private static <T> Array<Array<T>> permute(Array<T> initial, Array<Array<T>> left) {
        if (left.isEmpty()) {
            return initial.map(Array::of);
        }
        final Array<T> next = left.get(0);
        final Array<Array<T>> permuted = permute(next, left.drop(1));
        // [1],[2,3] => [1,2],[1,3]
        return initial.flatMap(a -> permuted.map(perm -> perm.append(a)));
    }
}
