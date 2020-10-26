package com.ambiwsstudio.hikingeverywhere;

import java.util.ArrayList;

class HikingEverywhereTools {

    private static int minimum(int a, int b) {

        return Math.min(a, b);

    }

    private static void insertionSort(ArrayList<PhotoViewerActivity.Photo> a, int left, int right) {

        for (int i = left + 1; i <= right; i++) {

            int temp = a.get(i).likes.size();
            PhotoViewerActivity.Photo tempPhoto = a.get(i);
            int j = i - 1;

            while (j >= left && a.get(j).likes.size() > temp) {

                a.set(j + 1, a.get(j));
                j--;

            }

            a.set(j + 1, tempPhoto);

        }

    }

    private static void merge(ArrayList<PhotoViewerActivity.Photo> a, int left, int mid, int right) {

        int len1 = mid - left + 1, len2 = right - mid;

        int[] beg = new int[len1];
        PhotoViewerActivity.Photo[] begPhoto = new PhotoViewerActivity.Photo[len1];
        int[] end = new int[len2];
        PhotoViewerActivity.Photo[] endPhoto = new PhotoViewerActivity.Photo[len2];

        int i, j, k;

        for (i = 0; i < len1; i++) {

            beg[i] = a.get(left + 1).likes.size();
            begPhoto[i] = a.get(left + 1);

        }

        for (i = 0; i < len2; i++) {

            end[i] = a.get(mid + 1 + i).likes.size();
            endPhoto[i] = a.get(mid + 1 + i);

        }

        i = 0;
        j = 0;
        k = left;

        while (i < len1 && j < len2) {

            if (beg[i] <= end[j]) {

                a.set(k, begPhoto[i]);
                i++;

            } else {

                a.set(k, endPhoto[j]);
                j++;

            }

            k++;

        }

        while (i < len1) {

            a.set(k, begPhoto[i]);
            k++;
            i++;

        }

        while (j < len2) {

            a.set(k, endPhoto[j]);
            k++;
            j++;

        }

    }

    static void timSortPhotos(ArrayList<PhotoViewerActivity.Photo> a, int n) {

        int i, size, beg, mid, end;

        int run = 32;
        for (i = 0; i < n; i += run)
            insertionSort(a, i, minimum((i + 31), (n - 1)));

        for (size = run; size < n; size = 2 * size) {

            for (beg = 0; beg < n; beg += 2 * size) {

                mid = beg + size - 1;
                end = minimum((beg + 2 * size - 1), (n - 1));

                merge(a, beg, mid, end);

            }

        }

    }

}
