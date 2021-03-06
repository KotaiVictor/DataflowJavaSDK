/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.dataflow.contrib.sorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.dataflow.contrib.sorter.SorterTestUtils.SorterGenerator;
import com.google.cloud.dataflow.sdk.values.KV;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

/** Tests for {@link BufferedExternalSorter}. */
@RunWith(JUnit4.class)
public class BufferedExternalSorterTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  @SuppressWarnings("unchecked")
  @Test
  public void testNoFallback() throws Exception {
    ExternalSorter mockExternalSorter = mock(ExternalSorter.class);
    InMemorySorter mockInMemorySorter = mock(InMemorySorter.class);
    BufferedExternalSorter testSorter =
        new BufferedExternalSorter(mockExternalSorter, mockInMemorySorter);

    KV<byte[], byte[]>[] kvs =
        new KV[] {
          KV.of(new byte[] {0}, new byte[] {}),
          KV.of(new byte[] {0, 1}, new byte[] {}),
          KV.of(new byte[] {1}, new byte[] {})
        };

    when(mockInMemorySorter.addIfRoom(kvs[0])).thenReturn(true);
    when(mockInMemorySorter.addIfRoom(kvs[1])).thenReturn(true);
    when(mockInMemorySorter.addIfRoom(kvs[2])).thenReturn(true);
    when(mockInMemorySorter.sort()).thenReturn(Arrays.asList(kvs[0], kvs[1], kvs[2]));

    testSorter.add(kvs[0]);
    testSorter.add(kvs[1]);
    testSorter.add(kvs[2]);

    assertEquals(Arrays.asList(kvs[0], kvs[1], kvs[2]), testSorter.sort());

    // Verify external sorter was never called
    verify(mockExternalSorter, never()).add(any(KV.class));
    verify(mockExternalSorter, never()).sort();
  }

  @Test
  public void testFallback() throws Exception {
    ExternalSorter mockExternalSorter = mock(ExternalSorter.class);
    InMemorySorter mockInMemorySorter = mock(InMemorySorter.class);
    BufferedExternalSorter testSorter =
        new BufferedExternalSorter(mockExternalSorter, mockInMemorySorter);

    @SuppressWarnings("unchecked")
    KV<byte[], byte[]>[] kvs =
        new KV[] {
          KV.of(new byte[] {0}, new byte[] {}),
          KV.of(new byte[] {0, 1}, new byte[] {}),
          KV.of(new byte[] {1}, new byte[] {})
        };

    when(mockInMemorySorter.addIfRoom(kvs[0])).thenReturn(true);
    when(mockInMemorySorter.addIfRoom(kvs[1])).thenReturn(true);
    when(mockInMemorySorter.addIfRoom(kvs[2])).thenReturn(false);
    when(mockInMemorySorter.sort()).thenReturn(Arrays.asList(kvs[0], kvs[1]));
    when(mockExternalSorter.sort()).thenReturn(Arrays.asList(kvs[0], kvs[1], kvs[2]));

    testSorter.add(kvs[0]);
    testSorter.add(kvs[1]);
    testSorter.add(kvs[2]);

    assertEquals(Arrays.asList(kvs[0], kvs[1], kvs[2]), testSorter.sort());

    verify(mockExternalSorter, times(1)).add(kvs[0]);
    verify(mockExternalSorter, times(1)).add(kvs[1]);
    verify(mockExternalSorter, times(1)).add(kvs[2]);
  }

  @Test
  public void testEmpty() throws Exception {
    SorterTestUtils.testEmpty(BufferedExternalSorter.create(new BufferedExternalSorter.Options()));
  }

  @Test
  public void testSingleElement() throws Exception {
    SorterTestUtils.testSingleElement(
        BufferedExternalSorter.create(new BufferedExternalSorter.Options()));
  }

  @Test
  public void testEmptyKeyValueElement() throws Exception {
    SorterTestUtils.testEmptyKeyValueElement(
        BufferedExternalSorter.create(new BufferedExternalSorter.Options()));
  }

  @Test
  public void testMultipleIterations() throws Exception {
    SorterTestUtils.testMultipleIterations(
        BufferedExternalSorter.create(new BufferedExternalSorter.Options()));
  }

  @Test
  public void testManySortersFewRecords() throws Exception {
    SorterTestUtils.testRandom(
        new SorterGenerator() {
          @Override
          public Sorter generateSorter() throws Exception {
            return BufferedExternalSorter.create(new BufferedExternalSorter.Options());
          }
        },
        1000000,
        10);
  }

  @Test
  public void testOneSorterManyRecords() throws Exception {
    SorterTestUtils.testRandom(
        new SorterGenerator() {
          @Override
          public Sorter generateSorter() throws Exception {
            return BufferedExternalSorter.create(new BufferedExternalSorter.Options());
          }
        },
        1,
        1000000);
  }

  @Test
  public void testAddAfterSort() throws Exception {
    SorterTestUtils.testAddAfterSort(
        BufferedExternalSorter.create(new BufferedExternalSorter.Options()), thrown);
    fail();
  }

  @Test
  public void testSortTwice() throws Exception {
    SorterTestUtils.testSortTwice(
        BufferedExternalSorter.create(new BufferedExternalSorter.Options()), thrown);
    fail();
  }

  @Test
  public void testNegativeMemory() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("memoryMB must be greater than zero");
    BufferedExternalSorter.Options options = new BufferedExternalSorter.Options();
    options.setMemoryMB(-1);
  }
}
