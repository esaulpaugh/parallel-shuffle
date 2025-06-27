/*
   Copyright 2025 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
import java.util.Arrays;
import java.util.SplittableRandom;

public class ParallelShuffle {

    public static void main(String[] args) throws InterruptedException {
        final long baseSeed = System.nanoTime() + ~System.identityHashCode(args);
        final int[] vals = new int[6];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = Integer.MAX_VALUE - i;
        }
        System.out.println(Arrays.toString(vals));
        final int[] keys = new int[vals.length];
        final int split = 3;

        Thread t0 = new Thread(() -> {
            encrypt(vals, keys, baseSeed, 0, split);
            partialMergeSort(vals, keys, 0, split);
        });
        Thread t1 = new Thread(() -> {
            encrypt(vals, keys, baseSeed, split, vals.length);
            partialMergeSort(vals, keys, split, vals.length);
        });
        t0.start();
        t1.start();

        t0.join();
        t1.join();

        merge(vals, keys, 0, split, vals.length);

        t0 = new Thread(() -> decrypt(vals, keys, 0, split));
        t1 = new Thread(() -> decrypt(vals, keys, split, vals.length));

        t0.start();
        t1.start();

        t0.join();
        t1.join();

        System.out.println(Arrays.toString(vals));
    }

    private static void encrypt(int[] vals, int[] keys, long baseSeed, int start, int end) {
        final SplittableRandom rng = new SplittableRandom(baseSeed + start);
        for (int i = start; i < end; i++) {
            int x = rng.nextInt();
            vals[i] += x;
            keys[i] = x;
        }
    }

    private static void decrypt(int[] vals, int[] keys, int start, int end) {
        for (int i = start; i < end; i++) {
            vals[i] -= keys[i];
        }
    }

    public static void partialMergeSort(int[] vals, int[] follower, int left, int right) {
        if (right - left <= 1) return;
        final int mid = (left + right) >>> 1;
        partialMergeSort(vals, follower, left, mid);
        partialMergeSort(vals, follower, mid, right);
        merge(vals, follower, left, mid, right);
    }

    private static void merge(int[] vals, int[] follower, int left, int mid, int right) {
        int[] tempVals = new int[right - left];
        int[] tempFollow = new int[right - left];
        int i = left, j = mid, k = 0;
        while (i < mid && j < right) {
            if (vals[i] <= vals[j]) {
                tempVals[k] = vals[i];
                tempFollow[k++] = follower[i++];
            } else {
                tempVals[k] = vals[j];
                tempFollow[k++] = follower[j++];
            }
        }
        while (i < mid) {
            tempVals[k] = vals[i];
            tempFollow[k++] = follower[i++];
        }
        while (j < right) {
            tempVals[k] = vals[j];
            tempFollow[k++] = follower[j++];
        }
        for (int m = 0; m < tempVals.length; m++) {
            vals[left + m] = tempVals[m];
            follower[left + m] = tempFollow[m];
        }
    }
}
