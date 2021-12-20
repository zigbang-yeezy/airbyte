/*
 * Copyright (c) 2021 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.commons.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MoreLists {

  /**
   * @return returns last element in array.
   */
  public static <T> T last(final List<T> list) {
    return list.get(list.size() - 1);
  }

  public static <T> List<T> sublistRemoveNFromEnd(final List<T> list, final int numItemsToRemove) {
    return list.subList(0, list.size() - numItemsToRemove);
  }

  public static <T> List<T> sublistToEnd(final List<T> list, final int startInclusive) {
    return list.subList(startInclusive, list.size());
  }

  /**
   * Reverses a list by creating a new list with the same elements of the input list and then
   * reversing it. The input list will not be altered.
   *
   * @param list to reverse
   * @param <T> type
   * @return new list with elements of original reversed.
   */
  public static <T> List<T> reversed(final List<T> list) {
    final ArrayList<T> reversed = new ArrayList<>(list);
    Collections.reverse(reversed);
    return reversed;
  }

}
