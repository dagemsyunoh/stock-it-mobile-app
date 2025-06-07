package com.lock.stockit.Helpers;

import com.lock.stockit.Models.StockModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StockComparator implements Comparator<StockModel> {
    private List<Integer> extractNumbers(String input) {
        List<Integer> numbers = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\d+").matcher(input);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        return numbers;
    }

    @Override
    public int compare(StockModel o1, StockModel o2) {
        // Compare by item name first (case-insensitive)
        int nameCompare = o1.getItemName().compareToIgnoreCase(o2.getItemName());
        if (nameCompare != 0) return nameCompare;

        // Extract and compare numbers from itemSize
        List<Integer> nums1 = extractNumbers(o1.getItemSize());
        List<Integer> nums2 = extractNumbers(o2.getItemSize());

        int minLength = Math.min(nums1.size(), nums2.size());
        for (int i = 0; i < minLength; i++) {
            int compare = Integer.compare(nums1.get(i), nums2.get(i));
            if (compare != 0) return compare;
        }

        // If all compared numbers are equal, the one with more numbers is considered greater
        return Integer.compare(nums1.size(), nums2.size());
    }
}

