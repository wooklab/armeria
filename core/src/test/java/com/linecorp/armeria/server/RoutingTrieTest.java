/*
 * Copyright 2017 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.server;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;

import com.linecorp.armeria.server.RoutingTrie.Node;

class RoutingTrieTest {

    @Test
    void testTrieStructure() {
        final RoutingTrie.Builder<Object> builder = new RoutingTrie.Builder<>();

        final Object value1 = new Object();
        final Object value2 = new Object();
        final Object value3 = new Object();
        final Object value4 = new Object();

        builder.add("/abc/123", value1);
        builder.add("/abc/133", value2);
        builder.add("/abc/134", value3);
        builder.add("/abc/134", value1);
        builder.add("/abc/134/*", value4);
        builder.add("/abc/124/:", value2);

        final RoutingTrie<Object> trie = builder.build();

        // Expectation:
        //  /abc/1          (root)
        //         2        (no value)
        //           3      (has value1)
        //           4/     (no value)
        //              :   (has value2)
        //         3        (no value)
        //           3      (has value2)
        //           4      (has value3 and value1)
        //             /    (no value)
        //               *  (has value4)
        trie.dump(System.err);

        final Node<Object> found;

        // Root node.
        found = trie.findNode("/abc/1");
        assertThat(found.values().size()).isEqualTo(0);
        assertThat(found.parent()).isNull();

        testNodeWithFindParentNode(trie, "/abc/123", "/abc/12", value1);
        testNodeWithFindParentNode(trie, "/abc/133", "/abc/13", value2);
        testNodeWithFindParentNode(trie, "/abc/134", "/abc/13", value3, value1);
        testNodeWithFindParentNode(trie, "/abc/134/5678", "/abc/134/", value4);
        testNodeWithFindParentNode(trie, "/abc/134/5/6/7/8", "/abc/134/", value4);
        testNodeWithFindParentNode(trie, "/abc/124/5678", "/abc/124/", value2);

        // Not existing nodes.
        assertThat(trie.findNode("/abc/124/5/6/7/8")).isNull();
        assertThat(trie.findNode("/abc/111")).isNull();
        assertThat(trie.findNode("/")).isNull();
        assertThat(trie.findNode("/hello")).isNull();

        // Intermediate nodes.
        testIntermNode(trie, "/abc/12", "/abc/1");
        testIntermNode(trie, "/abc/13", "/abc/1");
        testIntermNode(trie, "/abc/124/", "/abc/12");
    }

    @Test
    void testParameterAndCatchAll() {
        final RoutingTrie.Builder<Object> builder = new RoutingTrie.Builder<>();

        final Object value0 = new Object();
        final Object value1 = new Object();
        final Object value2 = new Object();
        final Object value3 = new Object();
        final Object value4 = new Object();
        final Object value5 = new Object();
        final Object value6 = new Object();
        final Object value7 = new Object();
        final Object value8 = new Object();
        final Object value9 = new Object();

        builder.add("/users/:", value0);
        builder.add("/users/:", value1);
        builder.add("/users/:/movies", value2);
        builder.add("/users/:/books", value3);
        builder.add("/users/:/books/harry_potter", value4);
        builder.add("/users/:/books/harry_potter*", value5);
        builder.add("/users/:/books/:", value6);
        builder.add("/users/:/movies/*", value7);
        builder.add("/:", value8);
        builder.add("/*", value9);

        final RoutingTrie<Object> trie = builder.build();

        // Expectation:
        //                          `/`(exact, values=[])  (root)
        //                                    |
        //           +------------------------------------------------------+
        //           |                        |                             |
        // `:`(param, values=[8])   `*`(catch, values=[9])   `users/`(exact, values=[])
        //           |                        |                             |
        //          Nil                      Nil               `:`(param, values=[0,1])
        //                                                                  |
        //                                                        `/`(exact, values=[])
        //                                                                  |
        //                           +----------------------------------------------+
        //                           |                                              |
        //           `movies`(exact, values=[2])                     `books`(exact, values=[3])
        //                           |                                              |
        //                 `/`(exact, values=[])                          `/`(exact, values=[])
        //                           |                                              |
        //                `*`(catch, values=[7])               +----------------------------------+
        //                           |                         |                                  |
        //                          Nil              `:`(param, values=[6])   `harry_potter`(exact, values=[4])
        //                                                     |                                  |
        //                                                    Nil                        `*`(catch, values=[5])
        //                                                                                        |
        //                                                                                       Nil
        trie.dump(System.err);

        testNodeWithCheckingParentPath(trie, "/users/tom", "users/", value0, value1);
        testNodeWithCheckingParentPath(trie, "/users/tom/movies", "/", value2);
        testNodeWithCheckingParentPath(trie, "/users/tom/books", "/", value3);
        testNodeWithCheckingParentPath(trie, "/users/tom/books/harry_potter", "/", value4);
        testNodeWithCheckingParentPath(trie, "/users/tom/books/harry_potter1", "harry_potter", value5);
        testNodeWithCheckingParentPath(trie, "/users/tom/books/the_hunger_games", "/", value6);
        testNodeWithCheckingParentPath(trie, "/users/tom/movies/dunkirk", "/", value7);
        testNodeWithCheckingParentPath(trie, "/users/tom/movies/spider_man", "/", value7);
        testNodeWithCheckingParentPath(trie, "/faq", "/", value8);
        testNodeWithCheckingParentPath(trie, "/events/2017", "/", value9);
        testNodeWithCheckingParentPath(trie, "/", "/", value9);
    }

    @ParameterizedTest
    @MethodSource("generateFindStrategyData")
    void testFindAll(String path, int findFirst, List<Integer> findAll) {
        final ImmutableList<Object> values = IntStream.range(0, 10).mapToObj(i -> new Object() {
            @Override
            public String toString() {
                return "value" + i;
            }
        }).collect(toImmutableList());

        final RoutingTrie.Builder<Object> builder = new RoutingTrie.Builder<>();
        builder.add("/users/:", values.get(0));
        builder.add("/users/*", values.get(1));
        builder.add("/users/:/movies", values.get(2));
        builder.add("/users/:/books", values.get(3));
        builder.add("/users/:/books/harry_potter", values.get(4));
        builder.add("/users/:/books/harry_potter*", values.get(5));
        builder.add("/users/:/books/:", values.get(6));
        builder.add("/users/:/movies/*", values.get(7));
        builder.add("/:", values.get(8));
        builder.add("/*", values.get(9));

        final RoutingTrie<Object> trie = builder.build();
        List<Object> found;
        found = trie.find(path);
        assertThat(found).containsExactly(values.get(findFirst));
        found = trie.find(path);
        assertThat(found).containsExactly(values.get(findFirst));
        found = trie.findAll(path);
        final List<Object> greedyExpect = findAll.stream().map(values::get).collect(toImmutableList());
        assertThat(found).containsAll(greedyExpect);
    }

    static Stream<Arguments> generateFindStrategyData() {
        return Stream.of(
                Arguments.of("/users/1", 0, ImmutableList.of(0, 1, 9)),
                Arguments.of("/users/1/movies/1", 7, ImmutableList.of(7, 1, 9))
        );
    }

    @Test
    void testExceptionalCases() {
        assertThatThrownBy(() -> new RoutingTrie.Builder<>().build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RoutingTrie.Builder<>().add("*", new Object()).build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RoutingTrie.Builder<>().add("*012", new Object()).build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RoutingTrie.Builder<>().add(":", new Object()).build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RoutingTrie.Builder<>().add(":012", new Object()).build())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RoutingTrie.Builder<>().add("/*abc", new Object()).build())
                .isInstanceOf(IllegalStateException.class);
    }

    private static Node<?> testNodeWithCheckingParentPath(RoutingTrie<?> trie,
                                                          String targetPath, String parentPath,
                                                          Object... values) {
        final Node<?> found = trie.findNode(targetPath);
        assertThat(found.parent().path()).isEqualTo(parentPath);
        testValues(found, values);
        return found;
    }

    private static Node<?> testNodeWithFindParentNode(RoutingTrie<?> trie,
                                                      String targetPath, String parentPath,
                                                      Object... values) {
        final Node<?> found = trie.findNode(targetPath);
        assertThat(found.parent()).isEqualTo(trie.findNode(parentPath, true));
        testValues(found, values);
        return found;
    }

    private static Node<?> testIntermNode(RoutingTrie<?> trie, String targetPath, String parentPath) {
        final Node<?> found = trie.findNode(targetPath);
        assertThat(found.values().size()).isEqualTo(0);
        assertThat(found.parent()).isEqualTo(trie.findNode(parentPath, true));
        return found;
    }

    private static void testValues(Node<?> node, Object[] values) {
        final List<?> v = node.values();
        assertThat(v.size()).isEqualTo(values.length);
        for (int i = 0; i < values.length; i++) {
            assertThat(v.get(i)).isEqualTo(values[i]);
        }
    }
}
